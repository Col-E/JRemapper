package me.coley.jremapper.util;

import javax.swing.JTree;

public class SwingUtil {
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