package me.coley.search;

import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;

import io.github.bmf.ClassNode;
import io.github.bmf.JarReader;
import io.github.bmf.consts.ConstUTF8;
import io.github.bmf.consts.Constant;
import io.github.bmf.consts.ConstantType;
import io.github.bmf.mapping.ClassMapping;
import me.coley.Program;
import me.coley.gui.component.tree.MappingTreeNode;
import me.coley.gui.component.tree.SearchResultTreeNode;

public class Search {
	public static final int UTF_ALL = 100;
	public static final int UTF_STRINGS = 101;
	public static final int UTF_NOTSTRINGS = 102;
	private final Program callback;

	public Search(Program callback) {
		this.callback = callback;
	}

	public DefaultMutableTreeNode searchUTF8(int mode, String text) {
		System.out.println("Search: " + text);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(text);
		JarReader jar = callback.getJarReader();
		for (Entry<String, ClassNode> entry : jar.getClassEntries().entrySet()) {
			ClassNode cn = entry.getValue();
			ClassMapping cm = jar.getMapping().getMapping(entry.getKey());
			MappingTreeNode mtn = new MappingTreeNode(entry.getKey(), cm);
			Stream<Constant> strUTF = cn.constants.stream().filter((c) -> (c != null && c.type == ConstantType.UTF8));
			//Stream<Constant> strStr = cn.constants.stream().filter((c) -> (c.type == ConstantType.UTF8));
			switch (mode) {
			case UTF_ALL: 
				strUTF.filter((c) -> ((ConstUTF8)c).getValue().contains(text)).forEach(
						(c) -> mtn.add(new SearchResultTreeNode(mtn,  ((ConstUTF8)c).getValue()))
						);
				break;
			
			case UTF_STRINGS:
				break;
			case UTF_NOTSTRINGS:
				break;
			}
			if (!mtn.isLeaf()){
				root.add(mtn);
			}
		}
		return root;
	}

}
