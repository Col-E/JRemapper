package me.coley.jremapper.asm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;

import me.coley.jremapper.util.History;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Streams;

/**
 * Jar content wrapper.
 * 
 * @author Matt
 */
public class Input {
	private static Input CURRENT;
	private final File jarFile;
	public final Map<String, byte[]> rawNodeMap = new HashMap<>();
	public final Map<String, byte[]> resourceMap = new HashMap<>();
	public final History history = new History(this);

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
				if (entry.isDirectory())
					continue;
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

	public byte[] getRawClass(String name) {
		return rawNodeMap.get(name);
	}

	public boolean hasRawClass(String name) {
		return getRawClass(name) != null;
	}

	public Set<String> names() {
		return rawNodeMap.keySet();
	}

	public int getClassAccess(String item) {
		try {
			return new ClassReader(getRawClass(item)).getAccess();
		} catch (Exception e) {
			return Access.PUBLIC;
		}
	}
	
	private void updateCurrent() {
		if (CURRENT != null) {
			history.reset();
		}
		CURRENT = this;
	}
	
	public static Input get() {
		return CURRENT;
	}
}
