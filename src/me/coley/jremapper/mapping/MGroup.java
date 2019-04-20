package me.coley.jremapper.mapping;

import java.util.HashSet;
import java.util.Set;

import me.coley.event.Bus;
import me.coley.jremapper.event.MappingChangeEvent;

/**
 * Method-Group. For some method defined by a NameType, hold the set of classes
 * that define that method.
 * 
 * @author Matt
 */
public class MGroup {
	public final NameType type;
	public final Set<CVert> definers = new HashSet<>();
	boolean locked;

	MGroup(NameType type) {
		this.type = type.copy();
	}

	public void setName(String newName) {
		if (locked) {
			throw new RuntimeException("Cannot rename a locked method-group!");
		}
		for (CVert c : definers) {
			CMap m = Mappings.INSTANCE.getClassMapping(c.data.name);
			if (m == null) {
				continue;
			}
			MMap mm = m.lookup(type.getName(), type.getDesc());
			if (mm == null) {
				continue;
			}
			Bus.post(new MappingChangeEvent(mm, newName));
			mm.setCurrentNameNoHierarchy(newName);
		}
	}
}