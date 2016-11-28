package me.coley.gui.component;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import me.coley.Program;
import me.coley.gui.Gui;
import me.coley.gui.JavaCellRenderer;

@SuppressWarnings("serial")
public class FileTree extends JPanel {
	private final JTree tree = new JTree();
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
}
