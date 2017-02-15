package me.coley.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import me.coley.CFRSetting;
import me.coley.Program;
import me.coley.RenameType;
import me.coley.gui.component.FileTree;
import me.coley.gui.component.JavaTextArea;
import me.coley.gui.listener.*;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MainWindow {
	private final Program callback;
	private final JFrame frame = new JFrame();
	private FileTree fileTree;
	private JavaTextArea sourceArea;

	/**
	 * Create the application.
	 */
	public MainWindow(Program callback) {
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
			frame.setTitle("CFR Remapper");
			frame.setBounds(100, 100, 867, 578);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(spMain, BorderLayout.CENTER);
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
			frame.setJMenuBar(menuBar);
			JMenu mnFile = new JMenu("File");
			{
				JMenu mnOpen = new JMenu("Open");
				{
					JMenuItem mntmOpenJar = new JMenuItem("Jar");
					JMenuItem mntmOpenDependency = new JMenuItem("Dependency Jar");
					JMenuItem mntmOpenMap = new JMenuItem("Mappings");
					mntmOpenJar.addActionListener(new ActionChooseFile(callback));
					mntmOpenDependency.addActionListener(new ActionAddLibrary(callback));
					mntmOpenMap.addActionListener(new ActionLoadMapping(callback));
					mnOpen.add(mntmOpenJar);
					mnOpen.add(mntmOpenDependency);
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
			JMenu mnMapping = new JMenu("Mapping");
			{
				JMenu mnMapCurrent = new JMenu("Current Class");
				{
					JMenuItem mntmRenameUnique = new JMenuItem("Rename-all uniquely");
					JMenuItem mntmResetMembers = new JMenuItem("Reset members");
					mntmRenameUnique.addActionListener(new ActionRenameCurrentMembers(callback));
					mntmResetMembers.addActionListener(new ActionRenameReset(callback));
					mnMapCurrent.add(mntmRenameUnique);
					mnMapCurrent.add(mntmResetMembers);
					mnMapping.add(mnMapCurrent);
				}
				JMenu mnMapAll = new JMenu("All Classes");
				{
					JMenuItem mntmRenameUnique = new JMenuItem("Rename-all: Simple");
					mntmRenameUnique.addActionListener(new ActionRenameClasses(callback, RenameType.SIMPLE));
					mnMapAll.add(mntmRenameUnique);
					mnMapping.add(mnMapAll);
				}
			}
			menuBar.add(mnFile);
			menuBar.add(mnOptions);
			menuBar.add(mnCfr);
			menuBar.add(mnMapping);
		}
	}

	public void setTitle(String string) {
		frame.setTitle("CFR Remapper: " + string);
	}

	/**
	 * Displays the GUI.
	 */
	public void display() {
		frame.setVisible(true);
	}

	public FileTree getFileTree() {
		return fileTree;
	}

	public JavaTextArea getSourceArea() {
		return sourceArea;
	}

}
