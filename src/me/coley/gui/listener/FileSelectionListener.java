package me.coley.gui.listener;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import me.coley.Program;
import me.coley.gui.component.MappingTreeNode;

public class FileSelectionListener implements TreeSelectionListener {
	private final Program callback;

	public FileSelectionListener(Program callback) {
		this.callback = callback;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreeNode node = (TreeNode) e.getPath().getLastPathComponent();
		if (node.isLeaf() && (node instanceof MappingTreeNode)){
			MappingTreeNode mtn = (MappingTreeNode) node;
			callback.onClassSelect(mtn.getMapping());
		}
	}

}
