package me.coley.jremapper.util;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import javassist.CtMethod;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JavaParser type utilities.
 *
 * @author Matt
 */
public class ParserTypeUtil {
	/**
	 * @param type
	 * 		Resolved method declaration.
	 *
	 * @return Descriptor of the resolved method.
	 */
	public static String getResolvedMethodDesc(ResolvedMethodDeclaration type) {
		Optional<MethodDeclaration> ast = type.toAst();
		String desc = null;
		if (ast.isPresent()) {
			desc = ParserTypeUtil.getMethodDesc(ast.get());
		} else if (type instanceof JavassistMethodDeclaration){
			CtMethod method = Reflect.get(type, "ctMethod");
			if (method != null)
				desc = method.getMethodInfo().getDescriptor();
		} else if (type instanceof ReflectionMethodDeclaration) {
			ReflectionMethodDeclaration ref = (ReflectionMethodDeclaration) type;
			Method method = Reflect.get(ref, "method");
			desc = org.objectweb.asm.Type.getType(method).getDescriptor();
		} else {
			desc = ParserTypeUtil.getMethodDesc(type);
		}
		return desc;
	}

	/**
	 * @param type
	 * 		Resolved field declaration.
	 *
	 * @return Descriptor of the resolved field.
	 */
	public static String getResolvedFieldDesc(ResolvedFieldDeclaration type) {
		String desc = null;
		try {
			desc =	ParserTypeUtil.getDescriptor(type.getType());
		} catch(UnsolvedSymbolException ex) {
			if (type instanceof JavaParserFieldDeclaration) {
				desc = ParserTypeUtil.getDescriptor(((JavaParserFieldDeclaration) type).getWrappedNode().getCommonType());
			}
		} catch(UnsupportedOperationException e) {}
		return desc;
	}

	/**
	 * @param md
	 *            JavaParser method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	private static String getMethodDesc(MethodDeclaration md) {
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
	private static String getMethodDesc(ResolvedMethodDeclaration md) {
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
	private static String getDescriptor(ResolvedType type) {
		if (type.isArray())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return type.isPrimitive() ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	private static String getDescriptor(Type type) {
		if (type.isArrayType())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return isPrim(type) ? primTypeToDesc(type) : typeToDesc(type);
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
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(ResolvedType type) {
		String qualified = null;
		if (type instanceof ResolvedTypeVariable) {
			qualified = ((ResolvedTypeVariable) type).qualifiedName();
		} else if (type instanceof ResolvedTypeParameterDeclaration) {
			qualified = type.asTypeParameter().getQualifiedName();;
		} else if(type.isPrimitive()) {
			return primTypeToDesc(type.asPrimitive());
		} else if(type.isVoid()) {
			return "V";
		} else {
			qualified = type.describe();
		}
		if (qualified == null)
			return null;
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.arrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(qualified.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser resolved type.
	 * @return Internal descriptor from type.
	 */
	private static String typeToDesc(ResolvedTypeDeclaration type) {
		return "L" + type.getQualifiedName().replace('.', '/') + ";";
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(Type type) {
		String key = null;
		if (type instanceof ClassOrInterfaceType) {
			try {
				key = ((ClassOrInterfaceType) type).resolve().getQualifiedName();
			} catch(Exception ex) { /* ignored */ }
		}
		if (key == null)
			key = type.asString();
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(key.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}
}
