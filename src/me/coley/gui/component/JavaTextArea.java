package me.coley.gui.component;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import me.coley.gui.Gui;

@SuppressWarnings("serial")
public class JavaTextArea extends JPanel {
	private final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
	private final RTextScrollPane scrollText = new RTextScrollPane(textArea);
	private final Gui callback;

	public JavaTextArea(Gui callback) {
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
		//
		setLayout(new BorderLayout());
		add(scrollText, BorderLayout.CENTER);
	}
}
