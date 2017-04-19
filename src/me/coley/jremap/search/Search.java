package me.coley.jremap.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.bmf.ClassNode;
import me.coley.bmf.JarReader;
import me.coley.bmf.consts.ConstString;
import me.coley.bmf.consts.ConstUTF8;
import me.coley.bmf.consts.Constant;
import me.coley.bmf.consts.ConstantType;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.util.StreamUtil;
import me.coley.jremap.Program;
import me.coley.jremap.gui.component.tree.MappingTreeNode;
import me.coley.jremap.gui.component.tree.SearchResultTreeNode;

public class Search {
	public static final int UTF_ALL = 100;
	public static final int UTF_STRINGS = 101;
	public static final int UTF_NOTSTRINGS = 102;
	private final Program callback;

	public Search(Program callback) {
		this.callback = callback;
	}

	@SuppressWarnings("rawtypes")
	public DefaultMutableTreeNode searchUTF8(int mode, String text) {
		System.out.println("Search: " + text);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(text);
		JarReader jar = callback.getJarReader();
		// Sort at this stage rather than sorting the root later.
		// Its not worth the trouble later on.
		for (String name : StreamUtil.listOfSortedJavaNames(jar.getClassEntries().keySet())) {
			ClassNode cn = jar.getClassEntries().get(name);
			ClassMapping cm = jar.getMapping().getMapping(name);
			MappingTreeNode mtn = new MappingTreeNode(cm.name.getValue(), cm);
			Stream<Constant> strUTF = cn.constants.stream().filter((c) -> (c != null && c.type == ConstantType.UTF8)).filter((c) -> ((ConstUTF8) c).getValue().contains(text));
			if (mode == UTF_ALL) {
				strUTF.forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
			} else {
				List<Constant> stringConstants = new ArrayList<>();
				cn.constants.stream().filter((c) -> (c != null && c.type == ConstantType.STRING)).forEach((c) -> stringConstants.add(cn.getConst(((ConstString) c).getValue())));
				if (mode == UTF_STRINGS) {
					strUTF.filter((c) -> stringConstants.contains(c)).forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
				} else if (mode == UTF_NOTSTRINGS) {
					strUTF.filter((c) -> !stringConstants.contains(c)).forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
				}
			}
			if (!mtn.isLeaf()) {
				root.add(mtn);
			}
		}
		return root;
	}

}
