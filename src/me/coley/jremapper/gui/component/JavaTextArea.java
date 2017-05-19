package me.coley.jremapper.gui.component;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import me.coley.bmf.mapping.AbstractMapping;
import me.coley.jremapper.Program;
import me.coley.jremapper.gui.listener.JavaCaretListener;
import me.coley.jremapper.gui.listener.JavaKeyListener;
import me.coley.jremapper.gui.listener.JavaMouseListener;
import me.coley.jremapper.parse.Context;

@SuppressWarnings("serial")
public class JavaTextArea extends JPanel {
	private final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
	private final RTextScrollPane scrollText = new RTextScrollPane(textArea);
	private final JavaCaretListener caret;
	private final JavaMouseListener mouse;
	private final JavaKeyListener keys;
	private AbstractMapping lastMapping;
	private Context context;
	/**
	 * If true, moving mouse around is selecting new things. Set to false while
	 * the user is typing in a new name for currently selected thing.
	 */
	private boolean parsing;

	public JavaTextArea(Program callback) {
		context = new Context(callback);
		//
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setComponentPopupMenu(null);
		textArea.setPopupMenu(null);
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
		context.parse(text);
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
	 * @param value
	 */
	public void setEditable(boolean value) {
		this.textArea.setEditable(value);
	}

	public AbstractMapping getSelectedMapping() {
		if (parsing) {
			return lastMapping = context.getMappingAtPoint(getCaretPosition());
		} else {
			return lastMapping;
		}
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
