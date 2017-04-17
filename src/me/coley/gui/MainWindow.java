package me.coley.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import io.github.bmf.mapping.ClassMapping;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import me.coley.CFRSetting;
import me.coley.History.RenameAction;
import me.coley.Options;
import me.coley.Program;
import me.coley.gui.component.JavaTextArea;
import me.coley.gui.component.NSplitPane;
import me.coley.gui.component.SearchPanel;
import me.coley.gui.component.tree.FileTree;
import me.coley.gui.listener.*;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MainWindow {
	private final Program callback;
	private final JFrame frame = new JFrame();
	private FileTree pnlFileTree;
	private SearchPanel pnlSearch;
	private JTabbedPane pnlTabbedSources;
	private Map<String, JavaTextArea> tabToSource = new HashMap<>();
	private JavaTextArea currentSource;


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
		// Setting up the main 3-way view
		setupSplitView();
		// Setting up the menu
		setupMenu();
	}

	private void setupSplitView() {
		NSplitPane nsplit = new NSplitPane(3);
		// Setting up the frame
		{
			// spMain.setDividerLocation(200);
			frame.setTitle("JRemapper");
			frame.setBounds(100, 100, 920, 578);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(nsplit, BorderLayout.CENTER);
		}
		// Setting up the file tree for jar layouts
		pnlFileTree = new FileTree(callback);
		{
			nsplit.addNComponent(0, 200, pnlFileTree);
			// spMain.setLeftComponent(fileTree);
		}
		// Setting up the decompile area
		// Tabbed panel will have tabs containing JavaTextAreas.
		pnlTabbedSources = new JTabbedPane();
		{
			// Keep a history of which classes were opened.
			pnlTabbedSources.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = pnlTabbedSources.getSelectedIndex();
					if (i < 0) {
						return;
					}
					String title = pnlTabbedSources.getTitleAt(i);
					callback.getHistory().onSelectClass(title);
				}
			});

			// Add ability to middle-click tabs to close them.
			pnlTabbedSources.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON2) {
						int i = pnlTabbedSources.getSelectedIndex();
						if (i < 0) {
							return;
						}
						String title = pnlTabbedSources.getTitleAt(i);
						if (tabToSource.containsKey(title)) {
							tabToSource.remove(title);
						}
						pnlTabbedSources.remove(i);
					}
				}
			});
			nsplit.addNComponent(1, 600, pnlTabbedSources);
			// spMain.setRightComponent(tabbedClasses);
		}
		// Set up the search results panel
		pnlSearch = new SearchPanel(this, callback);
		{
			nsplit.addNComponent(2, 200, pnlSearch);
		}
		// Finish split-pane porportions
		nsplit.setNDivider(0, 150);
		nsplit.setNDivider(1, 600);
	}

	private void setupMenu() {
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
			JMenu mnHistory = new JMenu("History");
			{
				JMenu mnHistSelection = new JMenu("Selections");
				{
					callback.getHistory().registerSelectionUpdate(new Runnable() {
						@Override
						public void run() {
							mnHistSelection.removeAll();
							for (String title : callback.getHistory().getSelectedClasses()) {
								JMenuItem mntmSel = new JMenuItem(title);
								mntmSel.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										boolean opened = selectTab(title);
										if (!opened) {
											// TODO: Get class by looking up
											// renames
											// This won't open renamed classes
											// right now.
											ClassMapping mc = callback.getJarReader().getMapping().getMapping(title);
											if (mc != null) {
												callback.onClassSelect(mc);
											}
										}
									}
								});
								mnHistSelection.add(mntmSel);
							}
						}

					});
					mnHistory.add(mnHistSelection);
				}
				JMenu mnHistRename = new JMenu("Renames");
				{
					callback.getHistory().registerSelectionUpdate(new Runnable() {
						@Override
						public void run() {
							mnHistRename.removeAll();
							for (RenameAction rename : callback.getHistory().getRenameActions()) {
								String title = rename.getBefore() + " -> " + rename.getAfter();
								JMenuItem mntmRen = new JMenuItem(title);

								mntmRen.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										rename.getMapping().name.setValue(rename.getBefore());
										/*
										 * if (rename.getMapping() instanceof
										 * ClassMapping) {
										 * callback.updateTreePath(rename.
										 * getMapping().name.original,
										 * rename.getBefore()); }
										 */
										callback.getHistory().onUndo(rename);
										// TODO: This is a lazy fix, only the
										// single class should need to be moved.
										if (rename.getMapping() instanceof ClassMapping) {
											callback.refreshTree();
										}
									}
								});

								mnHistRename.add(mntmRen);
							}
						}

					});
					mnHistory.add(mnHistRename);
				}
			}
			menuBar.add(mnFile);
			menuBar.add(mnOptions);
			menuBar.add(mnCfr);
			menuBar.add(mnMapping);
			menuBar.add(mnHistory);
		}
	}

	/**
	 * Opens a tab with a plaintext title and text.
	 * 
	 * @param title
	 *            Tab title.
	 * @param text
	 *            Message.
	 */
	public void openTab(String title, String text) {
		JTextArea textArea = new JTextArea(text);
		this.pnlTabbedSources.addTab(title, textArea);
	}

	/**
	 * Opens a tab with a title and decompiled class's contents.
	 * 
	 * @param title
	 *            Tab title.
	 * @param text
	 *            Decompiled class contents.
	 */
	public void openSourceTab(String title, String text) {
		// Try to get existing text area:
		/// - If it does not exist, create a new tab.
		/// - If it does exist, update content and set it as the current tab.
		JavaTextArea javaArea = tabToSource.get(title);
		if (javaArea == null) {
			int index = pnlTabbedSources.getTabCount();
			javaArea = new JavaTextArea(callback);
			javaArea.setText(text);
			pnlTabbedSources.addTab(title, javaArea);
			tabToSource.put(title, javaArea);
			pnlTabbedSources.setSelectedIndex(index);
		} else {
			// Re-decompile if option for refreshing is active
			if (callback.getOptions().get(Options.REFRESH_ON_SELECT)) {
				int caret = javaArea.getCaretPosition();
				javaArea.setText(text);
				javaArea.setCaretPosition(caret);
			}
			// Find tab with title
			selectTab(title);
		}
		currentSource = javaArea;
	}

	/**
	 * Sets the current tab based on a given title.
	 * 
	 * @param title
	 *            Name of the tab to select.
	 * @return true if success, false if failure.
	 */
	private boolean selectTab(String title) {
		int index = -1;
		for (int i = 0; i < pnlTabbedSources.getTabCount(); i++) {
			if (pnlTabbedSources.getTitleAt(i).equals(title)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		pnlTabbedSources.setSelectedIndex(index);
		return true;
	}

	/**
	 * Closes the tab by the given name. If no such tab exists nothing happens.
	 * 
	 * @param title
	 *            Name of the tab to close.
	 * @return true if success, false if failure.
	 */
	public boolean closeTab(String title) {
		int index = -1;
		for (int i = 0; i < pnlTabbedSources.getTabCount(); i++) {
			if (pnlTabbedSources.getTitleAt(i).equals(title)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		pnlTabbedSources.remove(index);
		return true;
	}

	public void setTitle(String string) {
		if (string == null) {
			frame.setTitle("JRemapper");
		} else {
			frame.setTitle("JRemapper: " + string);
		}
	}

	/**
	 * Displays the GUI.
	 */
	public void display() {
		frame.setVisible(true);
	}

	public FileTree getFileTree() {
		return pnlFileTree;
	}

	public JavaTextArea getSourceArea() {
		return currentSource;
	}
}
