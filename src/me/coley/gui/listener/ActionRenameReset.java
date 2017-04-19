package me.coley.gui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import me.coley.Program;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;

public class ActionRenameReset implements ActionListener {

	private Program callback;

	public ActionRenameReset(Program callback) {
		this.callback = callback;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ClassMapping cm = callback.getCurrentClass();
		if (cm != null) {
			for (MemberMapping mm : cm.getMembers()) {
				mm.name.setValue(mm.name.original);
			}
			callback.onClassSelect(cm);
		}
	}
}
