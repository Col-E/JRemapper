package me.coley.gui.component;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import io.github.bmf.JarReader;
import io.github.bmf.util.mapping.ClassMapping;
import me.coley.Program;
import me.coley.gui.JavaCellRenderer;
import me.coley.gui.listener.FileSelectionListener;
import meme.SwingUtils;

@SuppressWarnings("serial")
public class FileTree extends JPanel {
	private final JTree tree = new JTree(new String[] { "Open a jar" });
	private final JScrollPane scrollTree = new JScrollPane(tree);
	private final Program callback;

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
		tree.addTreeSelectionListener(new FileSelectionListener(callback));
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
		removeTreePath(root, curPath, model);
		// Add path back
		generateTreePath(root, newPath, mapping, model);
	}

	private MappingTreeNode removeTreePath(MappingTreeNode parent, ArrayList<String> dirPath, DefaultTreeModel model) {
		MappingTreeNode up = null;
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node = parent.getChild(section);
			// Create child if it doesn't exist.
			up = parent;
			if (dirPath.size() == 1) {
				// update model
				model.removeNodeFromParent(node);
				while (up.isLeaf() && up.getMapping() == null) {
					String lastSection = up.toString();
					up = (MappingTreeNode) up.getParent();
					// update model
					model.removeNodeFromParent(up.getChild(lastSection));
				}
			}
			parent = node;
			dirPath.remove(0);
		}
		return up;
	}

	private MappingTreeNode generateTreePath(MappingTreeNode parent, ArrayList<String> dirPath, ClassMapping mapping, DefaultTreeModel model) {
		MappingTreeNode newDir = null;
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node;
			// Create child if it doesn't exist.
			if ((node = parent.getChild(section)) == null) {
				newDir = new MappingTreeNode(section, dirPath.size() == 1 ? mapping : null);
				parent.addChild(section, newDir);
				parent.add(newDir);
				// update model
				model.nodesWereInserted(parent, new int[] { parent.getIndex(newDir) });
				node = newDir;
			}
			parent = node;
			dirPath.remove(0);
		}
		return newDir;
	}
}
