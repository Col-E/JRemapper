package me.coley.gui.listener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import me.coley.Program;
import me.coley.gui.component.tree.MappingTreeNode;

public class FileSelectionListener implements TreeSelectionListener, MouseListener {
	private final Program callback;
	private TreeNode lastNode;

	public FileSelectionListener(Program callback) {
		this.callback = callback;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreeNode node = (TreeNode) e.getPath().getLastPathComponent();
		this.lastNode = node;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		TreeNode node = lastNode;
		if (node != null && node.isLeaf() && (node instanceof MappingTreeNode)) {
			MappingTreeNode mtn = (MappingTreeNode) node;
			callback.onClassSelect(mtn.getMapping());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
