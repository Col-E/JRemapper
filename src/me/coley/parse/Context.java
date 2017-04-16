package me.coley.parse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.bmf.mapping.AbstractMapping;
import io.github.bmf.mapping.ClassMapping;
import io.github.bmf.mapping.MemberMapping;
import me.coley.Program;

public class Context {
	private final static List<String> ID_FLOW = Arrays.asList("if ", "else", "do", "while", "for", "continue", "break");
	private final static List<String> ID_MODIFIERS = Arrays.asList("abstract", "final", "interface", "native", "private", "protected", "public", "static", "strict", "synchronized", "transient",
			"volatile");
	private final static List<String> ID_PRIMITIVES = Arrays.asList("void", "boolean", "byte", "char", "short", "int", "long", "float", "double");
	private final static List<String> ID_PRIMITIVES_SYMBOL = Arrays.asList("V", "Z", "B", "C", "S", "I", "J", "F", "D");
	private final static List<String> ID_LANG = Arrays.asList("String", "System");
	private final static List<String> ID_LANG_SYMBOL = Arrays.asList("Ljava/lang/String;", "Ljava/lang/System;");
	private final static String ID_PACKAGE = "package";
	private final static String ID_IMPORT = "import";
	private final static String ID_CLASS = "class";
	private final static String ID_INTERFACE = "interface";
	private final static String ID_IMPLEMENTS = "implements";
	private final static String ID_EXTENDS = "extends";
	private final static String ID_ENUM = "enum";
	private final static String ID_FINAL = "final";
	private final static boolean debug = true;
	private ClassType thisType;
	private Map<String, String> simpleToQuantified = new HashMap<>();
	private ContextType[] context;
	private AbstractMapping[] mappings;
	private Program callback;

	public Context(Program callback) {
		this.callback = callback;
	}

	public ContextType getContextForLine(int line) {
		return ContextType.UNKNOWN;
		// return line < context.size() ? context.get(line) :
		// ContextType.UNKNOWN;
	}

