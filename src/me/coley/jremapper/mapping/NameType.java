package me.coley.jremapper.mapping;

import java.util.Objects;

/**
 * Name + Descriptor wrapper.
 * 
 * @author Matt
 */
public class NameType {
	private final String desc;
	private String name;
	private String def;

	NameType(String name, String desc) {
		this.name = name;
		this.desc = desc;
		this.def = name + desc;
	}

	public NameType copy() {
		return new NameType(name, desc);
	}

	public void setName(String newName) {
		this.name = newName;
		this.def = name + desc;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof NameType && other.toString().equals(toString())) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(def);
	}

	@Override
	public String toString() {
		return def;
	}

	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
}