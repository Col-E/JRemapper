package me.coley.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import me.coley.CFRSetting;
import me.coley.Options;
import me.coley.Program;
import me.coley.gui.component.JavaTextArea;
import me.coley.gui.component.tree.FileTree;
import me.coley.gui.listener.*;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MainWindow {
	private final Program callback;
	private final JFrame frame = new JFrame();
	private FileTree fileTree;
	private JavaTextArea currentSource;
	private JTabbedPane tabbedClasses;
	private Map<String, JavaTextArea> tabToText = new HashMap<>();

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
			frame.setTitle("JRemapper");
			frame.setBounds(100, 100, 867, 578);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(spMain, BorderLayout.CENTER);
		}
		// Setting up the file tree for jar layouts
		fileTree = new FileTree(callback);
		{
			spMain.setLeftComponent(fileTree);
		}
		// Setting up the decompile area
		// Tabbed panel will have tabs containing JavaTextAreas.
		tabbedClasses = new JTabbedPane();
		{
			// Keep a history of which classes were opened.
			tabbedClasses.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int tab = tabbedClasses.getSelectedIndex();

					System.out.println("Tab is: " + (tab + 1));
				}
			});

			// Add ability to middle-click tabs to close them.
			tabbedClasses.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (SwingUtilities.isMiddleMouseButton(e)) {
						int i = tabbedClasses.getSelectedIndex();
						String title = tabbedClasses.getTitleAt(i);
						if (tabToText.containsKey(title)) {
							tabToText.remove(title);
						}
						tabbedClasses.remove(i);
					}
				}
			});
			spMain.setRightComponent(tabbedClasses);
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
					JMenuItem mntmOpenMap = new JMenuItem("Mappings");
					mntmOpenJar.addActionListener(new ActionChooseFile(callback));
					mntmOpenMap.addActionListener(new ActionLoadMapping(callback));
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
					mntmRenameUnique.addActionListener(new ActionRenameClasses(callback));
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
		frame.setTitle("JRemapper: " + string);
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

	public JavaTextArea getS2ourceArea() {
		return currentSource;
	}

	/**
	 * Opens a
	 * 
	 * @param title
	 * @param text
	 */
	public void openTab(String title, String text) {
		JTextArea textArea = new JTextArea(text);
		this.tabbedClasses.addTab(title, textArea);
	}

	public void openSourceTab(String title, String decomp) {
		// Try to get existing text area:
		/// - If it does not exist, create a new tab.
		/// - If it does exist, update content and set it as the current tab.
		JavaTextArea javaArea = tabToText.get(title);
		int index = tabbedClasses.getTabCount();
		if (javaArea == null) {
			javaArea = new JavaTextArea(callback);
			javaArea.setText(decomp);
			tabbedClasses.addTab(title, javaArea);
			tabToText.put(title, javaArea);
			tabbedClasses.setSelectedIndex(index);
		} else {
			// Re-decompile if option for refreshing is active
			if (callback.getOptions().get(Options.REFRESH_ON_SELECT)) {
				int caret = javaArea.getCaretPosition();
				javaArea.setText(decomp);
				javaArea.setCaretPosition(caret);
			}
			// Find tab with title
			for (int i = 0; i < tabbedClasses.getTabCount(); i++) {
				if (tabbedClasses.getTitleAt(i).equals(title)) {
					index = i;
					break;
				}
			}
			tabbedClasses.setSelectedIndex(index);
		}
		currentSource = javaArea;
	}
}
