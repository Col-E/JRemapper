package me.coley.jremapper.gui.component;

import javax.swing.JLabel;

@SuppressWarnings("serial")
public class HTMLLabel extends JLabel {
	public HTMLLabel(String text) {
		super(text);
	}

	public void setToolTipText(String msg){
		super.setToolTipText("<html>" + msg + "</html>");
	}
}
