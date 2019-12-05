package me.coley.jremapper.parse;

import me.coley.jremapper.asm.Input;
import org.objectweb.asm.*;

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
		return new DecFieldVisitor(name, desc);
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String s, String[] e) {
		return new DecMethodVisitor(name, desc);
	}

	/**
	 * Field visitor.
	 */
	private class DecFieldVisitor extends FieldVisitor {
		private final MDec mdec;

		public DecFieldVisitor(String name, String desc) {
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
	 * Method visitor.
	 */
	private class DecMethodVisitor extends MethodVisitor {
		private final MDec mdec;
		private boolean visitedLocals;
		private boolean hasLocals;

		public DecMethodVisitor(String name, String desc) {
			super(Opcodes.ASM7);
			mdec = MDec.fromMember(dec, name, desc);
			if (dec.isLocked())
				mdec.lock();
		}

		@Override
		public void visitLocalVariable(String name, String desc, String s, Label b, Label e, int i) {
			mdec.addVariable(VDec.fromVariable(mdec, name, desc));
			visitedLocals = true;
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			hasLocals = true;
		}

		@Override
		public void visitEnd() {
			dec.addMember(mdec);
			// Check if local debug info was not visited (doesn't exist) but there are locals
			if (!visitedLocals && hasLocals) {
				// TODO: Try to determine variable types
			}
		}
	}
}
