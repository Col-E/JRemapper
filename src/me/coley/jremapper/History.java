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
	private final Stack<CodePane> panes = new Stack<>();
	private final Stack<MappingChangeEvent> applied = new Stack<>();
	private final Stack<MappingChangeEvent> unapplied = new Stack<>();
	private final Input input;
	private boolean ignore;

	public History(Input input) {
		this.input = input;
		Bus.subscribe(this);
	}

	@Listener
	public void onMappingUpdate(MappingChangeEvent remap) {
		if (!ignore) {
			unapplied.clear();
			applied.push(remap);
		}
	}

	public void undo() {
		if (applied.isEmpty()) {
			return;
		}
		MappingChangeEvent last = applied.pop();
		ignore = true;
		last.getMapping().setCurrentName(last.getOldName());
		ignore = false;
		unapplied.push(last);
		panes.peek().refreshCode();
	}

	public void redo() {
		if (unapplied.isEmpty()) {
			return;
		}
		MappingChangeEvent last = unapplied.pop();
		ignore = true;
		last.getMapping().setCurrentName(last.getNewName());
		ignore = false;
		applied.push(last);
		panes.peek().refreshCode();
	}

	public CodePane push(String path) {
		return push(CodePane.open(input, path));
	}

	public CodePane push(CodePane pane) {
		panes.push(pane);
		return pane;
	}

	public CodePane pop() {
		if (panes.isEmpty()) {
			return null;
		}
		return panes.pop();
	}

	public void reset() {
		Bus.unsubscribe(this);
		applied.clear();
		unapplied.clear();
	}
}