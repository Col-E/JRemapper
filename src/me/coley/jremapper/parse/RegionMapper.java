package me.coley.jremapper.parse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.Position;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;

import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.mapping.CMap;
import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.util.*;
import org.objectweb.asm.ClassReader;

/**
 * Allows linking regions of text do different mappings using the JavaParser
 * library.
 *
 * For reference:
 * <ul>
 * <li>Quantified name: Full name of a class, such as
 * <i>com.example.MyType</i></li>
 * <li>Simple name: Short-hand name of a class, such as <i>MyType</i></li>
 * </ul>
 *
 * @author Matt
 */
public class RegionMapper {
	private final Input input;
	private final CompilationUnit cu;
	private final String className;
	private final Map<String, CDec> decMap = new HashMap<>();

	public RegionMapper(Input input, String className, CompilationUnit cu) {
		this.input = input;
		this.cu = cu;
		this.className = className;
	}

	/**
	 * @return Dec of the current class.
	 */
	public CDec getHost() {
		return decMap.get(className);
	}

	/**
	 * @param line
	 *            Caret line in editor.
	 * @param column
	 *            Caret column in editor.
	 * @return CDec at position. May be {@code null}.
	 */
	public CDec getClassFromPosition(int line, int column) {
		Node node = getNodeAt(line, column);
		if(!(node instanceof Resolvable))
			return null;
		// Resolve node to some declaration type
		Resolvable<?> r = (Resolvable<?>) node;
		Object resolved = null;
		try {
			resolved = r.resolve();
		} catch(Exception ex) {
			return null;
		}
		if (resolved instanceof ResolvedReferenceType) {
			ResolvedReferenceType type = (ResolvedReferenceType) resolved;
			return getClassDec(type.getQualifiedName());
		} else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
			ResolvedReferenceTypeDeclaration type = (ResolvedReferenceTypeDeclaration) resolved;
			return getClassDec(type.getQualifiedName());
		} else if (resolved instanceof ResolvedConstructorDeclaration) {
			ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
			return getClassDec(type.getQualifiedName());
		}
		return null;
	}

	/**
	 * @param line
	 *            Caret line in editor.
	 * @param column
	 *            Caret column in editor.
	 * @return MDec at position. May be {@code null}.
	 */
	public MDec getMemberFromPosition(int line, int column) {
		Node node = getNodeAt(line, column);
		if(!(node instanceof Resolvable))
			return null;
		// Resolve node to some declaration type
		Resolvable<?> r = (Resolvable<?>) node;
		Object resolved = null;
		try {
			resolved = r.resolve();
		} catch(Exception ex) {
			return null;
		}
		if (resolved instanceof ResolvedMethodDeclaration) {
			ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
			ResolvedTypeDeclaration owner = type.declaringType();
			CDec dec = getClassDec(owner.getQualifiedName());
			String name = type.getName();
			if (dec.uniqueName(name)) {
				Optional<MDec> m = dec.getByName(name);
				if (m.isPresent())
					return m.get();
			}
			String desc = ParserTypeUtil.getResolvedMethodDesc(type);
			return dec.getMember(name, desc);
		} else if (resolved instanceof ResolvedFieldDeclaration) {
			ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
			ResolvedTypeDeclaration owner = type.declaringType();
			CDec dec = getClassDec(owner.getQualifiedName());
			String name = type.getName();
			if (dec.uniqueName(name)) {
				Optional<MDec> m = dec.getByName(name);
				if (m.isPresent())
					return m.get();
			}
			String desc = ParserTypeUtil.getResolvedFieldDesc(type);
			return dec.getMember(name, desc);
		}
		return null;
	}

	/**
	 * @param name
	 * 		Name of class.
	 *
	 * @return Declaration wrapper for class.
	 */
	private CDec getClassDec(String name) {
		if (name.contains("."))
			name = name.replace('.', '/');
		return decMap.computeIfAbsent(name, this::generateDec);
	}

	/**
	 * Creates a class declaration wrapper.
	 *
	 * @param name
	 * 		Name of class.
	 *
	 * @return Declaration wrapper for class.
	 */
	private CDec generateDec(String name) {
		CMap mapped = Mappings.INSTANCE.getClassReverseMapping(name);
		if (mapped != null) {
			name = mapped.getOriginalName();
		}
		CDec dec = CDec.fromClass(name);
		ClassReader cr = reader(dec.getFullName());
		if (!input.hasRawClass(dec.getFullName()))
			dec.setLocked(true);
		if(cr != null)
			cr.accept(new DecBuilder(dec, input), 0);
		else
			Logging.info("Failed class lookup for: " + dec.getFullName());
		return dec;
	}

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Reader of class.
	 */
	private ClassReader reader(String name) {
		byte[] code = input.getRawClass(name);
		if (code != null)
			return new ClassReader(code);
		try {
			return new ClassReader(name);
		} catch(IOException e) {
			return null;
		}
	}

	/**
	 * Returns the AST node at the given position.
	 * The child-most node may not be returned if the parent is better suited for contextual
	 * purposes.
	 *
	 * @param line
	 * 		Cursor line.
	 * @param column
	 * 		Cursor column.
	 *
	 * @return JavaParser AST node at the given position in the source code.
	 */
	private Node getNodeAt(int line, int column) {
		return getNodeAt(line, column, cu.findRootNode());
	}

	private Node getNodeAt(int line, int column, Node root) {
		// We want to know more about this type, don't resolve down to the lowest AST
		// type... the parent has more data and is essentially just a wrapper around SimpleName.
		if (root instanceof SimpleName)
			return null;
		// Verify the node range can be accessed
		if (!root.getBegin().isPresent() || !root.getEnd().isPresent())
			return null;
		// Check cursor is in bounds
		// We won't instantly return null because the root range may be SMALLER than
		// the range of children. This is really stupid IMO but thats how JavaParser is...
		boolean bounds = true;
		Position cursor = Position.pos(line, column);
		if (cursor.isBefore(root.getBegin().get()) || cursor.isAfter(root.getEnd().get()))
			bounds = false;
		// Iterate over children, return non-null child
		for (Node child : root.getChildNodes()) {
			Node ret = getNodeAt(line, column, child);
			if (ret != null)
				return ret;
		}
		// If we're not in bounds and none of our children are THEN we assume this node is bad.
		if (!bounds)
			return null;
		// In bounds so we're good!
		return root;
	}
}
