package me.coley.gui;

import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Slightly modified from Luyten's.
 */
@SuppressWarnings("serial")
public class JavaCellRenderer extends DefaultTreeCellRenderer {
	private final Icon folder;
	private final Icon file;

	public JavaCellRenderer() {
		this.folder = new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/package_obj.png")));
		this.file = new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/java.png")));
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getChildCount() > 0) {
			setIcon(this.folder);
		} else {
			setIcon(this.file);
		}
		return this;
	}
}
