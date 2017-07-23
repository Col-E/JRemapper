package me.coley.jremapper.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;

import me.coley.bmf.ClassNode;
import me.coley.bmf.JarReader;
import me.coley.bmf.MemberNode;
import me.coley.bmf.MethodNode;
import me.coley.bmf.consts.*;
import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.bmf.opcode.AbstractFieldOpcode;
import me.coley.bmf.opcode.AbstractMethodOpcode;
import me.coley.bmf.opcode.Opcode;
import me.coley.bmf.type.Type;
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
	public static final int CLASS_REFERENCE_FIELDS = 201;
	public static final int CLASS_REFERENCE_METHODS = 202;
	public static final int MEMBER_DEFINITION_NAME = 300;
	public static final int MEMBER_DEFINITION_DESC = 301;
	private final Program callback;

	public Search(Program callback) {
		this.callback = callback;
	}

	/**
	 * Search for the given text in all loaded class's UTF8 constants.
	 * 
	 * @param mode
	 *            Text search mode.
	 * @param text
	 *            Text to search.
	 * @return
	 */
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
			Stream<Constant> strUTF = cn.constants.stream().filter((c) -> (c != null && c.type == ConstantType.UTF8))
					.filter((c) -> ((ConstUTF8) c).getValue().contains(text));
			if (mode == UTF_ALL) {
				// If the UTF contains the text, regardless of how it's uses add
				// it to the node.
				strUTF.forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
			} else {
				// Before adding a UTF containing the text to the node, first
				// it's usage must be considered.
				// To access the correct UTF's first we create a list of UTF's
				// used in strings.
				List<Constant> stringConstants = new ArrayList<>();
				cn.constants.stream().filter((c) -> (c != null && c.type == ConstantType.STRING))
						.forEach((c) -> stringConstants.add(cn.getConst(((ConstString) c).getValue())));
				if (mode == UTF_STRINGS) {
					strUTF.filter((c) -> stringConstants.contains(c))
							.forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
				} else if (mode == UTF_NOTSTRINGS) {
					strUTF.filter((c) -> !stringConstants.contains(c))
							.forEach((c) -> mtn.add(new SearchResultTreeNode(mtn, ((ConstUTF8) c).getValue())));
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
				boolean meth = mode == CLASS_REFERENCE_METHODS;
				for (int i = 0; i < cn.constants.size(); i++) {
					Constant c = cn.constants.get(i);
					if (c == null || c.type != (meth ? ConstantType.METHOD : ConstantType.FIELD)) continue;
					if (c instanceof AbstractMemberConstant) {
						AbstractMemberConstant amc = (AbstractMemberConstant) c;
						String memberOwner = ConstUtil.getClassName(cn, amc.getClassIndex());
						ConstNameType cnt = (ConstNameType) cn.getConst(amc.getNameTypeIndex());
						String memberName = ConstUtil.getUTF8(cn, cnt.getNameIndex());
						String memberDesc = ConstUtil.getUTF8(cn, cnt.getDescIndex());

						if (memberOwner.equals(text)) {
							String combined = memberOwner + " " + memberName + (meth ? "" : " ") + memberDesc;
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

	/**
	 * Searches for members that match the given text
	 * 
	 * @param mode
	 *            Search mode
	 * @param methods
	 *            If searching for methods <i>(As opposed to fields)</i>
	 * @param text
	 *            Text to search for.
	 * @return
	 */
	public DefaultMutableTreeNode searchMember(int mode, boolean methods, String text) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(text);
		JarReader jar = callback.getJarReader();
		// Sort at this stage rather than sorting the root later.
		// Its not worth the trouble later on.
		for (String name : StreamUtil.listOfSortedJavaNames(jar.getClassEntries().keySet())) {
			ClassNode cn = jar.getClassEntries().get(name);
			ClassMapping cm = jar.getMapping().getMapping(name);
			MappingTreeNode mtn = new MappingTreeNode(cm.name.getValue(), cm);
			Stream<MemberNode> members = methods ? cn.methods.stream().map(m -> ((MemberNode) m))
					: cn.fields.stream().map(f -> ((MemberNode) f));
			Predicate<MemberNode> memberPredicate = null;
			if (mode == MEMBER_DEFINITION_NAME) {
				memberPredicate = m -> ConstUtil.getUTF8(cn, m.name).contains(ConstUtil.getUTF8(cn, m.name));
			} else if (mode == MEMBER_DEFINITION_DESC) {
				// If searching for descriptors test for cases where the text
				// should be an EXACT search
				Predicate<String> descPredciate = s -> isPrim(text, methods) ? s.equals(text) : s.contains(text);
				memberPredicate = m -> descPredciate.test(ConstUtil.getUTF8(cn, m.desc));
			} else {
				continue;
			}
			String space = methods ? "" : " ";
			members.filter(memberPredicate).forEach((c) -> mtn.add(new SearchResultTreeNode(mtn,
					ConstUtil.getUTF8(cn, c.name) + space + ConstUtil.getUTF8(cn, c.desc))));
			if (!mtn.isLeaf()) {
				root.add(mtn);
			}
		}
		return root;
	}

	/**
	 * Searches for references to the given member.
	 * 
	 * @param memberOwner
	 * @param mm
	 * @return
	 */
	public DefaultMutableTreeNode searchMember(ClassMapping memberOwner, MemberMapping mm) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(mm.toString());
		JarReader jar = callback.getJarReader();
		boolean isMethod = mm.desc.original.startsWith("(");
		// Sort at this stage rather than sorting the root later.
		// Its not worth the trouble later on.
		for (String name : StreamUtil.listOfSortedJavaNames(jar.getClassEntries().keySet())) {
			ClassNode cn = jar.getClassEntries().get(name);
			ClassMapping cm = jar.getMapping().getMapping(name);
			MappingTreeNode mtn = new MappingTreeNode(cm.name.getValue(), cm);
			for (MethodNode mn : cn.methods) {
				if (mn.code != null && mn.code.opcodes != null) {
					for (Opcode op : mn.code.opcodes.opcodes) {
						AbstractMemberConstant amc = getMemberConstantFromOpcode(cn, op, isMethod);
						if (amc == null) {
							continue;
						}
						// Check for proper owner
						String className = ConstUtil.getClassName(cn, amc.getClassIndex());
						if (className.equals(memberOwner.name.getValue())) {
							ConstNameType cnt = (ConstNameType) cn.getConst(amc.getNameTypeIndex());
							if (matchesNameDesc(cn, cnt, mm)) {
								mtn.add(new SearchResultTreeNode(mtn,
										ConstUtil.getUTF8(cn, mn.name) + " " + ConstUtil.getUTF8(cn, mn.desc)));
							}
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

	private static AbstractMemberConstant getMemberConstantFromOpcode(ClassNode cn, Opcode op, boolean isMethod) {
		if (isMethod && op instanceof AbstractMethodOpcode) {
			AbstractMethodOpcode amo = (AbstractMethodOpcode) op;
			return (AbstractMemberConstant) cn.getConst(amo.methodIndex);
		} else if (!isMethod && op instanceof AbstractFieldOpcode) {
			AbstractFieldOpcode afo = (AbstractFieldOpcode) op;
			return (AbstractMemberConstant) cn.getConst(afo.fieldIndex);
		}
		return null;
	}

	private static boolean matchesNameDesc(ClassNode cn, ConstNameType c, MemberMapping mm) {
		return mm.name.getValue().equals(ConstUtil.getUTF8(cn, c.getNameIndex()))
				&& mm.desc.toDesc().equals(ConstUtil.getUTF8(cn, c.getDescIndex()));
	}

	private static boolean isPrim(String search, boolean methods) {
		int l = search.length();
		if (methods) {
			return l == 3 && Type.readPrim(search.charAt(2)) != null;
		} else {
			return l == 1 && Type.readPrim(search.charAt(0)) != null;
		}
	}
}
