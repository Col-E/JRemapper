package me.coley.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.gui.component.JavaTextArea;

public class SwingUtil {
	/**
	 * Method by Adrian: [
	 * <a href="http://stackoverflow.com/a/15704264/5620200">StackOverflow</a> ]
	 * & Mike: [ <a href=
	 * "http://stackoverflow.com/questions/1542170/arranging-nodes-in-a-jtree">
	 * StackOverflow</a> ]
	 * 
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {
		List<DefaultMutableTreeNode> children = Collections.list(node.children());
		List<String> orgCnames = new ArrayList<String>();
		List<String> cNames = new ArrayList<String>();
		DefaultMutableTreeNode temParent = new DefaultMutableTreeNode();
		for (DefaultMutableTreeNode child : children) {
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode) child;
			temParent.insert(ch, 0);
			String upper = ch.toString().toUpperCase();
			// Not dependent on package name, so if duplicates are found
			// they will later on be confused. Adding this is of
			// very little consequence and fixes the issue.
			if (cNames.contains(upper)) {
				upper += "$COPY";
			}
			cNames.add(upper);
			orgCnames.add(upper);
			if (!child.isLeaf()) {
				sort(child);
			}
		}
		Collections.sort(cNames);
		for (String name : cNames) {
			int indx = orgCnames.indexOf(name);
			int insertIndex = node.getChildCount();
			node.insert(children.get(indx), insertIndex);
		}
		// Fixing folder placement
		for (int i = 0; i < node.getChildCount() - 1; i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			for (int j = i + 1; j <= node.getChildCount() - 1; j++) {
				DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
				if (!prevNode.isLeaf() && child.isLeaf()) {
					node.insert(child, j);
					node.insert(prevNode, i);
				}
			}
		}
		return node;
	}

	/**
	 * Stolen from http://stackoverflow.com/a/19935987/5620200
	 * 
	 * @param tree
	 * @param startingIndex
	 * @param rowCount
	 */
	public static void expand(JTree tree, int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			tree.expandRow(i);
		}

		if (tree.getRowCount() != rowCount) {
			expand(tree, rowCount, tree.getRowCount());
		}
	}
}