package me.coley.jremapper.gui.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.bmf.mapping.AbstractMapping;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.jremapper.JRemapper;
import me.coley.jremapper.search.Search;

@SuppressWarnings("serial")
public class MemberSelectionMenu extends JPopupMenu {

	public MemberSelectionMenu(JRemapper jremap, JavaTextArea text) {
		super("Selection:" + text.getSelectedMapping().name.getValue());
		AbstractMapping am = text.getSelectedMapping();
		if (am instanceof ClassMapping) {
			ClassMapping cm = (ClassMapping) am;
			if (hasNodeForMapping(cm, jremap)) {
				JMenuItem itemOpenTab = new JMenuItem("Open in new tab");
				itemOpenTab.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						jremap.onClassSelect(cm);
					}
				});
				add(itemOpenTab);
				//
				JMenuItem itemReferencesFields = new JMenuItem("Search for field references");
				itemReferencesFields.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						DefaultMutableTreeNode root = jremap.getSearcher().searchClass(Search.CLASS_REFERENCE_FIELDS, cm.name.getValue());
						jremap.getWindow().getSearchPanel().setResults(root);
					}
				});
				add(itemReferencesFields);
				//
				JMenuItem itemReferencesMethod = new JMenuItem("Search for method references");
				itemReferencesMethod.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						DefaultMutableTreeNode root = jremap.getSearcher().searchClass(Search.CLASS_REFERENCE_METHODS, cm.name.getValue());
						jremap.getWindow().getSearchPanel().setResults(root);
					}
				});
				add(itemReferencesMethod);
			}
		} else if (am instanceof MemberMapping){
			MemberMapping mm = (MemberMapping) am;
			// TODO: Better search for reference.
			// Narrow it down by the exact owner of the member. 
			JMenuItem itemReferences= new JMenuItem("Search for references");
			itemReferences.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DefaultMutableTreeNode root = jremap.getSearcher().searchMember(jremap.getCurrentClass(), mm);
					jremap.getWindow().getSearchPanel().setResults(root);
				}
			});
			add(itemReferences);
		}
	}

	private boolean hasNodeForMapping(ClassMapping cm, JRemapper jremap) {
		return jremap.getJarReader().getClassEntries().containsKey(cm.name.original);
	}
}
