package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;

import me.coley.jremapper.Program;

/**
 * Prompts a user to select a jar file.
 */
public class ActionSaveAsMapping implements ActionListener {
	private Program callback;

	public ActionSaveAsMapping(Program callback) {
		this.callback = callback;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser saver = callback.createFileSaver();
		int val = saver.showOpenDialog(null);
		if (val == JFileChooser.APPROVE_OPTION) {
			callback.onSaveMappings(saver.getSelectedFile());
		}
	}

}