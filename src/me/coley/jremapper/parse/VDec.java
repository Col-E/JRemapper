package me.coley.jremapper.parse;

import java.util.Objects;
import java.util.Optional;

import me.coley.jremapper.mapping.*;
import org.objectweb.asm.Type;

public class VDec extends AbstractDec<VMap> {
	private final String name;
	private final String desc;
	private final MDec owner;

	private VDec(MDec owner, String name, String desc) {
		this.owner = owner;
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
	 * @return Host member.
	 */
	public MDec declaring() {
		return owner;
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
	 * @return Mapping of this variable. Will be {@code null} if this variable has been
	 *         renamed.
	 */
	@Override
	protected VMap lookup() {
		MMap method = declaring().lookup();
		Optional<VMap> opt = method.getVariables().stream()
				.filter(v -> name.equals(v.getOriginalName())).findFirst();
		return opt.orElse(null);
	}

	/**
	 * Used when the declaring method is renamed.
	 *
	 * @return Mapping of this variable.
	 */
	@Override
	protected VMap lookupReverse() {
		MMap method = declaring().lookupReverse();
		Optional<VMap> opt = method.getVariables().stream()
				.filter(v -> name.equals(v.getOriginalName())).findFirst();
		return opt.orElse(null);
	}

	@Override
	protected void throwMappingFailure() {
		throw new RuntimeException("No mappings for the variable: '" + getName() + "'");
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
