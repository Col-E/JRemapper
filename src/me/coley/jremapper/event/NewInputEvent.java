package me.coley.jremapper.event;

import java.io.File;
import java.io.IOException;

import me.coley.event.Bus;
import me.coley.event.Event;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Threads;

/**
 * Event for when a new input is loaded.
 * 
 * @author Matt
 */
public class NewInputEvent extends Event {
	private final Input input;

	public NewInputEvent(Input input) {
		this.input = input;
	}

	public NewInputEvent(File file) throws IOException {
		this(new Input(file));
	}

	public Input get() {
		return input;
	}

	/**
	 * Multi-threaded invoke for event.
	 * 
	 * @param file
	 *            File to load.
	 */
	public static void call(File file) {
		Threads.runFx(() -> {
			try {
				Bus.post(new NewInputEvent(file));
			} catch (Exception e) {
				Logging.error(e);
			}
		});
	}
}