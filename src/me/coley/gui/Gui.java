package me.coley.gui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import java.awt.BorderLayout;

import me.coley.Program;
import me.coley.gui.component.FileTree;
import me.coley.gui.component.JavaTextArea;
import me.coley.gui.listener.*;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class Gui {
	private final Program callback;
	private final JFrame frmCfrRemapper = new JFrame();
	private FileTree tree;
	private JavaTextArea textArea;

	/**
	 * Create the application.
	 */
	public Gui(Program callback) {
		this.callback = callback;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	public void initialize() {
		JSplitPane spMain = new JSplitPane();
		// Setting up the frame
		{
			frmCfrRemapper.setTitle("CFR Remapper");
			frmCfrRemapper.setBounds(100, 100, 867, 578);
			frmCfrRemapper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frmCfrRemapper.getContentPane().add(spMain, BorderLayout.CENTER);
		}
		// Setting up the file tree for jar layouts
		tree = new FileTree(callback);
		{
			spMain.setLeftComponent(tree);
		}
		// Setting up the text area
		textArea = new JavaTextArea(callback);
		{
			spMain.setRightComponent(textArea);
		}
		// Setting up the menu
		JMenuBar menuBar = new JMenuBar();
		{
			frmCfrRemapper.setJMenuBar(menuBar);
			JMenu mnFile = new JMenu("File");
			JMenu mnSave = new JMenu("Save As...");
			JMenu mnOpen = new JMenu("Open...");
			JMenuItem mntmOpenJar = new JMenuItem("Jar");
			JMenuItem mntmOpenMap = new JMenuItem("Mappings");
			JMenuItem mntmSaveMap = new JMenuItem("Mapping");
			JMenuItem mntmSaveJar = new JMenuItem("Jar");
			JMenu mnCfr = new JMenu("CFR");
			mntmOpenJar.addActionListener(new ActionChooseFile(callback));
			mntmSaveMap.addActionListener(new ActionSaveAsMapping (callback));
			mntmSaveJar.addActionListener(new ActionSaveAsJar(callback));
			mnOpen.add(mntmOpenJar);
			mnOpen.add(mntmOpenMap);
			mnFile.add(mnOpen);
			mnFile.add(mnSave);
			mnSave.add(mntmSaveJar);
			mnSave.add(mntmSaveMap);
			menuBar.add(mnFile);
			menuBar.add(mnCfr);
		}
	}

	/**
	 * Displays the GUI.
	 */
	public void display() {
		frmCfrRemapper.setVisible(true);
	}

}
