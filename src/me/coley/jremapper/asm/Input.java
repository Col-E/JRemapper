package me.coley.jremapper.asm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.javaparser.*;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import me.coley.jremapper.util.History;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Streams;

import static com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory.toTypeDeclaration;

/**
 * Jar content wrapper.
 * 
 * @author Matt
 */
public class Input implements TypeSolver {
	private static Input CURRENT;
	private final File jarFile;
	public final Map<String, byte[]> rawNodeMap = new HashMap<>();
	public final Map<String, byte[]> resourceMap = new HashMap<>();
	public final History history = new History(this);
	// JavaParser stuff
	private final TypeSolver childSolver = new ReflectionTypeSolver();
	private final ClassPool classPool = new ClassPool(false);
	private ParserConfiguration config;
	private TypeSolver parent;


	/**
	 * Create JarFile content maps.
	 * 
	 * @param jarFile
	 * @throws IOException
	 *             Thrown if contents could not be read.
	 */
	public Input(File jarFile) throws IOException {
		updateCurrent();
		this.jarFile = jarFile;
		readArchive();
		classPool.appendSystemPath();
		rawNodeMap.forEach((k, v) ->
				classPool.insertClassPath(new ByteArrayClassPath(k.replace("/", "."), v)));
	}

	/**
	 * Populates class and resource maps.
	 * 
	 * @throws IOException
	 *             Thrown if the archive could not be read, or an internal file
	 *             could not be read.
	 */
	protected Map<String, byte[]> readArchive() throws IOException {
		Map<String, byte[]> contents = new HashMap<>();
		try (ZipFile file = new ZipFile(jarFile)) {
			// iterate zip entries
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// skip directories
				if (entry.isDirectory()) continue;
				try (InputStream is = file.getInputStream(entry)) {
					// add as class, or resource if not a class file.
					String name = entry.getName();
					if (name.endsWith(".class")) {
						addClass(name, is);
					} else {
						addResource(name, is);
					}
				}
			}
		}
		return contents;
	}

	/**
	 * Try to add the class contained in the given stream to the classes map.
	 * 
	 * @param name
	 *            Entry name.
	 * @param is
	 *            Stream of entry.
	 * @throws IOException
	 *             Thrown if stream could not be read or if ClassNode could not be
	 *             derived from the streamed content.
	 */
	protected void addClass(String name, InputStream is) {
		try {
			byte[] value = Streams.from(is);
			ClassReader cr = new ClassReader(value);
			String className = cr.getClassName();
			rawNodeMap.put(className, value);
		} catch (Exception e) {
			Logging.error("Could not parse class: " + name);
		}
	}

	/**
	 * Try to add the resource contained in the given stream to the resource map.
	 * 
	 * @param name
	 *            Entry name.
	 * @param is
	 *            Stream of entry.
	 * @throws IOException
	 *             Thrown if stream could not be read.
	 */
	protected void addResource(String name, InputStream is) {
		try {
			resourceMap.put(name, Streams.from(is));
		} catch (IOException e) {
			Logging.error("Could not parse resource: " + name);

		}
	}

	/**
	 * @param name
	 *            Internal class name.
	 * @return Class bytecode.
	 */
	public byte[] getRawClass(String name) {
		return rawNodeMap.get(name);
	}

	/**
	 * 
	 * @param name
	 *            Internal class name.
	 * @return {@code true} if class by the given name exists in the input.
	 */
	public boolean hasRawClass(String name) {
		return getRawClass(name) != null;
	}

	/**
	 * @return Set of internal class names.
	 */
	public Set<String> names() {
		return rawNodeMap.keySet();
	}

	/**
	 * @param name
	 *            Internal class name.
	 * @return Class access modifiers.
	 */
	public int getClassAccess(String name) {
		try {
			return new ClassReader(getRawClass(name)).getAccess();
		} catch (Exception e) {
			return Access.PUBLIC;
		}
	}

	/**
	 * @return Map of classes as tree-api representations.
	 * @throws IOException
	 */
	public Map<String, ClassNode> genNodes() throws IOException {
		Map<String, ClassNode> m = new HashMap<>();
		for (Entry<String, byte[]> e : rawNodeMap.entrySet()) {
			byte[] bs = e.getValue();
			ClassReader cr = new ClassReader(new ByteArrayInputStream(bs));
			ClassNode cn = new ClassNode();
			cr.accept(cn, ClassReader.SKIP_CODE);
			m.put(e.getKey(), cn);
		}
		return m;
	}

	/**
	 * @return JavaParser config to assist in resolving symbols.
	 */
	public ParserConfiguration getSourceParseConfig() {
		if (config == null)
			updateSourceConfig();
		return config;
	}

	/**
	 * Creates a source config with a type resolver that can access all types in the workspace.
	 */
	public void updateSourceConfig() {
		config = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(this));
	}

	@Override
	public TypeSolver getParent() {
		return this.parent;
	}

	@Override
	public void setParent(TypeSolver parent) {
		this.parent = parent;
	}

	@Override
	public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
		try {
			String internal = name.replace(".", "/");
			if(hasRawClass(internal)) {
				InputStream is = new ByteArrayInputStream(getRawClass(internal));
				ResolvedReferenceTypeDeclaration dec = toTypeDeclaration(classPool.makeClass(is), getRoot());
				SymbolReference<ResolvedReferenceTypeDeclaration> solved = SymbolReference.solved(dec);
				if (solved.isSolved())
					return solved;
			}
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to resolve type: " + name, ex);
		}
		return childSolver.tryToSolveType(name);
	}

	/**
	 * Update the static reference to this Input instance, unsubscribe the
	 * existing/old reference from events.
	 */
	private void updateCurrent() {
		if (CURRENT != null) {
			CURRENT.history.reset();
		}
		CURRENT = this;
	}

	/**
	 * @return Static reference to the current Input instance.
	 */
	public static Input get() {
		return CURRENT;
	}
}
