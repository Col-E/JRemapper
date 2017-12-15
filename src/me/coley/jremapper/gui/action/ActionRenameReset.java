package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.JRemapper;

public class ActionRenameReset implements ActionListener {

	private JRemapper jremap;

	public ActionRenameReset(JRemapper jremap) {
		this.jremap = jremap;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ClassMapping cm = jremap.getCurrentClass();
		if (cm != null) {
			for (MemberMapping mm : cm.getMembers()) {
				mm.name.setValue(mm.name.original);
			}
			jremap.onClassSelect(cm);
		}
	}
}
