package me.coley.jremapper.parse;

import me.coley.jremapper.asm.Input;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Class visitor to build {@link CDec} instances.
 */
public class DecBuilder extends ClassVisitor {
	private final CDec dec;
	private final Input input;

	public DecBuilder(CDec dec, Input input) {
		super(Opcodes.ASM7);
		this.dec = dec;
		this.input = input;
	}

	@Override
	public FieldVisitor visitField(int acc, String name, String desc, String s, Object v) {
		return new DecFieldBuilder(name, desc);
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String s, String[] e) {
		return new DecMethodBuilder(acc, name, desc);
	}

	/**
	 * Field visitor to build {@link MDec} instances for fields.
	 */
	private class DecFieldBuilder extends FieldVisitor {
		private final MDec mdec;

		public DecFieldBuilder(String name, String desc) {
			super(Opcodes.ASM7);
			mdec = MDec.fromMember(dec, name, desc);
			if (dec.isLocked())
				mdec.lock();
		}

		@Override
		public void visitEnd() {
			dec.addMember(mdec);
		}
	}

	/**
	 * Method visitor tobuild {@link MDec} instances for methods and check for local existence.
	 */
	private class DecMethodBuilder extends MethodNode {
		private final MDec mdec;

		public DecMethodBuilder(int acc, String name, String desc) {
			super(Opcodes.ASM7);
			mdec = MDec.fromMember(dec, name, desc);
			if (dec.isLocked())
				mdec.lock();
		}

		@Override
		public void visitLocalVariable(String name, String desc, String s, Label b, Label e, int i) {
			super.visitLocalVariable(name, desc, s, b, e, i);
			mdec.addVariable(VDec.fromVariable(mdec, name, desc));
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var);
		}

		@Override
		public void visitEnd() {
			dec.addMember(mdec);
		}
	}
}
