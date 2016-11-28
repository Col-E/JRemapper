package me.coley.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import me.coley.gui.component.FileTree;
import me.coley.gui.component.JavaTextArea;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class Gui {
	private JFrame frmCfrRemapper;
	private FileTree tree;
	private JavaTextArea textArea;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Gui window = new Gui();
					window.frmCfrRemapper.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Gui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		JSplitPane spMain = new JSplitPane();
		// Setting up the frame
		frmCfrRemapper = new JFrame();
		{
			frmCfrRemapper.setTitle("CFR Remapper");
			frmCfrRemapper.setBounds(100, 100, 867, 578);
			frmCfrRemapper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frmCfrRemapper.getContentPane().add(spMain, BorderLayout.CENTER);
		}
		// Setting up the file tree for jar layouts
		tree = new FileTree(this);
		{
			spMain.setLeftComponent(tree);
		}
		// Setting up the text area
		textArea = new JavaTextArea(this);
		{
			spMain.setRightComponent(textArea);
		}
		// Setting up the menu
		JMenuBar menuBar = new JMenuBar();
		{
			frmCfrRemapper.setJMenuBar(menuBar);
			JMenu mnFile = new JMenu("File");
			JMenuItem mntmOpen = new JMenuItem("Open");
			JMenu mnSaveAs = new JMenu("Save As...");
			JMenuItem mntmMapping = new JMenuItem("Mapping");
			JMenuItem mntmJar = new JMenuItem("Jar");
			JMenu mnCfr = new JMenu("CFR");
			menuBar.add(mnFile);
			mnFile.add(mntmOpen);
			mnFile.add(mnSaveAs);
			mnSaveAs.add(mntmMapping);
			mnSaveAs.add(mntmJar);
			menuBar.add(mnCfr);
		}
	}

}
