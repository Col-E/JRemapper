package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;

import me.coley.jremapper.JRemapper;

/**
 * Prompts a user to select a jar file.
 */
public class ActionChooseFile implements ActionListener {
	private final JRemapper jremap;

	public ActionChooseFile(JRemapper jremap) {
		this.jremap = jremap;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = jremap.getFileChooser();
		int val = chooser.showOpenDialog(null);
		if (val == JFileChooser.APPROVE_OPTION) {
			jremap.onFileSelect(chooser.getSelectedFile());
		}
	}

}