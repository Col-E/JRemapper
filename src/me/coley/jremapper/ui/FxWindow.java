package me.coley.jremapper.ui;

import java.io.File;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.event.LoadMapEvent;
import me.coley.jremapper.event.NewInputEvent;
import me.coley.jremapper.event.SaveJarEvent;
import me.coley.jremapper.event.SaveMapEvent;

public class FxWindow extends Application {
	@Override
	public void start(Stage stage) throws Exception {
		SplitPane split = new SplitPane(new FilePane(), new ContentPane());
		split.setDividerPositions(0.2);
		Scene scene = new Scene(split, 900, 600);
		scene.getStylesheets().add("resources/style/style.css");
		setupBinds(stage, scene);
		stage.setScene(scene);
		stage.setTitle("JRemapper");
		stage.show();
	}

	private void setupBinds(Stage stage, Scene scene) {
		KeyCombination bindNew = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
		KeyCombination bindLoadMap = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
		KeyCombination bindSaveMap = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
		KeyCombination bindSaveJar = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);
		KeyCombination bindUndo = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
		KeyCombination bindRedo = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
		scene.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			private File file = new File(System.getProperty("user.dir"));

			@Override
			public void handle(KeyEvent event) {
				if (bindNew.match(event)) {
					FileChooser fc = new FileChooser();
					fc.setTitle("Open");
					fc.setInitialDirectory(file);
					fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Java program", "*.jar"));
					File selected = fc.showOpenDialog(stage);
					if (selected != null) {
						NewInputEvent.call(selected);
					}
				} else if (bindSaveMap.match(event)) {
					FileChooser fc = new FileChooser();
					fc.setTitle("Save mappings");
					fc.setInitialDirectory(file);
					fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Json mappings", "*.json"));
					File selected = fc.showSaveDialog(stage);
					if (selected != null) {
						Bus.post(new SaveMapEvent(selected));
					}
				}  else if (bindLoadMap.match(event)) {
					FileChooser fc = new FileChooser();
					fc.setTitle("Load mappings");
					fc.setInitialDirectory(file);
					fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Json mappings", "*.json"));
					File selected = fc.showOpenDialog(stage);
					if (selected != null) {
						Bus.post(new LoadMapEvent(selected));
					}
				} else if (bindSaveJar.match(event)) {
					FileChooser fc = new FileChooser();
					fc.setTitle("Save jar");
					fc.setInitialDirectory(file);
					fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Java program", "*.jar"));
					File selected = fc.showSaveDialog(stage);
					if (selected != null) {
						Bus.post(new SaveJarEvent(selected));
					}
				} else if (bindUndo.match(event)) {
					Input.get().history.undo();
				} else if (bindRedo.match(event)) {
					Input.get().history.redo();
				}
			}
		});
	}

	public static void init(String[] args) {
		launch(args);
	}
}
