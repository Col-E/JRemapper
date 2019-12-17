package me.coley.jremapper.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Class visitor to re-supply methods with missing debug variable names.
 */
public class VariableFixer extends ClassNode {
	private boolean dirty;

	public VariableFixer() {
		super(Opcodes.ASM7);
	}

	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		for (MethodNode mn : methods) {
			if (mn.localVariables == null || mn.localVariables.isEmpty()) {
				MethodVisitor mv = new VariableSupplier(mn.access, mn.desc, mn);
				mn.accept(mv);
			}
		}
	}

	private ClassNode node() {
		return this;
	}

	/**
	 * Method visitor to supply missing local variable information.
	 */
	public class VariableSupplier extends MethodVisitor {
		private final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
		private final MethodNode mv;
		private final boolean isStatic;
		private final Type desc;
		private final Map<Integer, Type> varTypes = new TreeMap<>();
		private final Map<Integer, LabelNode> varLabels = new TreeMap<>();
		private LabelNode label;
		private boolean hasNullLabelLookup;

		public VariableSupplier(int access, String desc, MethodNode mv) {
			super(Opcodes.ASM7, null);
			this.desc = Type.getMethodType(desc);
			this.mv = mv;
			isStatic = Access.isStatic(access);
		}

		@Override
		public void visitLabel(Label label) {
			// Fetch inserted labelnode
			this.label = (LabelNode) label.info;
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			// "this"
			if (var == 0 && !isStatic) {
				setLocalType(var, Type.getObjectType(node().name));
				return;
			}
			// Other
			Type varType = null;
			switch(opcode) {
				case Opcodes.LLOAD:
				case Opcodes.LSTORE:
					varType = Type.LONG_TYPE;
					break;
				case Opcodes.DLOAD:
				case Opcodes.DSTORE:
					varType = Type.DOUBLE_TYPE;
					break;
				case Opcodes.FLOAD:
				case Opcodes.FSTORE:
					varType = Type.FLOAT_TYPE;
					break;
				case Opcodes.ILOAD:
				case Opcodes.ISTORE:
					varType = Type.INT_TYPE;
					break;
				case Opcodes.ALOAD:
				case Opcodes.ASTORE:
				case Opcodes.RET:
				default:
					varType = OBJECT_TYPE;
					/*
					int offset = var  + (isStatic ? 0 : -1);
					int size = desc.getArgumentsAndReturnSizes() - desc.getReturnType().getSize();
					// Check if local is given by the method parameters
					if (offset < size) {
						int k = 0;
						Type[] args = desc.getArgumentTypes();
						int i = 0;
						for (; i < args.length; i++) {
							if (k == offset)
								break;
							k += args[i].getSize();
						}
						if (i < args.length)
							varType = args[i];
					}
					break;
					*/
			}
			setLocalType(var, varType);
		}

		private void setLocalType(int local, Type type) {
			varTypes.put(local, type);
			if(label != null)
				// Record variable starting label
				varLabels.put(local, label);
			else
				// Mark that we'll need to add a dummy labels later
				hasNullLabelLookup = true;
		}


		@Override
		public void visitEnd() {
			// TODO: It would be cleaner to figure out variable scopes
			// Add dummy labels
			LabelNode dStart = new LabelNode(new Label());
			LabelNode dEnd = new LabelNode(new Label());
			if(hasNullLabelLookup) {
				mv.instructions.insert(dStart);
			}
			mv.instructions.add(dEnd);
			// Supply local variable infomation if needed
			if(mv.localVariables == null || mv.localVariables.isEmpty()) {
				dirty = true;
				mv.localVariables = new ArrayList<>();
				varTypes.forEach((index, type) -> {
					String name = "local_" + index;
					if(index == 0 && !isStatic) {
						name = "this";
					}
					String desc = type.getDescriptor();
					LabelNode start = varLabels.getOrDefault(index, dStart);
					mv.localVariables.add(new LocalVariableNode(name, desc, null, start, dEnd, index));
				});
			}
		}
	}
}
