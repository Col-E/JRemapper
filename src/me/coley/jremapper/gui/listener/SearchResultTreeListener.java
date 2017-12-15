package me.coley.jremapper.gui.listener;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import me.coley.jremapper.JRemapper;
import me.coley.jremapper.gui.component.tree.SearchResultTreeNode;

public class SearchResultTreeListener implements TreeSelectionListener, MouseListener {
	private final JRemapper jremap;
	private TreeNode lastNode;

	public SearchResultTreeListener(JRemapper jremap) {
		this.jremap = jremap;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreeNode node = (TreeNode) e.getPath().getLastPathComponent();
		this.lastNode = node;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		TreeNode node = lastNode;
		if (node != null) {
			/*
			if (node instanceof MappingTreeNode) {
				MappingTreeNode mtn = (MappingTreeNode) node;
				jremap.onClassSelect(mtn.getMapping());
			} else 
			*/
			if (node instanceof SearchResultTreeNode) {
				SearchResultTreeNode srtn = (SearchResultTreeNode) node;
				jremap.onClassSelect(srtn.getMappingNode().getMapping());
				// TODO: Find a way to accurately show the data, rather than just opening the class
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
