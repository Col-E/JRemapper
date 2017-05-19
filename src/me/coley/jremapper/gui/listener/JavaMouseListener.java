package me.coley.jremapper.gui.listener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import me.coley.jremapper.Program;
import me.coley.jremapper.gui.component.JavaTextArea;
import me.coley.jremapper.gui.component.MemberSelectionMenu;

public class JavaMouseListener implements MouseListener {
	private final Program callback;
	private final JavaTextArea text;

	public JavaMouseListener(Program callback, JavaTextArea text) {
		this.callback = callback;
		this.text = text;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		text.setCanParse(true);
		if (e.getButton() == MouseEvent.BUTTON3 &&  text.getSelectedMapping() != null) {
			new MemberSelectionMenu(callback, text).show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

}
