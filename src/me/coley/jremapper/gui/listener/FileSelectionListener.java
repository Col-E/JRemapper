package me.coley.jremapper.gui.listener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import me.coley.jremapper.JRemapper;
import me.coley.jremapper.gui.component.tree.MappingTreeNode;

public class FileSelectionListener implements TreeSelectionListener, MouseListener {
	private final JRemapper jremap;
	private TreeNode lastNode;
	private JTree tree;

	public FileSelectionListener(JTree tree, JRemapper jremap) {
		this.jremap = jremap;
		this.tree = tree;
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
			if (tree.getSelectionPath().getLastPathComponent().equals(node)) {
				MappingTreeNode mtn = (MappingTreeNode) node;
				jremap.onClassSelect(mtn.getMapping());
			}
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
