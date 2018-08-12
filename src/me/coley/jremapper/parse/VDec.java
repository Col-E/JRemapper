package me.coley.jremapper.parse;

import java.util.Objects;

import org.objectweb.asm.Type;

import me.coley.jremapper.mapping.CMap;
import me.coley.jremapper.mapping.Mappings;

public class VDec extends AbstractDec<CMap> {
	private final String name;
	private final String desc;

	private VDec(MDec owner, String name, String desc) {
		this.name = name;
		this.desc = desc;
		if (name == null) {
			throw new RuntimeException("Name cannot be null!");
		}
		if (desc == null) {
			throw new RuntimeException("Desc cannot be null!");
		}
	}

	public static VDec fromVariable(MDec owner, String name, String desc) {
		return new VDec(owner, name, desc);
	}

	/**
	 * @return Variable name, does not account for mappings.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Variable descriptor, does not account for mappings.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Internal name of the type.
	 */
	public String getInternalType() {
		return Type.getType(desc).getClassName().replace(".", "/");
	}

	/**
	 * @return Mapping of this class. Will be {@code null} if this class has been
	 *         renamed.
	 */
	@Override
	protected CMap lookup() {
		return Mappings.INSTANCE.getClassMapping(getInternalType());
	}

	/**
	 * @return Mapping of this class. Will discover the class even if it has been
	 *         renamed.
	 */
	@Override
	protected CMap lookupReverse() {
		return Mappings.INSTANCE.getClassReverseMapping(getInternalType());
	}

	@Override
	protected void throwMappingFailure() {
		throw new RuntimeException("No mappings for the class: '" + getInternalType() + "'");
	}

	@Override
	public String toString() {
		return name + (isRenamed() ? "(" + map() + ")" : "");
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, desc, map());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VDec) {
			return hashCode() == other.hashCode();
		}
		return false;
	}

}
