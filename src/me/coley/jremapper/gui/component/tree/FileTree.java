package me.coley.jremapper.gui.component.tree;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import me.coley.bmf.JarReader;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.util.StreamUtil;
import me.coley.jremapper.Program;
import me.coley.jremapper.gui.listener.FileSelectionListener;

@SuppressWarnings("serial")
public class FileTree extends JPanel {
	private final JTree tree = new JTree(new String[] { "Open a jar" });
	private final JScrollPane scrollTree = new JScrollPane(tree);
	private final Program callback;

	public FileTree(Program program) {
		this.callback = program;
		//
		try {
			tree.setCellRenderer(new FileTreeRenderer(callback));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		setLayout(new BorderLayout());
		add(scrollTree, BorderLayout.CENTER);
	}

	/**
	 * Updates the JTree with class files loaded into the given JarReader.
	 * 
	 * @param jar
	 */
	public void setup() {
		JarReader read = callback.getJarReader();
		// Root node
		String jarName = read.getFile().getName();
		MappingTreeNode root = new MappingTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		FileSelectionListener sel = new FileSelectionListener(tree, callback);
		tree.addTreeSelectionListener(sel);
		tree.addMouseListener(sel);
		tree.setModel(model);
		// Iterate sorted classes
		// Sorting is done here because sorting the list is way easier than
		// sorting the tree after.
		List<String> names = StreamUtil.listOfSortedJavaNames(read.getClassEntries().keySet());
		for (String className : names) {
			// Get mapping linked to class name
			ClassMapping mapping = read.getMapping().getMapping(className);
			String curName = mapping.name.getValue();
			// Create directory path based on current mapping stored name.
			List<String> dirPath = new ArrayList<>(Arrays.asList(curName.split("/")));
			// Create directory of nodes
			generateTreePath(root, dirPath, mapping, model);
		}
		model.setRoot(root);
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
		List<String> curPath = new ArrayList<>(Arrays.asList(curPathName.split("/")));
		List<String> newPath = new ArrayList<>(Arrays.asList(newPathName.split("/")));
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		//
		MappingTreeNode root = (MappingTreeNode) tree.getModel().getRoot();
		// Remove path
		removeTreePath(root, curPath, model);
		// Add path back
		generateTreePath(root, newPath, mapping, model);
	}

	/**
	 * Removes a path from a given parent node. Also updates the given model.
	 * 
	 * @param parent
	 * @param dirPath
	 * @param model
	 */
	private void removeTreePath(MappingTreeNode parent, List<String> dirPath, DefaultTreeModel model) {
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node = parent.getChild(section);
			// Create child if it doesn't exist.
			MappingTreeNode up = parent;
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
	}

	/**
	 * Adds a path to a given parent node. Also updates the given model.
	 * 
	 * @param parent
	 * @param dirPath
	 * @param mapping
	 * @param model
	 */
	private void generateTreePath(MappingTreeNode parent, List<String> dirPath, ClassMapping mapping, DefaultTreeModel model) {
		while (dirPath.size() > 0) {
			String section = dirPath.get(0);
			MappingTreeNode node;
			// Create child if it doesn't exist.
			if ((node = parent.getChild(section)) == null) {
				MappingTreeNode newDir = new MappingTreeNode(section, dirPath.size() == 1 ? mapping : null);
				parent.addChild(section, newDir);
				parent.add(newDir);
				// update model
				model.nodesWereInserted(parent, new int[] { parent.getIndex(newDir) });
				node = newDir;
			}
			parent = node;
			dirPath.remove(0);
		}
	}

	public void refresh() {
		JarReader read = callback.getJarReader();
		// Root node
		String jarName = read.getFile().getName();
		MappingTreeNode root = new MappingTreeNode(jarName, null);
		DefaultTreeModel model = new DefaultTreeModel(root);
		// FileSelectionListener sel = new FileSelectionListener(callback);
		// tree.addTreeSelectionListener(sel);
		// tree.addMouseListener(sel);
		tree.setModel(model);
		// Iterate classes
		List<String> names = StreamUtil.listOfSortedJavaNames(read.getClassEntries().keySet());
		for (String className : names) {
			if (!read.getClassEntries().containsKey(className)) {
				continue;
			}
			// Get mapping linked to class name
			ClassMapping mapping = read.getMapping().getMapping(className);
			String curName = mapping.name.getValue();
			// Create directory path based on current mapping stored name.
			ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(curName.split("/")));
			// Create directory of nodes
			generateTreePath(root, dirPath, mapping, model);
		}
		model.setRoot(root);
	}
}
