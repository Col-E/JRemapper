package me.coley.gui.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import io.github.bmf.mapping.AbstractMapping;
import io.github.bmf.mapping.ClassMapping;
import me.coley.Program;

@SuppressWarnings("serial")
public class MemberSelectionMenu extends JPopupMenu {

	public MemberSelectionMenu(Program callback, JavaTextArea text) {
		super("Selection:" + text.getSelectedMapping().name.getValue());
		AbstractMapping am = text.getSelectedMapping();
		if (am instanceof ClassMapping) {
			ClassMapping cm = (ClassMapping) am;
			if (callback.getJarReader().getClassEntries().containsKey(cm.name.original)) {
				JMenuItem itemOpenTab = new JMenuItem("Open in new tab");
				itemOpenTab.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {

						callback.onClassSelect(cm);

					}
				});
				add(itemOpenTab);
			}
		}
	}
}
