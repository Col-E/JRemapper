package me.coley.gui.component;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import me.coley.LineContext;
import me.coley.Program;
import me.coley.gui.listener.JavaCaretListener;
import me.coley.gui.listener.JavaKeyListener;
import me.coley.gui.listener.JavaMouseListener;
import me.coley.util.StringUtil;

@SuppressWarnings("serial")
public class JavaTextArea extends JPanel {
	private final static List<String> INVALID_CONTENT = Arrays.asList("for ", "try ", "do ", "if ", "catch ", "while ");
	private final static List<String> MODS = Arrays.asList("public", "protected", "private", "static", "volatile", "abstract", "transient");
	private final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
	private final RTextScrollPane scrollText = new RTextScrollPane(textArea);
	private final Program callback;
	private final JavaCaretListener caret;
	private final JavaMouseListener mouse;
	private final JavaKeyListener keys;
	private List<LineContext> context;
	/**
	 * If true, moving mouse around is selecting new things. Set to false while
	 * the user is typing in a new name for currently selected thing.
	 */
	private boolean parsing;

	public JavaTextArea(Program callback) {
		this.callback = callback;
		//
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		//
		caret = new JavaCaretListener(callback, this);
		mouse = new JavaMouseListener(callback, this);
		keys = new JavaKeyListener(callback, this);
		textArea.addCaretListener(caret);
		textArea.addMouseListener(mouse);
		textArea.addKeyListener(keys);
		//
		setLayout(new BorderLayout());
		add(scrollText, BorderLayout.CENTER);
	}

	/**
	 * Returns the text of the text area.
	 * 
	 * @return
	 */
	public String getText() {
		return textArea.getText();
	}

	/**
	 * Updates the text of the text area and sets the caret position to 0.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		textArea.setText(text);
		textArea.moveCaretPosition(0);
		parsing = true;
		// parse lines for context
		Scanner scan = new Scanner(text);
		String current = callback.getCurrentClass().name.getValue();
		String currentSimple = current.substring(current.lastIndexOf("/") + 1);
		context = new ArrayList<>(text.split("\n").length);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("package")) {
				context.add(LineContext.PACKAGE);
			} else if (line.startsWith("import ")) {
				context.add(LineContext.IMPORT);
			} else if ((line.contains("class ") || line.contains("enum ") || line.contains("interface ")) && line.contains(currentSimple)) {
				context.add(LineContext.CLASS_DEC);
			} else if (line.startsWith("implements ")) {
				context.add(LineContext.IMPLEMENTS);
			} else if (line.startsWith("extends ")) {
				context.add(LineContext.EXTENDS);
			} else if (line.endsWith(";")) {
				// Get the last word in the line (substring if assignment
				// detected)
				if (line.contains(" = ")) {
					// I mean, it's assignment so it's some kind of assignment.
					context.add(LineContext.VALUE_DEC);
				} else {
					// Split trimed line
					String trim = line.substring(0, line.length() - 1).trim();
					String[] decSplit = trim.split(" ");
					// If it only has 1 or no args, just call it unknown.
					if (decSplit.length >= 2) {
						// Get name and type
						String name = decSplit[decSplit.length - 1];
						String type = decSplit[decSplit.length - 2];
						// Check if there are invalid chars in either
						String invalidCharInName = StringUtil.getFirstNonWordChar(name);
						String invalidCharInType = StringUtil.getFirstNonWordChar(type);
						// No invalid chars? Declaration.
						// else, who knows!
						if (invalidCharInName == null && invalidCharInType == null) {
							context.add(LineContext.VALUE_DEC);
						} else {
							// TODO: edge cases
							// private static /* synthetic */ int[] $NAME$
							context.add(LineContext.UNKNOWN);
						}
					} else context.add(LineContext.UNKNOWN);
				}
			} else if (!line.endsWith(";") && line.contains("(") && line.contains(")") && line.endsWith("{")) {
				boolean isBody = false;
				// Detect control flow being false-pos for method declaration
				for (String x : INVALID_CONTENT) {
					if (line.contains(x)) {
						isBody = true;
						break;
					}
				}
				if (isBody) {
					context.add(LineContext.METHOD_BODY);
				} else {
					String sub = line.substring(0, line.indexOf("("));
					sub = sub.substring(sub.lastIndexOf(" ") + 1);
					// Does the substring return an empty list(checked against
					// members)?
					// if so: Method body / not a member
					// else: Detected member
					if (callback.getCurrentClass().getMembersByOriginalName(sub).isEmpty()) {
						context.add(LineContext.METHOD_BODY);
					} else {
						// TODO: See if there are any false positives and re-do
						// this logic if so.
						context.add(LineContext.METHOD_DEC);
					}
				}
			} else {
				context.add(LineContext.UNKNOWN);
			}
		}
		scan.close();
	}

	/**
	 * Current parsing status.
	 * 
	 * @return See {@linkplain #parsing}.
	 */
	public boolean canParse() {
		return parsing;
	}

	/**
	 * Set's parsing status.
	 * 
	 * @param parsing
	 *            See {@linkplain #parsing}.
	 */
	public void setCanParse(boolean parsing) {
		this.parsing = parsing;
	}

	/**
	 * Sets the text area's editable status.
	 * 
	 * @param vslue
	 */
	public void setEditable(boolean vslue) {
		this.textArea.setEditable(vslue);
	}

	/**
	 * Returns the given context of a line.
	 * 
	 * @param line
	 * @return
	 */
	public LineContext getContext(int line) {
		return line < context.size() ? context.get(line) : LineContext.UNKNOWN;
	}

	/**
	 * Returns the position of the caret in the text area.
	 * 
	 * @return
	 */
	public int getCaretPosition() {
		return textArea.getCaretPosition();
	}

	/**
	 * Sets the position of the caret in the text area.
	 * 
	 * @param pos
	 */
	public void setCaretPosition(int pos) {
		textArea.setCaretPosition(pos);
	}

	/**
	 * Returns the text area's caret listener.
	 * 
	 * @return
	 */
	public JavaCaretListener getCaretListener() {
		return caret;
	}

	/**
	 * Returns the text area's mouse listener.
	 * 
	 * @return
	 */
	public JavaMouseListener getMouseListener() {
		return mouse;
	}

	/**
	 * Returns the text area's key listener.
	 * 
	 * @return
	 */
	public JavaKeyListener getKeyListener() {
		return keys;
	}

}
