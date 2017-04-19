package me.coley.jremapper.gui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * SplitPane capable of having N-many panels.
 */
@SuppressWarnings("serial")
public class NSplitPane extends JPanel {
	private final JPanel[] panels;
	private final JSplitPane[] splits;

	public NSplitPane(int n) {
		setLayout(new BorderLayout());
		panels = new JPanel[n];
		splits = new JSplitPane[n - 1];
		for (int i = 0; i < n; i++) {
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			panels[i] = p;
		}
		JSplitPane start = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JSplitPane sp = start;
		for (int i = 0; i < n - 1; i++) {
			splits[i] = sp;
			sp.setDividerSize(7);
			JSplitPane sp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			if (i == n - 2) {
				sp.setLeftComponent(panels[i]);
				sp.setRightComponent(panels[i + 1]);
			} else {
				sp.setLeftComponent(panels[i]);
				sp.setRightComponent(sp2);
			}
			sp = sp2;
		}
		add(start, BorderLayout.CENTER);
	}

	public void setNDivider(int paneIndex, int divLocation) {
		splits[paneIndex].setDividerLocation(divLocation);
	}

	public void addNComponent(int index, int width, Component component) {
		JPanel panel = panels[index];
		panel.setPreferredSize(new Dimension(width, 100));
		panel.add(component, BorderLayout.CENTER);
	}
}
