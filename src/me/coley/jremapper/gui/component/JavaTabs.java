package me.coley.jremapper.gui.component;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;

@SuppressWarnings("serial")
public class JavaTabs extends JTabbedPane {
	/**
	 * Map of tab titles to text areas containing the decompiled source.
	 */
	private Map<String, JavaTextArea> tabToSource = new HashMap<>();

	/**
	 * Adds a tab with the given title containing the given component <i>(of
	 * type JavaTextArea)</i> to the tabbed pane.
	 * 
	 * @param title
	 *            Title of tab to add.
	 * @param component
	 *            Component to fill the tab content. Must be a JavaTextArea.
	 */
	@Override
	public void addTab(String title, Component component) {
		if (component instanceof JavaTextArea) {
			JavaTextArea text = (JavaTextArea) component;
			int index = getTabCount();
			tabToSource.put(title, text);
			super.addTab(title, component);
			setSelectedIndex(index);
		} else {
			throw new RuntimeException("Component was not a supported type!");
		}
	}

	/**
	 * Return the JavaTextArea associated with the given tab title.
	 * 
	 * @param title
	 *            Title of a tab containing a JavaTextArea component.
	 * @return JavaTextArea tab with the given title.
	 */
	public JavaTextArea getSourceArea(String title) {
		return tabToSource.get(title);
	}

	/**
	 * Removes a tab at the given index.
	 * 
	 * @param index
	 */
	@Override
	public void remove(int index) {
		String title = getTitleAt(index);
		if (hasTab(title)) {
			tabToSource.remove(title);
		}
		super.remove(index);
	}

	/**
	 * Returns true if the tabbed pane has a tab of the given title open. False
	 * otherwise.
	 * 
	 * @param title
	 *            Title of a tab.
	 * @return True if tab exists, false otherwise.
	 */
	public boolean hasTab(String title) {
		return tabToSource.containsKey(title);
	}

	/**
	 * Removes the tab by the given title.
	 * 
	 * @param title
	 *            Title of the tab to remove.
	 * @return True if removal successful, false otherwise.
	 */
	public boolean removeTab(String title) {
		int index = getTitleIndex(title);
		if (index == -1) {
			return false;
		}
		remove(index);
		return true;
	}

	/**
	 * Selects the tab by the given title.
	 * 
	 * @param title
	 *            Title of the tab to select.
	 * @return True if selection successful, false otherwise.
	 */
	public boolean selectTab(String title) {
		int index = getTitleIndex(title);
		if (index == -1) {
			return false;
		}
		setSelectedIndex(index);
		return true;
	}

	/**
	 * Finds the index of the tab by the given title.
	 * 
	 * @param title
	 *            Title of the tab to search for.
	 * @return Index of the tab.
	 */
	private int getTitleIndex(String title) {
		int index = -1;
		for (int i = 0; i < getTabCount(); i++) {
			if (getTitleAt(i).equals(title)) {
				index = i;
				break;
			}
		}
		return index;
	}
}
