package me.coley.jremapper.event;

import me.coley.event.Event;
import me.coley.jremapper.mapping.AbstractMapping;

/**
 * Event for when a mapping is updated.
 * 
 * @author Matt
 */
public class MappingChangeEvent extends Event {
	private final AbstractMapping mapping;
	private final String newName, currentName;

	public MappingChangeEvent(AbstractMapping mapping, String newName) {
		this.mapping = mapping;
		this.currentName = mapping.getCurrentName();
		this.newName = newName;
	}

	/**
	 * @return Mapping being updated.
	 */
	public AbstractMapping getMapping() {
		return mapping;
	}

	/**
	 * @return The name to be applied to the mapping.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 * @return The name being replaced.
	 */
	public String getOldName() {
		return currentName;
	}
}