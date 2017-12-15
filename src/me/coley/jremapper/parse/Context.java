package me.coley.jremapper.parse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import me.coley.bmf.ClassNode;
import me.coley.bmf.mapping.AbstractMapping;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.bmf.util.Access;
import me.coley.jremapper.JRemapper;

public class Context {
	private final static boolean debug = false;
	private final static Map<String, String> primtiveLookup = new HashMap<>();
	private AbstractMapping[] mappings;
	private JRemapper jremap;
	private String[] lines;

	public Context(JRemapper jremap) {
		this.jremap = jremap;
	}

	public boolean parse(String text) {
		this.lines = text.split("\n");
		mappings = new AbstractMapping[text.length()];
		//
		try {
			CompilationUnit comp = JavaParser.parse(text);
			String current = jremap.getCurrentClass().name.getValue();
			String currentSimple = comp.getType(0).getNameAsString();
			//
			parseImports(comp.getImports());
			// Parse class
			ClassNode cn = jremap.getJarReader().getClassEntries().get(current);
			parseClassName(comp.getImports(), comp.getClassByName(currentSimple));
			if (Access.isEnum(cn.access)) {
				EnumDeclaration ed = comp.getEnumByName(currentSimple).get();
				parseExtend(comp.getImports(), ed.getImplementedTypes());
				parseFields(comp.getImports(), ed.getFields());
				parseMethods(comp.getImports(), ed.getMethods());
			} else {
				ClassOrInterfaceDeclaration cd = comp.getClassByName(currentSimple).get();
				parseExtend(comp.getImports(), cd.getExtendedTypes());
				parseExtend(comp.getImports(), cd.getImplementedTypes());
				parseFields(comp.getImports(), cd.getFields());
				parseMethods(comp.getImports(), cd.getMethods());
			}
			return true;
		} catch (Exception e) {
			String t = e.getMessage() + "\n\n";
			for (StackTraceElement s : e.getStackTrace()) {
				t += s.toString() + "\n";
			}
			jremap.getWindow().openTab("Error: JavaParser", t);
			e.printStackTrace();
			return false;
		}
	}

	private void parseImports(NodeList<ImportDeclaration> imports) {
		imports.forEach(i -> {
			String imported = i.getNameAsString().replace(".", "/");
			ClassMapping cm = jremap.getJarReader().getMapping().getMapping(imported);
			if (cm != null) {
				int start = toIndex(i.getName().getBegin().get()) - 1;
				fill(start, imported, cm);
			}
		});
	}

	private void parseClassName(NodeList<ImportDeclaration> imports, Optional<ClassOrInterfaceDeclaration> clazz) {
		if (clazz.isPresent()) {
			ClassOrInterfaceDeclaration cd = clazz.get();
			String name = cd.getNameAsString();
			ClassMapping cm = jremap.getCurrentClass();
			int start = toIndex(cd.getName().getBegin().get()) - 1;
			fill(start, name, cm);
		}
	}

	private void parseExtend(NodeList<ImportDeclaration> imports, NodeList<ClassOrInterfaceType> extendedTypes) {
		extendedTypes.forEach(e -> {
			String ext = e.getNameAsString();
			String full = getQuantified(imports, ext);
			int start = toIndex(e.getBegin().get()) - 1;
			ClassMapping cm = jremap.getJarReader().getMapping().getMapping(full);
			if (cm != null) {
				fill(start, ext, cm);
			}
		});
	}

	private void parseFields(NodeList<ImportDeclaration> imports, List<FieldDeclaration> fields) {
		fields.forEach(f -> {
			// Map field type
			String type = trimType(f.getElementType().asString());
			int typeStart = toIndex(f.getElementType().getBegin().get());
			String full = getQuantified(imports, type);
			ClassMapping cm = jremap.getJarReader().getMapping().getMapping(full);
			if (cm != null) {
				fill(typeStart - 1, type, cm);
			}
			// Map field name
			VariableDeclarator var = f.getVariable(0);
			String name = var.getNameAsString();
			String desc = getArr(f.getElementType()) + (full.length() == 1 ? full : "L" + full + ";");
			int start = toIndex(var.getBegin().get()) - 1;
			MemberMapping mm = jremap.getCurrentClass().getMemberMappingWithRenaming(name, desc);
			if (mm != null) {
				fill(start, name, mm);
			}
		});
	}

