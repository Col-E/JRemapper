package me.coley.jremapper.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.objectweb.asm.Type;

import me.coley.jremapper.mapping.MMap;

public class MDec extends AbstractDec<MMap> {
	private final Map<String, VDec> variables = new HashMap<>();
	private final CDec owner;
	private final String name;
	private final String desc;
	private final boolean isMethod;

	private MDec(CDec owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		isMethod = desc.contains("(");
	}

	public static MDec fromMember(CDec owner, String name, String desc) {
		return new MDec(owner, name, desc);
	}

	/**
	 * @return Member declaration denotes a method.
	 */
	public boolean isMethod() {
		return isMethod;
	}

	/**
	 * @return Member declaration denotes a field.
	 */
	public boolean isField() {
		return !isMethod;
	}

	/**
	 * @return Member name, does not account for mappings.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Member descriptor, does not account for mappings.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Internal name of the type.
	 */
	public String getInternalType() {
		if (isField()) {
			return Type.getType(desc).getClassName().replace(".", "/");
		} else {
			return Type.getType(desc).getReturnType().getClassName().replace(".", "/");
		}
	}

	/**
	 * @return Class that holds this member declaration.
	 */
	public CDec getOwner() {
		return owner;
	}

	/**
	 * @return Mapping of this member. Will be {@code null} if this member has been
	 *         renamed.
	 */
	@Override
	protected MMap lookup() {
		return owner.map().lookup(name, desc);
	}

	/**
	 * @return Mapping of this member. Will discover the member even if it has been
	 *         renamed.
	 */
	@Override
	protected MMap lookupReverse() {
		return owner.map().lookupReverse(name, desc);
	}

	@Override
	protected void throwMappingFailure() {
		throw new RuntimeException("No mappings for the member: '" + name + " " + desc + "'");
	}

	@Override
	public String toString() {
		try {
			String nameType = isMethod ? name + desc : desc + " " + name;
			return nameType + (isRenamed() ? "(" + map() + ")" : "");
		} catch(NullPointerException npe)
		{
			npe.printStackTrace();
			return "FUCk";
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, desc, map());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MDec) {
			return hashCode() == other.hashCode();
		}
		return false;
	}

	/**
	 * Add variable to the member.
	 * 
	 * @param variable
	 *            Variable declaration.
	 */
	public void addVariable(VDec variable) {
		variables.put(variableKey(variable), variable);
	}

	/**
	 * @param name
	 *            Name of the variable.
	 * @return First variable matching the given name.
	 */
	public Optional<VDec> getVariableByName(String name) {
		return variables.values().stream().filter(v -> v.getName().equals(name)).findFirst();
	}

	/**
	 * @param member
	 *            Variable to generate key from.
	 * @return Key to store variable with.
	 */
	private static String variableKey(VDec member) {
		return member.getName() + "#" + member.getDesc();
	}
}
