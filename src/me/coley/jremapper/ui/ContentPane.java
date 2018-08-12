package me.coley.jremapper.ui;

import javafx.scene.layout.BorderPane;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.jremapper.History;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.event.ClassOpenEvent;
import me.coley.jremapper.event.OpenCodeEvent;
import me.coley.jremapper.event.NewInputEvent;

public class ContentPane extends BorderPane {
	private Input input;

	public ContentPane() {
		Bus.subscribe(this);
	}

	@Listener
	public void onClassOpen(ClassOpenEvent event) {
		try {
			content(History.push(event.getPath()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Listener
	public void onHistoryPrevious(OpenCodeEvent event) {
		try {
			content(event.getPane());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	private void content(CodePane pane) {
		setCenter(pane);
		pane.onShow();
	}

	@Listener
	public void onInput(NewInputEvent event) {
		input = event.get();
		History.reset(input);
	}

}
