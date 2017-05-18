package me.coley.jremapper.gui;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.jremapper.CFRSetting;
import me.coley.jremapper.Options;
import me.coley.jremapper.Program;
import me.coley.jremapper.History.RenameAction;
import me.coley.jremapper.gui.action.*;
import me.coley.jremapper.gui.component.JavaTextArea;
import me.coley.jremapper.gui.component.NSplitPane;
import me.coley.jremapper.gui.component.SearchPanel;
import me.coley.jremapper.gui.component.tree.FileTree;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class MainWindow {
	private final Program callback;
	private final JFrame frame = new JFrame();
	private FileTree pnlFileTree;
	private SearchPanel pnlSearch;
	private JavaTabs pnlJavaTabs;
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
		// Creating the menu bar
		setupMenu();
		// Creating the components to be added to the frame.
		setupFrame();
		setupFileTree();
		setupSearchPanel();
		setupSourceTabs();
		// Create the splitpane that will contain the components
		NSplitPane nsplit = new NSplitPane(3);
		setupSplitView(nsplit);
	}

	private void setupMenu() {
		JMenuBar menuBar = new JMenuBar();
		setupFileMenu(menuBar);
		setupOptionsMenu(menuBar);
		setupCFRMenu(menuBar);
		setupMappingMenu(menuBar);
		setupHistoryMenu(menuBar);
		frame.setJMenuBar(menuBar);
	}

	private void setupHistoryMenu(JMenuBar menuBar) {
		JMenu mnHistory = new JMenu("History");
		// Create menu for opening/selecting the last few decompiled classes.
		JMenu mnHistSelection = new JMenu("Selections");
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
		// Create menu for undoing the last couple rename actions.
		JMenu mnHistRename = new JMenu("Renames");
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
							 * if (rename.getMapping() instanceof ClassMapping)
							 * { callback.updateTreePath(rename.
							 * getMapping().name.original, rename.getBefore());
							 * }
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
		// Add to menu bar.
		menuBar.add(mnHistory);
	}

	private void setupMappingMenu(JMenuBar menuBar) {
		JMenu mnMapping = new JMenu("Mapping");
		// Automated tasks for the current class
		JMenu mnMapCurrent = new JMenu("Current Class");
		JMenuItem mntmRenameUnique = new JMenuItem("Rename-all uniquely");
		JMenuItem mntmResetMembers = new JMenuItem("Reset members");
		mntmRenameUnique.addActionListener(new ActionRenameCurrentMembers(callback));
		mntmResetMembers.addActionListener(new ActionRenameReset(callback));
		mnMapCurrent.add(mntmRenameUnique);
		mnMapCurrent.add(mntmResetMembers);
		mnMapping.add(mnMapCurrent);
		// Automated tasks for all loaded classes
		JMenu mnMapAll = new JMenu("All Classes");
		JMenuItem mntmRenameUniqueAll = new JMenuItem("Rename-all: Simple");
		mntmRenameUniqueAll.addActionListener(new ActionRenameClasses(callback));
		mnMapAll.add(mntmRenameUniqueAll);
		mnMapping.add(mnMapAll);
		// Add to menu bar.
		menuBar.add(mnMapping);
	}

	private void setupFileMenu(JMenuBar menuBar) {
		JMenu mnFile = new JMenu("File");
		// Open options
		JMenu mnOpen = new JMenu("Open");
		JMenuItem mntmOpenJar = new JMenuItem("Jar");
		JMenuItem mntmOpenMap = new JMenuItem("Mappings");
		mntmOpenJar.addActionListener(new ActionChooseFile(callback));
		mntmOpenMap.addActionListener(new ActionLoadMapping(callback));
		mnOpen.add(mntmOpenJar);
		mnOpen.add(mntmOpenMap);
		// Save options
		JMenu mnSave = new JMenu("Save As");
		JMenuItem mntmSaveJar = new JMenuItem("Jar");
		JMenuItem mntmSaveMap = new JMenuItem("Mapping");
		mntmSaveMap.addActionListener(new ActionSaveAsMapping(callback));
		mntmSaveJar.addActionListener(new ActionSaveAsJar(callback));
		mnSave.add(mntmSaveJar);
		mnSave.add(mntmSaveMap);
		// Add to menu bar.
		mnFile.add(mnOpen);
		mnFile.add(mnSave);
		menuBar.add(mnFile);
	}

	private void setupOptionsMenu(JMenuBar menuBar) {
		JMenu mnOptions = new JMenu("Options");
		// Iterate program options, create a checkbox for each.
		// TODO: Options for non-boolean options.
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
		// Add to menu bar.
		menuBar.add(mnOptions);
	}

	private void setupCFRMenu(JMenuBar menuBar) {
		JMenu mnCfr = new JMenu("CFR");
		// Iterate CFR options, create a checkbox for each option.
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
		// Add to menu bar.
		menuBar.add(mnCfr);
	}

	private void setupFrame() {
		frame.setTitle("JRemapper");
		frame.setBounds(100, 100, 920, 578);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void setupFileTree() {
		pnlFileTree = new FileTree(callback);
	}

	private void setupSearchPanel() {
		pnlSearch = new SearchPanel(callback);
	}

	private void setupSourceTabs() {
		pnlJavaTabs = new JavaTabs();
		// Keep a history of which classes were opened.
		pnlJavaTabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = pnlJavaTabs.getSelectedIndex();
				if (i < 0) {
					return;
				}
				String title = pnlJavaTabs.getTitleAt(i);
				callback.getHistory().onSelectClass(title);
			}
		});

		// Add ability to middle-click tabs to close them.
		pnlJavaTabs.addMouseListener(new ActionCloseTab(pnlJavaTabs));
	}

	private void setupSplitView(NSplitPane nsplit) {
		frame.getContentPane().add(nsplit, BorderLayout.CENTER);
		nsplit.addNComponent(0, 200, pnlFileTree);
		nsplit.addNComponent(2, 200, pnlSearch);
		nsplit.addNComponent(1, 600, pnlJavaTabs);
		// Finish split-pane porportions
		nsplit.setNDivider(0, 150);
		nsplit.setNDivider(1, 600);
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
		this.pnlJavaTabs.addTab(title, textArea);
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
		JavaTextArea javaArea = pnlJavaTabs.getSourceArea(title);
		if (javaArea == null) {
			javaArea = new JavaTextArea(callback);
			javaArea.setText(text);
			pnlJavaTabs.addTab(title, javaArea);
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
		return pnlJavaTabs.selectTab(title);
	}

	/**
	 * Closes the tab by the given name. If no such tab exists nothing happens.
	 * 
	 * @param title
	 *            Name of the tab to close.
	 * @return true if success, false if failure.
	 */
	public boolean removeTab(String title) {
		return pnlJavaTabs.removeTab(title);

	}

	/**
	 * Sets the window's title to the given text.
	 * 
	 * @param text
	 *            Text to append.
	 */
	public void setTitle(String text) {
		if (text == null) {
			frame.setTitle("JRemapper");
		} else {
			frame.setTitle("JRemapper: " + text);
		}
	}

	/**
	 * Displays the GUI.
	 */
	public void display() {
		frame.setVisible(true);
	}

	/**
	 * Return the FileTree.
	 * 
	 * @return
	 */
	public FileTree getFileTree() {
		return pnlFileTree;
	}

	/**
	 * Return the currently displayed JavaTextArea.
	 * 
	 * @return
	 */
	public JavaTextArea getSourceArea() {
		return currentSource;
	}
}
