package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;

import me.coley.jremapper.JRemapper;

public class ActionLoadMapping implements ActionListener {
	private final JRemapper jremap;

	public ActionLoadMapping(JRemapper jremap) {
		this.jremap = jremap;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = jremap.getFileChooser(null, null);
		int val = chooser.showOpenDialog(null);
		if (val == JFileChooser.APPROVE_OPTION) {
			jremap.onLoadMapping(chooser.getSelectedFile());
		}
	}

}
