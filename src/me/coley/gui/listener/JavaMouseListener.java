package me.coley.gui.listener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import me.coley.Program;
import me.coley.gui.component.JavaTextArea;

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
