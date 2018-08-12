package me.coley.jremapper.event;

import java.io.File;
import me.coley.event.Event;

/**
 * Event for loading mappings from a text file.
 * 
 * @author Matt
 */
public class LoadMapEvent extends Event {
	private final File destination;

	public LoadMapEvent(File destination) {
		this.destination = destination;
	}

	public File getDestination() {
		return destination;
	}
}