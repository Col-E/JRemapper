package me.coley.gui.component;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import me.coley.Program;
import me.coley.gui.component.tree.SearchRenderer;
import me.coley.search.Search;
import me.coley.util.SwingUtil;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	private static final String SearchUTF8 = "UTF8";
	private static final String SearchClasses = "Classes";
	private static final String SearchMembers = "Members";
	private final Program callback;
	private final JTree tree = new JTree(new String[] {});
	private CardLayout layout;

	public SearchPanel(Program callback) {
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
		JPanel cardMember = createMemberSearchPanel();
		cardController.add(cardStrings, SearchUTF8);
		cardController.add(cardClasses, SearchClasses);
		cardController.add(cardMember, SearchMembers);
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
		searchAll.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				e.consume();
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					DefaultMutableTreeNode root = callback.getSearcher().searchUTF8(Search.UTF_ALL, searchAll.getText());
					setResults(root);
				}
			}
		});
		searchStrings.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				e.consume();
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					DefaultMutableTreeNode root = callback.getSearcher().searchUTF8(Search.UTF_STRINGS, searchAll.getText());
					setResults(root);
				}
			}
		});
		searchNonStrings.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				e.consume();
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					DefaultMutableTreeNode root = callback.getSearcher().searchUTF8(Search.UTF_NOTSTRINGS, searchAll.getText());
					setResults(root);
				}
			}
		});
		return p;
	}

	private JPanel createClassSearchPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(new JLabel("TODO"));
		return p;
	}

	private JPanel createMemberSearchPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(new JLabel("TODO"));
		return p;
	}

	private void setupSearchResults() {
		JPanel wrapper = new JPanel(new BorderLayout());
		JScrollPane scrollTree = new JScrollPane(tree);
		wrapper.add(scrollTree, BorderLayout.CENTER);
		tree.setCellRenderer(new SearchRenderer(callback));
		add(wrapper, BorderLayout.CENTER);
	}

	public void setResults(DefaultMutableTreeNode root) {
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		// TODO: Check if the extra steps in FileTree's setRoot are needed
		model.setRoot(SwingUtil.sort(root));
	}
}
