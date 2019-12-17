package me.coley.jremapper.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/**
 * Class remapper extension that supports renaming variable names.
 */
public class ClassRemapperExt extends ClassRemapper {
	private String className;
	private String methodName, methodDesc;

	public ClassRemapperExt(ClassWriter cw, RemapperImpl mapper, String className) {
		super(Opcodes.ASM7, cw, mapper);
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
									 String[] exceptions) {
		methodName = name;
		methodDesc = descriptor;
		String remappedDescriptor = remapper.mapMethodDesc(descriptor);
		MethodVisitor methodVisitor = super.visitMethod(access, remapper.mapMethodName(className,
				name, descriptor), remappedDescriptor, remapper.mapSignature(signature, false),
				exceptions == null ? null : remapper.mapTypes(exceptions));
		return methodVisitor == null ? null : createMethodRemapper(methodVisitor);
	}

	@Override
	protected MethodVisitor createMethodRemapper(final MethodVisitor methodVisitor) {
		return new MethodRemapperExt(methodVisitor, remapper);
	}

	private class MethodRemapperExt extends MethodRemapper {
		public MethodRemapperExt(MethodVisitor mv, Remapper remapper) {
			super(Opcodes.ASM7, mv, remapper);
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature,
									   Label start, Label end, int index) {
			super.visitLocalVariable(((RemapperImpl) remapper).mapVariableName(className,
					methodName, methodDesc, name), remapper.mapDesc(descriptor),
					remapper.mapSignature(signature, true), start, end, index);
		}
	}
}
