package me.coley.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import me.coley.CFRSetting;
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
	private FileTree fileTree;
	private JavaTextArea sourceArea;

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
			spMain.setDividerLocation(200);
			frmCfrRemapper.setTitle("CFR Remapper");
			frmCfrRemapper.setBounds(100, 100, 867, 578);
			frmCfrRemapper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frmCfrRemapper.getContentPane().add(spMain, BorderLayout.CENTER);
		}
		// Setting up the file tree for jar layouts
		fileTree = new FileTree(callback);
		{
			spMain.setLeftComponent(fileTree);
		}
		// Setting up the text area
		sourceArea = new JavaTextArea(callback);
		{
			spMain.setRightComponent(sourceArea);
		}
		// Setting up the menu
		JMenuBar menuBar = new JMenuBar();
		{
			frmCfrRemapper.setJMenuBar(menuBar);
			JMenu mnFile = new JMenu("File");
			{
				JMenu mnOpen = new JMenu("Open");
				{
					JMenuItem mntmOpenJar = new JMenuItem("Jar");
					JMenuItem mntmOpenMap = new JMenuItem("Mappings");
					mntmOpenJar.addActionListener(new ActionChooseFile(callback));
					mnOpen.add(mntmOpenJar);
					mnOpen.add(mntmOpenMap);
				}
				JMenu mnSave = new JMenu("Save As");
				{
					JMenuItem mntmSaveJar = new JMenuItem("Jar");
					JMenuItem mntmSaveMap = new JMenuItem("Mapping");
					mntmSaveMap.addActionListener(new ActionSaveAsMapping(callback));
					mntmSaveJar.addActionListener(new ActionSaveAsJar(callback));
					mnSave.add(mntmSaveJar);
					mnSave.add(mntmSaveMap);
				}
				mnFile.add(mnOpen);
				mnFile.add(mnSave);
			}
			JMenu mnOptions = new JMenu("Options");
			{
				for (final String setting : callback.getOptions().getOptions().keySet()) {
					boolean enabled = callback.getOptions().get(setting);
					final JCheckBox chk = new JCheckBox(setting);
					chk.setSelected(enabled);
					chk.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							callback.getOptions().set(setting, chk.isSelected());
						}
					});
					mnOptions.add(chk);
				}
			}
			JMenu mnCfr = new JMenu("CFR");
			{
				for (final CFRSetting setting : CFRSetting.values()) {
					final JCheckBox chk = new JCheckBox(setting.getText());
					chk.setSelected(setting.isEnabled());
					chk.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							setting.setEnabled(chk.isSelected());
						}
					});
					mnCfr.add(chk);
				}
			}
			menuBar.add(mnFile);
			menuBar.add(mnOptions);
			menuBar.add(mnCfr);
		}
	}

	public void setTitle(String string) {
		frmCfrRemapper.setTitle("CFR Remapper: " + string);
	}

	/**
	 * Displays the GUI.
	 */
	public void display() {
		frmCfrRemapper.setVisible(true);
	}

	public FileTree getFileTree() {
		return fileTree;
	}

	public JavaTextArea getSourceArea() {
		return sourceArea;
	}

}
