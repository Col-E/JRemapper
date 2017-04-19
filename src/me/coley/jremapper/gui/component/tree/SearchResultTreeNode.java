package me.coley.jremapper.gui.component.tree;

import javax.swing.tree.DefaultMutableTreeNode;

@SuppressWarnings("serial")
public class SearchResultTreeNode extends DefaultMutableTreeNode {
	private MappingTreeNode mappingNode;

	public SearchResultTreeNode(MappingTreeNode mappingNode, String title) {
		super(title);
		this.mappingNode = mappingNode;
	}
	
	public MappingTreeNode getMappingNode(){
		return mappingNode;
	}
}