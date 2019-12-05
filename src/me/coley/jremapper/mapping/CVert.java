package me.coley.jremapper.mapping;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

/**
 * Class Vertex. Edges denote parent/child relations.
 * 
 * @author Matt
 */
public class CVert {
	final Set<String> externalParents = new HashSet<>();
	final Set<CVert> parents = new HashSet<>();
	final Set<CVert> children = new HashSet<>();
	ClassNode data;

	CVert(ClassNode data) {
		this.data = data;
	}

	public String getName() {
		return data.name;
	}

	public String getSuperName() {
		return data.superName;
	}

	public CVert getSuper() {
		return parents.stream()
				.filter(v -> v.data.name.equals(getSuperName()))
				.findFirst().orElse(null);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(data.name);
	}

	@Override
	public String toString() {
		return data.name;
	}

}
