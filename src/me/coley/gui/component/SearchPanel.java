package me.coley.gui.component;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import me.coley.Program;
import me.coley.gui.MainWindow;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	private static final String SearchUTF8 = "UTF8";
	private static final String SearchClasses = "Classes";
	private static final String SearchMembers = "Members";
	private final MainWindow window;
	private final Program callback;
	private CardLayout layout;

	public SearchPanel(MainWindow window, Program callback) {
		this.window = window;
		this.callback = callback;
		setLayout(new BorderLayout());
		setupSearchOptions();
		setupSearchResults();
	}

	private void setupSearchOptions() {
		JPanel wrapper = new JPanel(new BorderLayout());
		final JPanel cardController = new JPanel(layout = new CardLayout());
		JComboBox<String> combo = new JComboBox<>();
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(SearchUTF8);
		model.addElement(SearchClasses);
		model.addElement(SearchMembers);
		combo.setModel(model);
		combo.setEditable(false);
		combo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent evt) {
				layout.show(cardController, (String) evt.getItem());
			}
		});
		JPanel cardStrings = createUTFSearchPanel();
		JPanel cardClasses = createClassSearchPanel();
		cardController.add(cardStrings, SearchUTF8);
		cardController.add(cardClasses, SearchClasses);
		wrapper.add(combo, BorderLayout.NORTH);
		wrapper.add(cardController, BorderLayout.CENTER);
		add(wrapper, BorderLayout.NORTH);
	}

	private JPanel createUTFSearchPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JTextField searchAll = new JTextField();
		JTextField searchStrings = new JTextField();
		JTextField searchNonStrings = new JTextField();
		p.add(new JLabel("Search All"));
		p.add(searchAll);
		p.add(new JLabel("Search Strings"));
		p.add(searchStrings);
		p.add(new JLabel("Search Non-Strings"));
		p.add(searchNonStrings);
		return p;
	}

	private JPanel createClassSearchPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		return p;
	}

	private void setupSearchResults() {

	}
}
