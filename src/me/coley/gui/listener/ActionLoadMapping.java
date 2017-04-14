package me.coley.gui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;

import me.coley.Program;

public class ActionLoadMapping implements ActionListener {
	private final Program callback;

	public ActionLoadMapping(Program callback) {
		this.callback = callback;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = callback.getFileChooser(null, null);
		int val = chooser.showOpenDialog(null);
		if (val == JFileChooser.APPROVE_OPTION) {
			callback.onLoadMapping(chooser.getSelectedFile());
		}
	}

}
