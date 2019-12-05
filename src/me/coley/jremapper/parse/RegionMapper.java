package me.coley.jremapper.parse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.Position;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.*;

import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import javassist.CtMethod;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Reflect;
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
			Optional<MethodDeclaration> ast = type.toAst();
			String desc = null;
			if (ast.isPresent()) {
				desc = getMethodDesc(ast.get());
			} else if (resolved instanceof JavassistMethodDeclaration){
				CtMethod method = Reflect.get(resolved, "ctMethod");
				if (method != null)
					desc = method.getMethodInfo().getDescriptor();
			} else if (resolved instanceof ReflectionMethodDeclaration) {
				ReflectionMethodDeclaration ref = (ReflectionMethodDeclaration) resolved;
				Method method = Reflect.get(ref, "method");
				desc = org.objectweb.asm.Type.getType(method).getDescriptor();
			} else {
				desc = getMethodDesc(type);
			}
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
			String desc = null;
			try {
				desc =	getDescriptor(type.getType());
			} catch(UnsolvedSymbolException ex) {
				if (type instanceof JavaParserFieldDeclaration) {
					desc = getDescriptor(((JavaParserFieldDeclaration) type).getWrappedNode().getCommonType());
				}
			}
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
	 * @param md
	 *            JavaParser method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	private String getMethodDesc(MethodDeclaration md) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the method parameters for the descriptor
		NodeList<Parameter> params = md.getParameters();
		for (Parameter param : params) {
			Type pType = param.getType();
			String pDesc = getDescriptor(pType);
			if (pDesc == null)
				return null;
			sbDesc.append(pDesc);
		}
		// Append the return type for the descriptor
		Type typeRet = md.getType();
		String retDesc = getDescriptor(typeRet);
		if (retDesc == null)
			return null;
		sbDesc.append(")");
		sbDesc.append(retDesc);
		return sbDesc.toString();
	}

	/**
	 * @param md
	 *            JavaParser resolved method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	private String getMethodDesc(ResolvedMethodDeclaration md) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the method parameters for the descriptor
		int p = md.getNumberOfParams();
		for (int i = 0; i < p; i++) {
			ResolvedParameterDeclaration param = md.getParam(i);
			String pDesc = null;
			if (param.isType()) {
				ResolvedTypeDeclaration pType = param.asType();
				pDesc = typeToDesc(pType);
			} else {
				ResolvedType pType = param.getType();
				pDesc = typeToDesc(pType);
			}
			if (pDesc == null)
				return null;
			sbDesc.append(pDesc);
		}
		// Append the return type for the descriptor
		ResolvedType typeRet = md.getReturnType();
		String retDesc = typeToDesc(typeRet);
		if (retDesc == null) {
			return null;
		}
		sbDesc.append(")");
		sbDesc.append(retDesc);
		return sbDesc.toString();
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	private String getDescriptor(Type type) {
		if (type.isArrayType())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return isPrim(type) ? primTypeToDesc(type) : typeToDesc(type);
	}


	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	private String getDescriptor(ResolvedType type) {
		if (type.isArray())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return type.isPrimitive() ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private String typeToDesc(Type type) {
		String key = null;
		if (type instanceof ClassOrInterfaceType) {
			try {
				key = ((ClassOrInterfaceType) type).resolve().getQualifiedName();
			} catch(Exception ex) { /* ignored */ }
		}
		if (key == null)
			key = type.asString();

		CDec dec = getClassDec(key);
		if (dec == null)
			return null;
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(dec.getFullName());
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private String typeToDesc(ResolvedType type) {
		CDec dec = null;
		if (type instanceof ResolvedTypeVariable) {
			dec = getClassDec(((ResolvedTypeVariable) type).qualifiedName());
		} else if (type instanceof ResolvedTypeParameterDeclaration) {
			dec = getClassDec(type.asTypeParameter().getQualifiedName());
		} else if(type.isPrimitive()) {
			return primTypeToDesc(type.asPrimitive());
		} else if(type.isVoid()) {
			return "V";
		} else {
			dec = getClassDec(type.describe());
		}
		if (dec == null)
			return null;
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.arrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(dec.getFullName());
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser resolved type.
	 * @return Internal descriptor from type.
	 */
	private String typeToDesc(ResolvedTypeDeclaration type) {
		CDec dec = getClassDec(type.getQualifiedName());
		if (dec == null)
			return null;
		return "L" + dec.getFullName() + ";";
	}

	/**
	 * @param type
	 *            JavaParser type.
	 * @return {@code true} if the type denotes a primitive or void type.
	 */
	private static boolean isPrim(Type type) {
		// void is not a primitive, but lets just pretend it is.
		return type.isVoidType() || type.isPrimitiveType();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(Type type) {
		String desc = null;
		switch (type.asString()) {
			case "boolean":
				desc = "Z";
				break;
			case "int":
				desc = "I";
				break;
			case "long":
				desc = "J";
				break;
			case "short":
				desc = "S";
				break;
			case "byte":
				desc = "B";
				break;
			case "double":
				desc = "D";
				break;
			case "float":
				desc = "F";
				break;
			case "void":
				desc = "V";
				break;
			default:
				throw new RuntimeException("Unknown primitive type field '" + type.asString() + "'");
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append(desc);
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(ResolvedType type) {
		String desc = null;
		switch (type.describe()) {
			case "boolean":
				desc = "Z";
				break;
			case "int":
				desc = "I";
				break;
			case "long":
				desc = "J";
				break;
			case "short":
				desc = "S";
				break;
			case "byte":
				desc = "B";
				break;
			case "double":
				desc = "D";
				break;
			case "float":
				desc = "F";
				break;
			case "void":
				desc = "V";
				break;
			default:
				throw new RuntimeException("Unknown primitive type field '" + type.describe() + "'");
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.arrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append(desc);
		return sbDesc.toString();
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
