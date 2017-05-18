package me.coley.jremapper.gui.component;

import javax.swing.JLabel;

/**
 * A label with HTML tooltips. This will primarily be used in formatting
 * multi-line tooltips.
 */
@SuppressWarnings("serial")
public class HTMLLabel extends JLabel {
	public HTMLLabel(String text) {
		super(text);
	}

	public void setToolTipText(String msg) {
		super.setToolTipText("<html>" + msg + "</html>");
	}
}
