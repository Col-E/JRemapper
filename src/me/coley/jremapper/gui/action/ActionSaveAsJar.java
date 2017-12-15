package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;

import me.coley.jremapper.JRemapper;

/**
 * Prompts a user to select a jar file.
 */
public class ActionSaveAsJar implements ActionListener {
	private JRemapper jremap;

	public ActionSaveAsJar(JRemapper jremap) {
		this.jremap = jremap;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser saver = jremap.createFileSaver();
		int val = saver.showOpenDialog(null);
		if (val == JFileChooser.APPROVE_OPTION) {
			jremap.onSaveJar(saver.getSelectedFile());
		}
	}

}