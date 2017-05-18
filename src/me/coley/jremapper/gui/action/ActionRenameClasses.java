package me.coley.jremapper.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import me.coley.bmf.mapping.ClassMapping;
import me.coley.bmf.mapping.MemberMapping;
import me.coley.bmf.type.PrimitiveType;
import me.coley.bmf.type.Type;
import me.coley.bmf.type.descriptors.MethodDescriptor;
import me.coley.bmf.type.descriptors.VariableDescriptor;
import me.coley.jremapper.Program;
import me.coley.jremapper.util.StringUtil;

public class ActionRenameClasses implements ActionListener {
	private final Program callback;
	private final Map<String, Integer> nameCounter = new HashMap<String, Integer>();
	private int classCounter, fieldCounter, methodCounter;

	public ActionRenameClasses(Program callback) {
		this.callback = callback;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		for (ClassMapping cm : callback.getJarReader().getMapping().getMappings().values()) {
			if (cm.name.original.equals(cm.name.getValue())) {
				String newName = genClassName(cm);
				cm.name.setValue(newName);
			}
		}
		for (ClassMapping cm : callback.getJarReader().getMapping().getMappings().values()) {
			for (MemberMapping mm : cm.getMembers()) {
				if (mm.name.original.contains("t>") || !mm.name.original.equals(mm.name.getValue())) {
					continue;
				}
				if (mm.desc.original.contains("(")) {
					mm.name.setValue(genMethodName(mm));
				} else {
					mm.name.setValue(genFieldName(mm));
				}
			}
		}
		callback.refreshTree();
	}

	public String genClassName(ClassMapping cm) {
		ClassMapping parent = callback.getJarReader().getMapping().getParent(cm);
		if (parent != null) {
			if (isValidClassName(parent.name.getValue())) {
				String pnv = parent.name.getValue();
				String parentSimple = pnv.substring(pnv.lastIndexOf('/') + 1);
				StringBuilder name = new StringBuilder("pkg/");
				if (!nameCounter.containsKey(parentSimple)) {
					nameCounter.put(parentSimple, 1);
				} else {
					nameCounter.put(parentSimple, nameCounter.get(parentSimple) + 1);
				}
				int i = nameCounter.get(parentSimple);
				String nameSimple = parentSimple + "_" + i;
				name.append(nameSimple);
				return name.toString();
			}
		}
		return "pkg/Class" + classCounter++;
	}

	private String genFieldName(MemberMapping mm) {
		if (mm.desc instanceof VariableDescriptor) {
			VariableDescriptor vd = (VariableDescriptor) mm.desc;
			if (vd.type instanceof PrimitiveType) {
				PrimitiveType pt = (PrimitiveType) vd.type;
				return pt.toJavaName() + "_fd" + fieldCounter++;
			} else {
				String s = vd.type.toDesc();
				if (s.contains("/")) {
					s = s.substring(s.lastIndexOf("/") + 1);
				}
				s = s.substring(0, s.length() - 1);
				if (s.length() > 1) {
					String s2 = s.substring(0, 1).toLowerCase() + s.substring(1);
					return s2 + "_fd" + fieldCounter++;
				}
			}
		}
		return "unknown" + "_fd" + fieldCounter++;
	}

	private String genMethodName(MemberMapping mm) {
		if (mm.desc instanceof MethodDescriptor) {
			MethodDescriptor md = (MethodDescriptor) mm.desc;
			if (md.returnType instanceof PrimitiveType) {
				PrimitiveType pt = (PrimitiveType) md.returnType;
				if (pt == Type.VOID) {
					return "void_mt" + methodCounter++;
				} else {
					String s = pt.toJavaName();
					s = s.substring(0, 1).toUpperCase() + s.substring(1);
					return "get" + s + "_mt" + methodCounter++;
				}
			} else {
				String s = md.returnType.toDesc();
				if (s.contains("/")) {
					s = s.substring(s.lastIndexOf("/") + 1);
				}
				s = s.substring(0, s.length() - 1);
				if (s.length() > 1) {
					String s2 = s.substring(0, 1).toUpperCase() + s.substring(1);
					return "get" + s2 + "_mt" + methodCounter++;
				}
			}
		}
		return "unknown" + "_mt" + methodCounter++;
	}

	private boolean isValidClassName(String value) {
		if (value.equals("java/lang/Object")) {
			return false;
		}
		String copy = value.replace("/", "");
		if (copy.length() < 5) {
			return false;
		}
		if (StringUtil.getFirstNonWordChar(copy) != null) {
			return false;
		}
		return true;
	}
}
