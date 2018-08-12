package me.coley.jremapper.event;

import me.coley.event.Event;
import me.coley.jremapper.ui.CodePane;

/**
 * Event for opening a code-pane.
 * 
 * @author Matt
 */
public class OpenCodeEvent extends Event {
	private final CodePane pane;

	public OpenCodeEvent(CodePane previous) {
		this.pane = previous;
	}

	/**
	 * @return Pane to open.
	 */
	public CodePane getPane() {
		return pane;
	}
}