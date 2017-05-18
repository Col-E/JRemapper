package me.coley.jremapper.gui.listener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import me.coley.bmf.mapping.AbstractMapping;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.Options;
import me.coley.jremapper.Program;
import me.coley.jremapper.gui.component.JavaTextArea;
import me.coley.jremapper.util.StringUtil;

public class JavaKeyListener implements KeyListener {
	private final Program callback;
	private final JavaTextArea text;

	public JavaKeyListener(Program callback, JavaTextArea text) {
		this.callback = callback;
		this.text = text;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// Make sure the selection data isn't
		// updated while the user is typing.
		text.setCanParse(false);
		int c = e.getKeyCode();
		if (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN) {
			// Movement via typing is exempted
			text.setCanParse(true);
		} else if (c == KeyEvent.VK_ENTER) {
			e.consume();
			// Send result
			AbstractMapping am = text.getSelectedMapping();
			ClassMapping cm = am instanceof ClassMapping ? (ClassMapping) am : null;
			MemberMapping mm = am instanceof MemberMapping ? (MemberMapping) am : null;
			int pos = text.getCaretPosition();
			if (cm != null) {
				boolean reg = callback.getOptions().get(Options.REGEX_REPLACE_CLASSES);
				String orig = cm.name.original;
				String origCut = orig.substring(orig.lastIndexOf("/") + 1);
				String value = StringUtil.getWordAtIndex(text.getCaretPosition(), text.getText(), true);
				String current = cm.name.getValue();
				// Update rename history
				callback.getHistory().onRename(cm, current, value);
				// Update tree path
				// TODO: Account for inner classes with $ names
				callback.updateTreePath(orig, value);
				// Rename mapping
				cm.name.setValue(value);
				if (cm.equals(callback.getCurrentClass())) {
					// Close tab with now outdated name
					callback.getWindow().removeTab(current);
				}
				// Update text area.
				if (reg) {
					String valueCut = cm.name.getValue().substring(cm.name.getValue().lastIndexOf("/") + 1);
					text.setText(text.getText().replaceAll("\\b(" + origCut + ")\\b", valueCut));
				} else {
					callback.onClassSelect(callback.getCurrentClass());
				}
				// TODO: Better positon reset
				text.setCaretPosition(pos);
			} else if (mm != null) {
				boolean reg = callback.getOptions().get(Options.REGEX_REPLACE_MEMBERS);
				String orig = mm.name.original;
				String origCut = orig.substring(orig.lastIndexOf("/") + 1);
				String value = StringUtil.getWordAtIndex(text.getCaretPosition(), text.getText(), true);
				// Update rename history
				callback.getHistory().onRename(mm, mm.name.getValue(), value);
				// Rename mapping
				mm.name.setValue(value);
				// Update text area.
				if (reg) {
					String valueCut = mm.name.getValue().substring(mm.name.getValue().lastIndexOf("/") + 1);
					text.setText(text.getText().replaceAll("\\b(" + origCut + ")\\b", valueCut));
				} else {
					callback.onClassSelect(callback.getCurrentClass());
				}
				// TODO: Better positon reset
				text.setCaretPosition(pos);
			}
			text.setEditable(false);
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
