package me.coley.gui.component;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import io.github.bmf.JarReader;
import io.github.bmf.util.mapping.ClassMapping;
import me.coley.Program;
import me.coley.gui.Gui;
import me.coley.gui.JavaCellRenderer;
import meme.SwingUtils;

@SuppressWarnings("serial")
public class FileTree extends JPanel {
	private final JTree tree = new JTree();
	private final JScrollPane scrollTree = new JScrollPane(tree);
	private final Program callback;

	// TODO: See if editing JTree is faster than 100% regeneration
	public FileTree(Program program) {
		this.callback = program;
		//
		tree.setCellRenderer(new JavaCellRenderer());
		//
		setLayout(new BorderLayout());
		add(scrollTree, BorderLayout.CENTER);
	}

	/**
	 * Updates the JTree with class files loaded into the given JarReader.
	 * 
	 * @param jar
	 */
	public void setup(JarReader read) {
		// Root node
		String jarName = read.getFile().getName();
		MappingTreeNode root = new MappingTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		// Iterate classes
		for (String className : read.getClassEntries().keySet()) {
			// Get mapping linked to class name
			ClassMapping mapping = read.getMapping().getMapping(className);
			String curName = mapping.name.getValue();
			// Create directory path based on current mapping stored name.
			ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(curName.split("/")));
			// Create directory of nodes
			generateTreePath(root, dirPath, mapping, model);
		}
		model.setRoot(SwingUtils.sort(root));
	}

	/**
	 * Updates a single node in the JTree.
	 * 
	 * @param read
	 * @param originalName
	 *            Class in tree to be updated.
	 */
	public void update(JarReader read, String originalName, String newPathName) {
		ClassMapping mapping = read.getMapping().getMapping(originalName);
		String curPathName = mapping.name.getValue();
		ArrayList<String> curPath = new ArrayList<String>(Arrays.asList(curPathName.split("/")));
		ArrayList<String> newPath = new ArrayList<String>(Arrays.asList(newPathName.split("/")));
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		//
		MappingTreeNode root = (MappingTreeNode) tree.getModel().getRoot();
		// Remove path
		removeTreePath(root,  curPath, model);
		// Add path back
		generateTreePath(root, newPath, mapping, model);
		// Refresh
		model.reload(root);
	}

	private void removeTreePath(MappingTreeNode parent, ArrayList<String> dirPath, DefaultTreeModel model) {
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node = parent.getChild(section);
			// Create child if it doesn't exist.
			if (dirPath.size() == 1) {
				node.removeFromParent();
				MappingTreeNode up = parent;
				while (up.isLeaf() && up.getMapping() == null){
					String lastSection = up.toString();
					up = (MappingTreeNode) up.getParent();
					up.remove(up.getChild(lastSection));
				}
				return;
			}
			parent = node;
			dirPath.remove(0);
		}
	}

	private void generateTreePath(MappingTreeNode parent, ArrayList<String> dirPath, ClassMapping mapping, DefaultTreeModel model) {
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node;
			// Create child if it doesn't exist.
			if ((node = parent.getChild(section)) == null) {
				MappingTreeNode newDir = new MappingTreeNode(section, dirPath.size() == 1 ? mapping : null);
				parent.addChild(section, newDir);
				parent.add(newDir);
				node = newDir;
			}
			parent = node;
			dirPath.remove(0);
		}
	}
}
