package me.coley.jremapper.gui.action;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import me.coley.jremapper.gui.component.JavaTabs;

public class ActionCloseTab extends MouseAdapter {
	private JavaTabs tabs;

	public ActionCloseTab(JavaTabs tabs) {
		this.tabs = tabs;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int i = tabs.getSelectedIndex();
		if (isMiddleClick(e) && i >= 0) {
			tabs.remove(i);
		}
	}

	private static boolean isMiddleClick(MouseEvent e) {
		return e.getButton() == MouseEvent.BUTTON2;
	}
}
