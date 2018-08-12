package me.coley.jremapper.event;

import me.coley.event.Event;

/**
 * Event for when a class is selected.
 * 
 * @author Matt
 */
public class ClassOpenEvent extends Event {
	private final String path;

	public ClassOpenEvent(String path) {
		this.path = path;
	}

	/**
	 * @return Node selected.
	 */
	public String getPath() {
		return path;
	}
}