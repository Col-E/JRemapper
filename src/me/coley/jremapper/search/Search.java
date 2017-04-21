package me.coley.jremapper.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.bmf.ClassNode;
import me.coley.bmf.JarReader;
import me.coley.bmf.consts.*;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.util.ConstUtil;
import me.coley.bmf.util.StreamUtil;
import me.coley.jremapper.Program;
import me.coley.jremapper.gui.component.tree.MappingTreeNode;
import me.coley.jremapper.gui.component.tree.SearchResultTreeNode;

public class Search {
	public static final int UTF_ALL = 100;
	public static final int UTF_STRINGS = 101;
	public static final int UTF_NOTSTRINGS = 102;
	public static final int CLASS_NAME_CONTAINS = 200;
	public static final int CLASS_REF_FIELDS = 201;
	public static final int CLASS_REF_METHODS = 202;
	private final Program callback;

	public Search(Program callback) {
		this.callback = callback;
	}

	@SuppressWarnings("rawtypes")
	public DefaultMutableTreeNode searchUTF8(int mode, String text) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(text);
		JarReader jar = callback.getJarReader();
		// Sort at this stage rather than sorting the root later.
		// Its not worth the trouble later on.
		for (String name : StreamUtil.listOfSortedJavaNames(jar.getClassEntries().keySet())) {
			ClassNode cn = jar.getClassEntries().get(name);
			ClassMapping cm = jar.getMapping().getMapping(name);
			MappingTreeNode mtn = new MappingTreeNode(cm.name.getValue(), cm);
			// Create stream of UTF8's containing the search text
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

	@SuppressWarnings("rawtypes")
	public DefaultMutableTreeNode searchClass(int mode, String text) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(text);
		JarReader jar = callback.getJarReader();
		// Sort at this stage rather than sorting the root later.
		// Its not worth the trouble later on.
		for (String name : StreamUtil.listOfSortedJavaNames(jar.getClassEntries().keySet())) {
			ClassNode cn = jar.getClassEntries().get(name);
			ClassMapping cm = jar.getMapping().getMapping(name);
			MappingTreeNode mtn = new MappingTreeNode(cm.name.getValue(), cm);
			if (mode == CLASS_NAME_CONTAINS) {
				if (name.contains(text)) {
					mtn.add(new SearchResultTreeNode(mtn, name));
				}
			} else {
				// TODO: Move to member search?
				// Doesn't seem super relevant to class search as-is.
				boolean meth = mode == CLASS_REF_METHODS;
				for (int i = 0; i < cn.constants.size(); i++) {
					Constant c = cn.constants.get(i);
					if (c == null || c.type != (meth ? ConstantType.METHOD : ConstantType.FIELD))
						continue;
					if (c instanceof AbstractMemberConstant) {
						AbstractMemberConstant amc = (AbstractMemberConstant) c;
						ConstNameType cnt = (ConstNameType) cn.getConst(amc.getNameTypeIndex());
						String memberName = ConstUtil.getUTF8(cn, cnt.getNameIndex());
						String memberDesc = ConstUtil.getUTF8(cn, cnt.getDescIndex());
						if (memberDesc.contains(text)) {
							String combined = memberName + (meth ? "" : " ") + memberDesc;
							mtn.add(new SearchResultTreeNode(mtn, combined));
						}
					}
				}
			}

			if (!mtn.isLeaf()) {
				root.add(mtn);
			}
		}
		return root;
	}

}
