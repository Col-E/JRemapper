package me.coley.gui.listener;

import java.util.List;
import java.util.Map.Entry;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import io.github.bmf.ClassNode;
import io.github.bmf.consts.ConstClass;
import io.github.bmf.consts.ConstantType;
import io.github.bmf.mapping.ClassMapping;
import io.github.bmf.mapping.MemberMapping;
import io.github.bmf.type.PrimitiveType;
import io.github.bmf.type.Type;
import io.github.bmf.type.descriptors.MethodDescriptor;
import io.github.bmf.type.descriptors.VariableDescriptor;
import io.github.bmf.util.ConstUtil;
import javafx.geometry.Pos;
import me.coley.LineContext;
import me.coley.Program;
import me.coley.gui.component.JavaTextArea;
import me.coley.util.StringUtil;

public class JavaCaretListener implements CaretListener {
	private final Program callback;
	private final JavaTextArea text;
	private int selectedLine;
	private LineContext lineContext;
	private String lineContent;
	private String word, lastText;
	private ClassMapping mappedClass;
	private MemberMapping mappedMember;

	public JavaCaretListener(Program callback, JavaTextArea text) {
		this.callback = callback;
		this.text = text;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		if (!text.canParse()) {
			return;
		}
		int dot = e.getDot();
		if (dot == 0 || dot >= text.getText().length() - 1) {
			return;
		}
		// Getting the line data
		int lineEnd = text.getText().substring(dot).indexOf("\n");
		String firstPart = text.getText().substring(0, dot + lineEnd);
		int lineStart = firstPart.lastIndexOf("\n") + 1;
		String line = firstPart.substring(lineStart, firstPart.length());
		String word = StringUtil.getWordAtIndex(dot - lineStart, line);
		// Parsing the line data
		this.selectedLine = firstPart.split("\n").length - 1;
		this.lineContext = text.getContext(selectedLine);
		this.lineContent = line;
		this.mappedClass = null;
		this.mappedMember = null;
		if (word.length() > 0) {
			this.word = word;
			callback.getWindow().setTitle("<" + lineContext.name() + ">");
			switch (lineContext) {
			case CLASS_DEC:
				// It's the current class
				this.mappedClass = callback.getCurrentClass();
				break;
			case EXTENDS:
				// It's the parent
				this.mappedClass = callback.getJarReader().getMapping().getParent(callback.getCurrentClass());
				break;
			case IMPLEMENTS:
				// It's one of the interfaces
				List<ClassMapping> inters = callback.getJarReader().getMapping().getInterfaces(callback.getCurrentClass());
				for (ClassMapping in : inters) {
					String name = in.name.getValue();
					if (name.contains("/"))
						name = name.substring(name.lastIndexOf("/") + 1);
					if (word.equals(name)) {
						this.mappedClass = in;
						break;
					}
				}
				break;
			case IMPORT:
				// The class name is explicitly defined
				String className = line.substring(0, line.length() - 1).split(" ")[1].replace(".", "/");
				System.err.println(className);
				this.mappedClass = callback.getJarReader().getMapping().getMapping(className);
				break;
			case METHOD_BODY:
				// It could be anything
				// TODO: Determine if having it follow the declaration logic is
				// a not horrible idea.
			case VALUE_DEC:
				// TODO: Should VALUE_DEC be separate?
			case METHOD_DEC:
				// It could be the member name, a parameter, a value, or return
				// type.
				// TODO: Check for types
				List<MemberMapping> possMembers = callback.getCurrentClass().getMembersByName(this.word);
				if (possMembers.size() > 0) {
					// Only one possible option.
					if (possMembers.size() == 1) {
						this.mappedMember = possMembers.get(0);
						break;
					}
					// Ok so there's a couple of things it can be.
					// First lets check if it's a method/field and compare the
					// descriptors of the matches.
					// Discard obvious mis-matches.
					//
					// Word with space is because space is always present before
					// the word if it truly is a type/name of something.
					String sw = " " + this.word;
					// Get the index of the word in the line.
					// Determined by patterns used by field/method declarations.
					int baseIndx = this.lineContent.indexOf(sw + "("); // method
					if (baseIndx == -1)
						baseIndx = this.lineContent.indexOf(sw + " = "); // field
					if (baseIndx == -1)
						baseIndx = this.lineContent.indexOf(sw + ";"); // field
					int afterWordIndx = baseIndx + sw.length();
					boolean isMethod = this.lineContent.charAt(afterWordIndx) == '(';
					boolean isField = this.lineContent.charAt(afterWordIndx) == ';' || this.lineContent.substring(afterWordIndx).startsWith(" = ");
					if (!isMethod && !isField) {
						// TODO: Determine what this is. Likely that the user
						// clicked on the return type or a parameter.
						System.out.println("a: " + this.lineContent.substring(afterWordIndx));
						break;
					}
					for (MemberMapping mm : possMembers) {
						// Methods have a ( in their description.
						if (isMethod) {
							if (!mm.desc.original.contains("("))
								continue;
							// Calculating number of args
							// commas used under the assumption generics aren't
							// in the args like Map<K, V>
							boolean hasParams = this.lineContent.indexOf("()") == -1;
							int commas = hasParams ? StringUtil.countOccurrences(this.lineContent, ',') : 0;
							int params = hasParams ? 1 + (commas) : 0;
							MethodDescriptor md = (MethodDescriptor) mm.desc;
							// Checking the number of args
							if (md.parameters.size() != params) {
								continue;
							}
							// Checking return type
							String retType = this.lineContent.substring(0, this.lineContent.indexOf(sw + "("));
							retType = retType.substring(retType.lastIndexOf(" ") + 1, retType.length());
							// For primitives convert desc to name and check if
							// types
							// are equal.
							// For objects check if desc contains return
							// decompiled type name.
							if (md.returnType instanceof PrimitiveType) {
								PrimitiveType prim = (PrimitiveType) md.returnType;
								if (!retType.equals(prim.toJavaName()))
									continue;
							} else if (!md.returnType.toDesc().contains(retType))
								continue;
						}
						// Fields do not have a ( in their description
						if (isField) {
							if (mm.desc.original.contains("("))
								continue;
							VariableDescriptor vd = (VariableDescriptor) mm.desc;
							String retType = this.lineContent.substring(0, this.lineContent.indexOf(sw));
							retType = retType.substring(retType.lastIndexOf(" ") + 1);
							// For primitives convert desc to name and check if
							// types
							// are equal.
							// For objects check if desc contains return
							// decompiled type name.
							if (vd.type instanceof PrimitiveType) {
								PrimitiveType prim = (PrimitiveType) vd.type;
								if (!retType.equals(prim.toJavaName()))
									continue;
							} else if (!vd.type.toDesc().contains(retType))
								continue;
						}
						this.mappedMember = mm;
					}
				} else {
					// Likely a parameter or return type (class name)
					// Type is cross referenced with imports (checking class
					// constants)
					//
					// Get current ClassNode
					String name = callback.getCurrentClass().name.original;
					ClassNode cn = callback.getJarReader().getClassEntries().get(name);
					// This shouldn't happen, but just in case.
					if (cn == null)
						break;
					// Iterate class constants, check if there's a match
					List<ConstClass> clist = ConstUtil.getConstants(cn, ConstantType.CLASS);
					for (ConstClass cc : clist) {
						String cname = ConstUtil.getUTF8String(cn, cc.getValue());
						String sub = cname;
						if (sub.contains("/"))
							sub = sub.substring(sub.lastIndexOf("/") + 1);
						// Since the exact types aren't included (Sting rather
						// than java.lang.String) only the class's name can be
						// compared, the package is ignored (bad, but IDK how to
						// account for it easily).
						if (sub.equals(this.word)) {
							this.mappedClass = callback.getJarReader().getMapping().getMapping(cname);
						}
						// If the mapped class could not be found:
						// TODO: Optimize this so the entire jar doesn't have to
						// be iterated...
						if (this.mappedClass == null) {
							for (String pre : callback.getJarReader().getClassEntries().keySet()) {
								ClassMapping post = callback.getJarReader().getMapping().getMapping(pre);
								if (post.name.getValue().equals(cname)) {
									this.mappedClass = post;
								}
							}
						}
					}
				}
				break;
			case PACKAGE:
				// TODO: Special case for this to rename entire package
				break;
			case UNKNOWN:
				// It could be anything or nothing.
				break;
			default:
				break;
			}
			this.lastText = text.getText();
			// Enable interaction if a selection has been found
			if (this.mappedClass != null || this.mappedMember != null) {
				if (this.mappedClass != null) {
					callback.getWindow().setTitle(" CC " + this.mappedClass.name.getValue());
				} else {
					String type = this.mappedMember.desc.original.contains("(") ? "MM" : "FF";
					callback.getWindow().setTitle(" " + type + " " + this.mappedMember.name.getValue() + " " + this.mappedMember.desc.toDesc());
				}
				text.setEditable(true);
			} else {
				System.err.println("Nothing - " + word);
			}
		} else {
			this.word = null;
			// Disable interaction
			text.setEditable(false);
		}
	}

