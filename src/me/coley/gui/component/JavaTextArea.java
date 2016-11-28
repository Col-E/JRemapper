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

@SuppressWarnings("serial")
public class JavaTextArea extends JPanel {
	private final static List<String> fuck = Arrays.asList("for ", "try ", "do ", "if ", "catch ", "while ");
	private final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
	private final RTextScrollPane scrollText = new RTextScrollPane(textArea);
	private final Program callback;
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
		textArea.setEditable(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.addCaretListener(new JavaCaretListener(callback, this));
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
				String lineCopy = line;
				if (line.contains(" = ")) {
					lineCopy = line.substring(0, line.indexOf(" = "));
				}
				String[] lineSplit = lineCopy.split(" ");
				String lastWord = lineSplit[lineSplit.length - 1];
				// Does the last word return an empty list(checked against
				// members)?
				// if so: Method body / not a member
				// else: Detected member
				if (callback.getCurrentClass().getMembersByName(lastWord).isEmpty()) {
					context.add(LineContext.METHOD_BODY);
				} else {
					// TODO: Detect false positives
					context.add(LineContext.FIELD_DEC);
				}
			} else if (!line.endsWith(";") && line.contains("(") && line.contains(")") && line.endsWith("{")) {
				boolean isBody = false;
				for (String x : fuck) {
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
					if (callback.getCurrentClass().getMembersByName(sub).isEmpty()) {
						context.add(LineContext.METHOD_BODY);
					} else {
						// TODO: Detect false positives
						context.add(LineContext.METHOD_DEC);
					}
				}
				// Possibly method declaration
				// Else method body
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
	 * Returns the given context of a line.
	 * 
	 * @param line
	 * @return
	 */
	public LineContext getContext(int line) {
		return line < context.size() ? context.get(line) : LineContext.UNKNOWN;
	}
}
