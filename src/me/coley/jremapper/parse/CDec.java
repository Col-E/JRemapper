package me.coley.jremapper.parse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.coley.jremapper.mapping.CMap;
import me.coley.jremapper.mapping.Mappings;

/**
 * Class Declaration.
 * 
 * @author Matt
 */
public class CDec extends AbstractDec<CMap> {
	private final Map<String, MDec> members = new HashMap<>();
	private final String full;
	private final String pack;
	private final String simple;

	/**
	 * @param name
	 *            Internal name.
	 */
	private CDec(String name) {
		full = name;
		int packIndex = name.lastIndexOf("/");
		simple = name.substring(packIndex + 1);
		pack = packIndex == -1 ? "" : name.substring(0, packIndex);
	}

	/**
	 * @param name
	 *            Class name.
	 * @return Declaration of class.
	 */
	public static CDec fromClass(String name) {
		if (name.contains(".")) {
			name = name.replace(".", "/");
		}
		return new CDec(name);
	}

	/**
	 * @return Class package + name.
	 */
	public String getFullName() {
		return full;
	}

	/**
	 * @return Class package.
	 */
	public String getPackage() {
		return pack;
	}

	/**
	 * @return Class name <i>(no package)</i>.
	 */
	public String getSimpleName() {
		return simple;
	}

	/**
	 * @return {@code true} if the package is the empty string.
	 */
	public boolean isDefaultPackage() {
		return getPackage().equals("");
	}

	/**
	 * Add member to the class.
	 * 
	 * @param member
	 *            Member declaration.
	 */
	public void addMember(MDec member) {
		members.put(memberKey(member), member);
	}

	/**
	 * 
	 * @param name
	 *            Member original name.
	 * @param desc
	 *            Member original name.
	 * @return Member declaration.
	 */
	public MDec getMember(String name, String desc) {
		if (name == null || desc == null)
			return null;
		return members.get(memberKey(name, desc));
	}

	/**
	 * @param member
	 *            Member to generate key from.
	 * @return Key to store member with.
	 */
	private String memberKey(MDec member) {
		return memberKey(member.getName(), member.getDesc());
	}

	/**
	 * @param name
	 *            Member original name.
	 * @param desc
	 *            Member original descriptor.
	 * @return Key to store member with.
	 */
	private String memberKey(String name, String desc) {
		return name + "#" + desc;
	}

	/**
	 * @return List of members of the class.
	 */
	public Collection<MDec> getMembers() {
		return members.values();
	}

	/**
	 * @return Mapping of this class. Will be {@code null} if this class has been
	 *         renamed.
	 */
	@Override
	protected CMap lookup() {
		return Mappings.INSTANCE.getClassMapping(full);
	}

	/**
	 * @return Mapping of this class. Will discover the class even if it has been
	 *         renamed.
	 */
	@Override
	protected CMap lookupReverse() {
		return Mappings.INSTANCE.getClassReverseMapping(full);
	}

	@Override
	protected void throwMappingFailure() {
		throw new RuntimeException("No mappings for the class: '" + full + "'");
	}

	@Override
	public String toString() {
		return full + (isRenamed() ? "(" + map() + ")" : "");
	}

	@Override
	public int hashCode() {
		return Objects.hash(full, map());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CDec) {
			return hashCode() == other.hashCode();
		}
		return false;
	}
}