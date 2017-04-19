package me.coley.jremapper.gui.component.tree;

import java.util.HashMap;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.bmf.mapping.ClassMapping;

@SuppressWarnings("serial")
public class MappingTreeNode extends DefaultMutableTreeNode {
	private final Map<String, MappingTreeNode> children = new HashMap<String, MappingTreeNode>();
	private final ClassMapping mapping;

	public MappingTreeNode(String title, ClassMapping mapping) {
		super(title);
		this.mapping = mapping;
	}

	public MappingTreeNode getChild(String name) {
		return children.get(name);
	}

	public void addChild(String name, MappingTreeNode node) {
		children.put(name, node);
	}

	public ClassMapping getMapping() {
		return mapping;
	}
}