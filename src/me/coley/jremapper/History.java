package me.coley.jremapper;

import java.util.Stack;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.event.MappingChangeEvent;
import me.coley.jremapper.ui.CodePane;

/**
 * This is a really poorly made history manager. It works.
 * 
 * @author Matt
 */
public class History {
	private static History INSTANCE;
	private final Stack<CodePane> panes = new Stack<>();
	private final Stack<MappingChangeEvent> applied = new Stack<>();
	private final Stack<MappingChangeEvent> unapplied = new Stack<>();
	private static boolean ignore;
	private Input input;

	private History(Input input) {
		this.input = input;
	}

	@Listener
	public void onMappingUpdate(MappingChangeEvent remap) {
		if (!ignore) {
			unapplied.clear();
			applied.push(remap);
		}
	}

	public static void undo() {
		if (INSTANCE.applied.isEmpty()) {
			return;
		}
		MappingChangeEvent last = INSTANCE.applied.pop();
		ignore = true;
		last.getMapping().setCurrentName(last.getOldName());
		ignore = false;
		INSTANCE.unapplied.push(last);
		INSTANCE.panes.peek().refreshCode();
	}

	public static void redo() {
		if (INSTANCE.unapplied.isEmpty()) {
			return;
		}
		MappingChangeEvent last = INSTANCE.unapplied.pop();
		ignore = true;
		last.getMapping().setCurrentName(last.getNewName());
		ignore = false;
		INSTANCE.applied.push(last);
		INSTANCE.panes.peek().refreshCode();
	}

	public static CodePane push(String path) {
		return push(CodePane.open(INSTANCE.input, path));
	}

	public static CodePane push(CodePane pane) {
		INSTANCE.panes.push(pane);
		return pane;
	}

	public static CodePane pop() {
		if (INSTANCE.panes.isEmpty()) {
			return null;
		}
		return INSTANCE.panes.pop();
	}

	public static void reset(Input input) {
		if (INSTANCE != null) {
			INSTANCE.reset();

		}
		INSTANCE = new History(input);
		Bus.subscribe(INSTANCE);
	}

	private void reset() {
		Bus.unsubscribe(this);
		applied.clear();
		unapplied.clear();
	}
}