	/**
	 * Returns the current line based on the caret position.
	 * 
	 * @return
	 */
	public int getSelectedLine() {
		return selectedLine;
	}

	/**
	 * Returns the current line ({@linkplain #getSelectedLine()}) context.
	 * 
	 * @return
	 */
	public LineContext getLineContext() {
		return lineContext;
	}

	/**
	 * Returns the text of the current line ({@linkplain #getSelectedLine()}).
	 * 
	 * @return
	 */
	public String getLineContent() {
		return lineContent;
	}

	/**
	 * Returns the text of the TextArea at the time the last mapping was
	 * detected.
	 * 
	 * @return
	 */
	public String getLastText() {
		return lastText;
	}

	/**
	 * Returns the selected word in the current line (
	 * {@linkplain #getLineContent()}).
	 * 
	 * @return Null if selection is invalid. Single word otherwise.
	 */
	public String getWord() {
		return word;
	}

	/**
	 * Returns the ClassMapping based on the current selection (
	 * {@linkplain #getWord()}).
	 * 
	 * @return
	 */
	public ClassMapping getClassMapping() {
		return mappedClass;
	}

	/**
	 * Returns the MemberMapping based on the current selection (
	 * {@linkplain #getWord()}).
	 * 
	 * @return
	 */
	public MemberMapping getMemberMapping() {
		return mappedMember;
	}
}
