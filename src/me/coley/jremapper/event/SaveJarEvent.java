package me.coley.jremapper.event;

import java.io.File;
import me.coley.event.Event;

/**
 * Event for saving the current modifications to a jar.
 * 
 * @author Matt
 */
public class SaveJarEvent extends Event {

	private final File destination;

	public SaveJarEvent(File destination) {
		this.destination = destination;
	}

	public File getDestination() {
		return destination;
	}
}