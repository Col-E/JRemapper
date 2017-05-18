package me.coley.jremapper.gui.component;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import me.coley.jremapper.Program;
import me.coley.jremapper.gui.component.tree.SearchRenderer;
import me.coley.jremapper.gui.listener.SearchResultTreeListener;
import me.coley.jremapper.search.Search;

@SuppressWarnings("serial")
public class SearchPanel extends JPanel {
	private static final String SearchUTF8 = "UTF8";
	private static final String SearchClasses = "Classes";
	private static final String SearchMembers = "Members";
	private static final String SearchMemberMethod = "Methods";
	private static final String SearchMemberField = "Fields";
	private final Program callback;
	private final JTree tree = new JTree(new String[] {});
	private CardLayout layout;
	private boolean searchMethodMembers;

	public SearchPanel(Program callback) {
		this.callback = callback;
		setLayout(new BorderLayout());
		setupSearchOptions();
		setupSearchResults();
	}

	private void setupSearchOptions() {
		JPanel pnlTitleAndContentWrapper = new JPanel(new BorderLayout());
		JPanel pnlSearchOptionWrapper = new JPanel(new BorderLayout());
		JLabel title = new HTMLLabel("Search ConstPools:");
		title.setToolTipText("Search works by checking the constant pool of all loaded classes.<br>"
				+ "In order to better understand the results, read up on the class file specs.");
		title.setBorder(new EmptyBorder(5, 5, 5, 0));

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
		pnlSearchOptionWrapper.add(combo, BorderLayout.NORTH);
		pnlSearchOptionWrapper.add(cardController, BorderLayout.CENTER);
		pnlTitleAndContentWrapper.add(pnlSearchOptionWrapper, BorderLayout.CENTER);
		pnlTitleAndContentWrapper.add(title, BorderLayout.NORTH);
		add(pnlTitleAndContentWrapper, BorderLayout.NORTH);
	}

	private JPanel createUTFSearchPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JTextField searchAll = new JTextField();
		JTextField searchStrings = new JTextField();
		JTextField searchNonStrings = new JTextField();
		p.add(new JLabel("All:"));
		p.add(searchAll);
		p.add(new JLabel("Strings:"));
		p.add(searchStrings);
		p.add(new JLabel("Non-strings:"));
		p.add(searchNonStrings);
		//
		Function<String, DefaultMutableTreeNode> funcSAll = s -> callback.getSearcher().searchUTF8(Search.UTF_ALL, s);
		searchAll.addKeyListener(new SearchAdapter(searchAll, funcSAll));
		//
		Function<String, DefaultMutableTreeNode> funcSStrings = s -> callback.getSearcher()
				.searchUTF8(Search.UTF_STRINGS, s);
		searchStrings.addKeyListener(new SearchAdapter(searchStrings, funcSStrings));
		//
		Function<String, DefaultMutableTreeNode> funcSNonStrings = s -> callback.getSearcher()
				.searchUTF8(Search.UTF_NOTSTRINGS, s);
		searchNonStrings.addKeyListener(new SearchAdapter(searchNonStrings, funcSNonStrings));
		return p;
	}

	private JPanel createClassSearchPanel() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JTextField searchContains = new JTextField();
		JTextField searchReferencesMethods = new JTextField();
		JTextField searchReferencesFields = new JTextField();
		p.add(new JLabel("Name contains:"));
		p.add(searchContains);
		p.add(new JLabel("Method references to:"));
		p.add(searchReferencesMethods);
		p.add(new JLabel("Field references to:"));
		p.add(searchReferencesFields);
		//
		Function<String, DefaultMutableTreeNode> funcSNameContains = s -> callback.getSearcher()
				.searchClass(Search.CLASS_NAME_CONTAINS, s);
		searchContains.addKeyListener(new SearchAdapter(searchContains, funcSNameContains));
		//
		Function<String, DefaultMutableTreeNode> funcSMethodRefs = s -> callback.getSearcher()
				.searchClass(Search.CLASS_REFERENCE_METHODS, s);
		searchReferencesMethods.addKeyListener(new SearchAdapter(searchReferencesMethods, funcSMethodRefs));
		//
		Function<String, DefaultMutableTreeNode> funcSFieldRefs = s -> callback.getSearcher()
				.searchClass(Search.CLASS_REFERENCE_FIELDS, s);
		searchReferencesFields.addKeyListener(new SearchAdapter(searchReferencesFields, funcSFieldRefs));
		//
		return p;
	}

	private JPanel createMemberSearchPanel() {
		// TODO: figure out why labels aren't left-aligned like the other
		// panels.
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 5));
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		JComboBox<String> combo = new JComboBox<>();
		JTextField searchNameContains = new JTextField();
		JTextField searchDescContains = new JTextField();
		p.add(combo);
		p.add(new JLabel("Name:"));
		p.add(searchNameContains);
		JLabel lblDesc = new HTMLLabel("Descriptor:");
		lblDesc.setToolTipText(
				"If the entered text is that of a primitive, only primitive descriptors will be searched.<br>"
						+ "If you search for fields with the descriptor of <i>'I'</i> a class with <i>'I'</i> will not appear. Only primitives will show in the results.");
		p.add(lblDesc);
		p.add(searchDescContains);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(SearchMemberMethod);
		model.addElement(SearchMemberField);
		combo.setModel(model);
		combo.setEditable(false);
		combo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent evt) {
				searchMethodMembers = combo.getSelectedItem().toString().endsWith(SearchMemberMethod);
			}
		});
		searchMethodMembers = combo.getSelectedItem().toString().endsWith(SearchMemberMethod);
		//
		Function<String, DefaultMutableTreeNode> funcSMemberName = s -> callback.getSearcher()
				.searchMember(Search.MEMBER_DEFINITION_NAME, this.searchMethodMembers, s);
		searchNameContains.addKeyListener(new SearchAdapter(searchNameContains, funcSMemberName));
		//
		Function<String, DefaultMutableTreeNode> funcSMemberDesc = s -> callback.getSearcher()
				.searchMember(Search.MEMBER_DEFINITION_DESC, this.searchMethodMembers, s);
		searchDescContains.addKeyListener(new SearchAdapter(searchDescContains, funcSMemberDesc));
		return p;
	}

	private void setupSearchResults() {
		JPanel wrapper = new JPanel(new BorderLayout());
		JScrollPane scrollTree = new JScrollPane(tree);
		wrapper.add(scrollTree, BorderLayout.CENTER);
		tree.setCellRenderer(new SearchRenderer(callback));
		SearchResultTreeListener sel = new SearchResultTreeListener(callback);
		tree.addTreeSelectionListener(sel);
		tree.addMouseListener(sel);
		add(wrapper, BorderLayout.CENTER);
	}

	public void setResults(DefaultMutableTreeNode root) {
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
	}

	class SearchAdapter extends KeyAdapter {
		private final JTextField txt;
		private final Function<String, DefaultMutableTreeNode> dest;

		public SearchAdapter(JTextField txt, Function<String, DefaultMutableTreeNode> dest) {
			this.txt = txt;
			this.dest = dest;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				e.consume();
				DefaultMutableTreeNode root = dest.apply(txt.getText());
				setResults(root);
			}
		}
	}
}
