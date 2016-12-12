package me.coley.gui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import io.github.bmf.mapping.ClassMapping;
import io.github.bmf.mapping.MemberMapping;
import me.coley.Program;

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
