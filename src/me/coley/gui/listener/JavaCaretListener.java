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
import io.github.bmf.util.ConstUtil;
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
				String className = line.substring(0, line.length()-1).split(" ")[1].replace(".", "/");
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
				List<MemberMapping> possMembers = callback.getCurrentClass().getMembersByName(this.word);
				if (possMembers.size() > 0) {
					// Ok so it's likely a method name
					// TODO: If there is more than one run further tests to
					// determine which method it is
					this.mappedMember = possMembers.get(0);
				} else {
					// Likely a parameter or return type.
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
						if (sub.equals(this.word))
							this.mappedClass = callback.getJarReader().getMapping().getMapping(cname);
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
					callback.getWindow().setTitle(" CL<" + this.mappedClass.name.getValue() + ">");
				} else {
					callback.getWindow().setTitle(" FM<" + this.mappedMember.name.getValue() + ">");
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
	 * Detects a class based on the current selection.
	 * 
	 * @return
	 */
	private ClassMapping detectClass() {
		for (Entry<String, ClassMapping> entry : callback.getJarReader().getMapping().getMappings().entrySet()) {
			ClassMapping cm = entry.getValue();
			String nameOriginal = entry.getKey();
			String nameCurrent = cm.name.getValue();
			String cutOrig = nameOriginal.substring(nameOriginal.lastIndexOf("/") + 1);
			String cutCurr = nameCurrent.substring(nameCurrent.lastIndexOf("/") + 1);
			if (word.equals(cutOrig) || word.equals(cutCurr)) {
				return cm;
			}
		}
		return null;
	}

	/**
	 * Detects a member based on the current selection.
	 * 
	 * @return
	 */
	private MemberMapping detectMember() {
		for (MemberMapping mm : callback.getCurrentClass().getMembers()) {
			String nameOriginal = mm.name.original;
			String nameCurrent = mm.name.getValue();
			if (word.equals(nameOriginal) || word.equals(nameCurrent)) {
				return mm;
			}
		}
		return null;
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
