package me.coley.jremapper.event;

import java.io.File;
import me.coley.event.Event;

/**
 * Event for saving the current mappings to a text file.
 * 
 * @author Matt
 */
public class SaveMapEvent extends Event {

	private final File destination;

	public SaveMapEvent(File destination) {
		this.destination = destination;
	}

	public File getDestination() {
		return destination;
	}
}