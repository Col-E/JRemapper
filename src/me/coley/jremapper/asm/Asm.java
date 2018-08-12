package me.coley.jremapper.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Objectweb ASM utilities.
 * 
 * @author Matt
 */
public class Asm {
	public static final int INPUT_FLAGS = ClassReader.EXPAND_FRAMES;
	public static final int OUTPUT_FLAGS = ClassWriter.COMPUTE_FRAMES;

	/**
	 * Create a writer capable of generating proper frames.
	 * 
	 * @param input
	 *            Jar input.
	 * @return ClassWriter that can support computation of frames.
	 */
	public static ClassWriter createWriter(Input input) {
		return new ClassWriter(OUTPUT_FLAGS);
	}
}
