package me.coley.gui.listener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import io.github.bmf.util.mapping.ClassMapping;
import me.coley.Program;
import me.coley.gui.component.JavaTextArea;
import me.coley.util.StringUtil;

public class JavaKeyListener implements KeyListener {
	private final Program callback;
	private final JavaTextArea text;

	public JavaKeyListener(Program callback, JavaTextArea text) {
		this.callback = callback;
		this.text = text;
	}

	@Override
	public void keyPressed(KeyEvent e) {// Make sure the selection data isn't
										// updated while the user is typing.
		text.setCanParse(false);
		int c = e.getKeyCode();
		if (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN) {
			// Movement via typing is exempted
			text.setCanParse(true);
		} else if (c == KeyEvent.VK_ENTER) {
			// Send result
			text.setEditable(false);
			ClassMapping cm = text.getCaretListener().getSelectedMapping();
			String orig = cm.name.original;
			String value = StringUtil.getWordAtIndex(text.getCaretPosition(), text.getText(), true);
			// Update tree path
			callback.updateTreePath(orig, value);
			// Rename mapping
			cm.name.setValue(value);
			// Update text area.
			// TODO: Instead of re-decompile, have an option for lazy-replacement. 
			// IE: Regex. Would help for larger classes.
			callback.onClassSelect(callback.getCurrentClass());
		} else if (c == KeyEvent.VK_SPACE) {
			// Replace word with package
		} else {
			// Editing
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

}