	public void parse(String text) {
		// parse lines for context
		// Scanner scan = new Scanner(text);
		IndexableStringReader read = new IndexableStringReader(text);
		String current = callback.getCurrentClass().name.getValue();
		String currentSimple = current.substring(current.lastIndexOf("/") + 1);
		simpleToQuantified.put(currentSimple, current);

		context = new ContextType[text.length()];
		mappings = new AbstractMapping[text.length()];
		Arrays.fill(context, ContextType.UNKNOWN);
		Segment lastType = null;
		try {
			while (read.hasMore()) {
				String elem = read.nextWord();
				// Skip CFR comment
				if (lastType == null && elem.equals("/*")) {
					read.skipWords(5);
					continue;
				}

				if (elem.equals(ID_PACKAGE)) {
					lastType = Segment.PACKAGE;
					readPackage(read);
				} else if (elem.equals(ID_IMPORT)) {
					lastType = Segment.IMPORT;
					readImport(read);
				} else if (!inBody(lastType)) {
					lastType = readHeader(read, lastType, currentSimple, elem);
				} else {
					if (ID_MODIFIERS.contains(elem)) {
						while (ID_MODIFIERS.contains(elem)) {
							elem = read.nextWord();
						}
						if (elem.equals("/*")) {
							read.skipWords(2);
							elem = read.nextWord();
						}
						readMember(read, currentSimple, elem);
					} else if (thisType == ClassType.Enum){
						if (elem.endsWith(",") || elem.endsWith(";")){
							
						}
					}
					// TODO: What if there are no modifiers? IE: Default access
					// How would it be checked if it's a member and not some random data?
					//
					// else { readMember(read, currentSimple, elem); }
					//
					// TODO: Method body parsing.
					// 'this.FIELD_NAME' and 'new CLASS_NAME()`
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void readPackage(IndexableStringReader read) throws IOException {
		// Read the package the class is located in.
		String packge = read.nextWord();
		// TODO: Allow package remapping
	}

	private void readImport(IndexableStringReader read) throws IOException {
		// Read classes that have been imported,
		String imported = read.nextWord();
		// Replace '.' with '/' and substring off the ';'
		imported = imported.replace(".", "/").substring(0, imported.length() - 1);
		// Get mapping for referenced class, if it exists
		// TODO: ability to lookup classes that have been renamed
		ClassMapping cm = getClass(imported);
		if (cm != null) {
			fill(read, imported, cm);
		}
		// Get the simple name of the class and map it to the fully
		// quantified name.
		// Used for parsing body contents since the body uses the
		// simplified names.
		String importedSimple = imported;
		if (importedSimple.contains("/")) {
			importedSimple = importedSimple.substring(importedSimple.lastIndexOf("/") + 1);
		}
		simpleToQuantified.put(importedSimple, imported);
	}

	private Segment readHeader(IndexableStringReader read, Segment lastType, String currentSimple, String elem) throws IOException {
		// If the last words were dealing with packages or imports,
		// the next one is the class declaration.
		if (lastType == Segment.PACKAGE || lastType == Segment.IMPORT) {
			// Handle declaration
			boolean isClass = false, isEnum = false, isInterface = false;
			if ((isClass = elem.equals(ID_CLASS)) || (isEnum = elem.equals(ID_ENUM)) || (isInterface = elem.equals(ID_INTERFACE))) {
				String clazz = read.nextWord();
				if (clazz.equals(currentSimple)) {
					fill(read, clazz, callback.getCurrentClass());
					// Mark what kind of class this is. Will be used
					// for handling the body later.
					if (isClass) {
						thisType = ClassType.Normal;
					} else if (isEnum) {
						thisType = ClassType.Enum;
					} else if (isInterface) {
						thisType = ClassType.Interface;
					}
				} else {
					// Declared class is not the one selected from
					// the class tree menu...
					if (debug)
						System.out.println("NON-CURRENT CLASS: " + clazz);
				}
				lastType = Segment.CLASS;
			}

		} else if (lastType == Segment.CLASS) {
			// Marks the beginning of body parsing
			if (elem.equals("{")) {
				lastType = Segment.BODY;
			} else if (elem.equals(ID_IMPLEMENTS) || elem.equals(ID_EXTENDS)) {
				// extends, implements
				String clazz = read.nextWord();
				if (debug)
					System.out.println("MISSING EXTENDING / IMPLEMENTS: " + clazz);
			}
		}
		return lastType;
	}

	private void readMember(IndexableStringReader read, String currentSimple, String elem) throws IOException {
		String name = null;
		String retType = null;
		String type = elem;
		// Get the return type
		if (ID_PRIMITIVES.contains(type)) {
			retType = ID_PRIMITIVES_SYMBOL.get(ID_PRIMITIVES.indexOf(type));
		} else if (ID_LANG.contains(type)) {
			retType = ID_LANG_SYMBOL.get(ID_LANG.indexOf(type));
		} else if (simpleToQuantified.containsKey(type)) {
			String full = simpleToQuantified.get(type);
			retType = "L" + full + ";";
			ClassMapping cm = getClass(full);
			if (cm != null) {
				fill(read, type, cm);
			}
		} else if (type.startsWith(currentSimple + "(")) {
			// This is a constructor
			name = "<init>";
			retType = "V";
			int offset = type.length() - type.indexOf("(");
			fill(read, currentSimple, callback.getCurrentClass(), offset);
		} else {
			if (debug)
				System.out.println("UNKNOWN MEMBER TYPE: " + type);
		}
		// The name is set only if a constructor is detected.
		// Data should end up being:
		// - Field name + ';'
		// - Method name + '()'
		// - Method name + '(Arg1Type'
		String data;
		boolean isDataOfMethod = false;
		boolean fieldDeclared = false;
		if (name == null) {
			data = read.nextWord();
			if (data.contains("(")) {
				// Handle reading as method
				isDataOfMethod = true;
				name = data.substring(0, data.indexOf("("));
			} else {
				// Handle reading as field
				// We already have the return type so we're done
				// here.
				fieldDeclared = !data.endsWith(";");
				name = fieldDeclared ? data : data.substring(0, data.length() - 1);
			}
		} else {
			data = type;
		}

		// Handle parsing data
		if (isDataOfMethod) {
			// No arguments for method
			if (data.endsWith("()")) {
				MemberMapping mm = callback.getCurrentClass().getMemberMapping(name, "()" + retType);
				if (mm != null) {
					fill(read, name, mm, 2);
				}
			} else {
				// Has args
				// Record current point read index is at.
				// Used for calculating the range to fill the member mapping at.
				int nameIndex = read.getIndex() - (data.length() - data.indexOf("("));
				int arrayDepth = 0;
				String argType = data.substring(data.indexOf("(") + 1);
				// Collect arguments to build the full method desc.
				StringBuilder sbDesc = new StringBuilder("(");
				while (true) {
					while (argType.equals(ID_FINAL)) {
						argType = read.nextWord();
					}
					// Read array level from type
					while (argType.contains("[]")) {
						argType = argType.substring(0, argType.length() - 2);
						arrayDepth++;
					}
					// Fetch the arg's desc representation
					String desc = null;
					if (ID_LANG.contains(argType)) {
						desc = ID_LANG_SYMBOL.get(ID_LANG.indexOf(argType));
					} else if (ID_PRIMITIVES.contains(argType)) {
						desc = ID_PRIMITIVES_SYMBOL.get(ID_PRIMITIVES.indexOf(argType));
					} else {
						String argTypeFull = simpleToQuantified.get(argType);
						ClassMapping cm = getClass(argTypeFull);
						if (cm != null) {
							fill(read, argType, cm);
						}
						desc = "L" + argTypeFull + ";";

					}
					// Add array level to arg in desc
					while (arrayDepth > 0) {
						sbDesc.append("[");
						arrayDepth--;
					}
					// Append to method desc
					sbDesc.append(desc);
					arrayDepth = 0;
					String argName = read.nextWord();
					// Check if there are more args to parse or break the reader
					// loop if there are none.
					if (argName.endsWith(",")) {
						argType = read.nextWord();
					} else {
						break;
					}
				}
				// Finish up the method descriptor and
				sbDesc.append(")" + retType);
				MemberMapping mm = callback.getCurrentClass().getMemberMapping(name, sbDesc.toString());
				if (mm != null) {
					fill(read, name, mm, read.getIndex() - nameIndex);
				}
			}
		} else {
			// Fields are super simple
			MemberMapping mm = callback.getCurrentClass().getMemberMapping(name, retType);
			if (mm != null) {
				fill(read, name, mm, fieldDeclared ? 0 : 1);
			}
		}
	}

	private boolean inBody(Segment lastType) {
		return lastType == Segment.BODY;
	}

	enum Segment {
		PACKAGE, IMPORT, CLASS, BODY
	}

	enum ClassType {
		Normal, Interface, Enum
	}

	/**
	 * Sets the context over the range of the word's length to the given
	 * mapping.
	 * 
	 * @param read
	 * @param element
	 * @param mapping
	 */
	private void fill(IndexableStringReader read, String element, AbstractMapping mapping) {
		fill(read, element, mapping, 0);
	}

	/**
	 * Sets the context over the range of the word's length to the given
	 * mapping.
	 * 
	 * @param read
	 * @param element
	 * @param mapping
	 * @param leftShift
	 *            Offset to shift the filling by.
	 */
	private void fill(IndexableStringReader read, String element, AbstractMapping mapping, int leftShift) {
		int index = read.getIndex();
		int start = index - element.length() - leftShift;
		int end = index - leftShift;
		if (debug)
			System.out.println("\t" + start + ":" + end + " -> " + mapping.name.original);
		Arrays.fill(mappings, start, end, mapping);
	}

	/**
	 * Finds the class mapping given a quantified name.
	 * 
	 * @param name
	 * @return
	 */
	private ClassMapping getClass(String name) {
		ClassMapping cm = callback.getJarReader().getMapping().getMapping(name);
		if (cm == null && callback.getHistory().getRenamedToMappingMap().containsKey(name)) {
			return (ClassMapping) callback.getHistory().getRenamedToMappingMap().get(name);
		}
		return cm;
	}

	public AbstractMapping getMappingAtPoint(int index) {
		return mappings[index];
	}
}