	private void parseMethods(NodeList<ImportDeclaration> imports, List<MethodDeclaration> methods) {
		methods.forEach(m -> {
			// Map method type
			String retType = trimType(m.getType().asString());
			int retTypeStart = toIndex(m.getType().getBegin().get());
			String retTypeFull = getQuantified(imports, retType);
			ClassMapping cmRet = jremap.getJarReader().getMapping().getMapping(retTypeFull);
			if (cmRet != null) {
				fill(retTypeStart - 1, retType, cmRet);
			}
			// Map method name
			StringBuilder args = new StringBuilder();
			for (Parameter param : m.getParameters()) {
				// Map parameter type
				String paramType = trimType(param.getType().asString());
				String paramTypeFull = getQuantified(imports, paramType);
				int paramTypeStart = toIndex(param.getType().getBegin().get());
				ClassMapping cmParam = jremap.getJarReader().getMapping().getMapping(paramTypeFull);
				if (cmParam != null) {
					fill(paramTypeStart - 1, paramType, cmParam);
				}
				// Append to method arg descriptor builder
				String arg = getArr(param.getType())
						+ (paramTypeFull.length() == 1 ? paramTypeFull : "L" + paramTypeFull + ";");
				args.append(arg);
			}
			String ret = retTypeFull.length() == 1 ? retTypeFull : "L" + retTypeFull + ";";
			String desc = "(" + args.toString() + ")" + ret;
			String name = m.getNameAsString();
			int start = toIndex(m.getName().getBegin().get()) - 1;
			MemberMapping mm = jremap.getCurrentClass().getMemberMappingWithRenaming(name, desc);
			if (mm != null) {
				fill(start, name, mm);
			}
			// Parse method body
			if (m.getBody().isPresent()) {
				parseMethodBody(imports, m.getBody().get());
			}
		});
	}

	private void parseMethodBody(NodeList<ImportDeclaration> imports, BlockStmt blockStmt) {
		// TODO Auto-generated method stub
	}

	/**
	 * Fills the indexed mapping range of the given beginning + length of the given
	 * element.
	 * 
	 * @param begin
	 *            Start position.
	 * @param element
	 *            Element to apply mapping to.
	 * @param am
	 *            Mapping to apply.
	 */
	private void fill(int start, String element, AbstractMapping am) {
		int end = start + element.length() + 1;
		Arrays.fill(mappings, start, end, am);
	}

	/**
	 * Retrieves the quantified name from the simple name by searching the given
	 * imports.
	 * 
	 * @param imports
	 * @param simple
	 * @return
	 */
	private String getQuantified(NodeList<ImportDeclaration> imports, String simple) {
		for (ImportDeclaration i : imports) {
			String name = i.getNameAsString();
			if (name.substring(name.lastIndexOf(".") + 1).equals(simple)) {
				return name.replace(".", "/");
			}
		}

		return primtiveLookup.getOrDefault(simple, simple);
	}

	/**
	 * Converts the javaparser position to the index of the original text.
	 * 
	 * @param pos
	 * @return
	 */
	private int toIndex(Position pos) {
		int length = 0;
		for (int i = 0; i < pos.line - 1; i++) {
			length += lines[i].length() + 1;
		}
		length += pos.column;
		return length;
	}

	/**
	 * Remove bits not used in parsing mapping names.
	 * 
	 * @param type
	 * @return
	 */
	private static String trimType(String type) {
		// Skip generics
		if (type.contains("<")) {
			type = type.substring(0, type.indexOf("<"));
		}
		// Skip array level
		if (type.contains("[]")) {
			type = type.substring(0, type.indexOf("[]"));
		}
		return type;
	}

	/**
	 * Build descriptor array level.
	 * 
	 * @param type
	 * @return
	 */
	private String getArr(Type type) {
		String arr = "";
		for (int k = 0; k < type.getArrayLevel(); k++) {
			arr += "[";
		}
		return arr;
	}

	public AbstractMapping getMappingAtPoint(int index) {
		return mappings[index];
	}

	static {
		// Hack for including primtive arrays but I'm lazy and this should work fine.
		for (int i = 0; i < 3; i++) {
			String inArr = "", outArr = "";
			for (int k = 0; k < i; k++) {
				inArr += "[]";
				outArr += "[";
			}
			primtiveLookup.put("int" + inArr, outArr + "I");
			primtiveLookup.put("float" + inArr, outArr + "F");
			primtiveLookup.put("double" + inArr, outArr + "D");
			primtiveLookup.put("long" + inArr, outArr + "J");
			primtiveLookup.put("boolean" + inArr, outArr + "Z");
			primtiveLookup.put("char" + inArr, outArr + "C");
			primtiveLookup.put("byte" + inArr, outArr + "B");
		}
		primtiveLookup.put("void", "V");
	}
}
