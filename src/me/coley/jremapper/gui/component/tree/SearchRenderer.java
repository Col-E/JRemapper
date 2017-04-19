package me.coley.jremapper.gui.component.tree;


import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import me.coley.bmf.ClassNode;
import me.coley.bmf.util.Access;
import me.coley.jremapper.Program;

@SuppressWarnings("serial")
public class SearchRenderer extends DefaultTreeCellRenderer {
	private static final Icon ICON_PACKAGE;
	private static final Icon ICON_CLASS;
	private static final Icon ICON_INTERFACE;
	private static final Icon ICON_ENUM;
	private static final Icon ICON_ANNOTATION;
	private static final Icon ICON_RESULT;
	private final Program callback;

	public SearchRenderer(Program callback) {
		this.callback = callback;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getChildCount() > 0) {
			if (node instanceof MappingTreeNode) {
				MappingTreeNode mtNode = (MappingTreeNode) node;
				if (mtNode.getMapping() == null) {
					// The root node of the tree has no mapping.
					// The root isn't DefaultMutableTreeNode because otherwise
					// it makes the code for generating the tree a uglier. This
					// if statement is the exchange.
					setIcon(SearchRenderer.ICON_CLASS);
				} else {
					// Get the classnode, determine icon by access
					String className = mtNode.getMapping().name.original;
					ClassNode cn = callback.getJarReader().getClassEntries().get(className);
					int acc = cn.access;
					if (Access.isInterface(acc)) {
						setIcon(SearchRenderer.ICON_INTERFACE);
					} else if (Access.isEnum(acc)) {
						setIcon(SearchRenderer.ICON_ENUM);
					} else if (Access.isAnnotation(acc)) {
						setIcon(SearchRenderer.ICON_ANNOTATION);
					} else {
						setIcon(SearchRenderer.ICON_CLASS);
					}
				}
			}else {
				// Package that will contain MappingTreeNode sub-nodes
				setIcon(SearchRenderer.ICON_PACKAGE);
			}
		} else {
			if (node instanceof SearchResultTreeNode){
				setIcon(SearchRenderer.ICON_RESULT);
			}
			
		}
		return this;
	}

	static {
		Toolkit kit = Toolkit.getDefaultToolkit();
		Class<SearchRenderer> thisClass = SearchRenderer.class;
		ICON_PACKAGE = new ImageIcon(kit.getImage(thisClass.getResource("/resources/package.png")));
		ICON_CLASS = new ImageIcon(kit.getImage(thisClass.getResource("/resources/class.png")));
		ICON_INTERFACE = new ImageIcon(kit.getImage(thisClass.getResource("/resources/interface.png")));
		ICON_ENUM = new ImageIcon(kit.getImage(thisClass.getResource("/resources/enum.png")));
		ICON_ANNOTATION = new ImageIcon(kit.getImage(thisClass.getResource("/resources/annotation.png")));
		ICON_RESULT = new ImageIcon(kit.getImage(thisClass.getResource("/resources/result.png")));
	}
}