package me.coley.jremapper.ui;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.event.*;
import me.coley.jremapper.mapping.CMap;
import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.util.Icons;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Threads;

/**
 * Pane displaying file-tree of loaded classes.
 * 
 * @author Matt
 */
public class FilePane extends BorderPane {
	private final TreeView<String> tree = new TreeView<>();
	private Input input;

	public FilePane() {
		Bus.subscribe(this);
		setCenter(tree);
		// drag-drop support for inputs
		tree.setOnDragOver(e -> {
			if (e.getGestureSource() != tree && e.getDragboard().hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			e.consume();
		});

		tree.setOnMouseClicked(e -> {
			// Double click to open class
			if (e.getClickCount() == 2) {
				FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
				if (item != null && !item.isDir) {
					Bus.post(new ClassOpenEvent(item.fullPath));
				}
			}
		});
		tree.setOnDragDropped(e -> {
			Dragboard db = e.getDragboard();
			if (db.hasFiles()) {
				NewInputEvent.call(db.getFiles().get(0));
			}
		});
		// Custom tree renderering.
		tree.setShowRoot(false);
		tree.setCellFactory(param -> new TreeCell<String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					// Hide elements.
					// Items enter this state when 'hidden' in the tree.
					setText(null);
					setGraphic(null);
				} else {
					boolean cont = input.hasRawClass(item);
					Node fxImage = cont ? Icons.getClass(input.getClassAccess(item)) : new ImageView(Icons.CL_PACKAGE);
					setGraphic(fxImage);
					String name = item;
					if (cont) {
						// This is dumb, but I dont feel like editing the file tree classes.
						CMap map = Mappings.INSTANCE.getClassMapping(item);
						if (map != null) {
							name = map.getCurrentName();
						}
						name = trim(name);
						setText(name);
					}
					int max = 150;
					if (name.length() > max) {
						name = name.substring(0, max);
					}
					setText(name);
				}
			}
		});
		Bus.subscribe(this);
		Threads.runFx(tree::requestFocus);
	}

	/**
	 * Resets the tree to match content of input.
	 *
	 * @param input
	 * 		New content.
	 */
	@Listener
	public void onInputChange(NewInputEvent input) {
		this.input = input.get();
		tree.setRoot(getNodesForDirectory(this.input));
	}
	
	@Listener
	public void onLoadMapRequest(LoadMapEvent load) {
		Mappings.INSTANCE.loadMapping(load.getDestination());
	}

	@Listener
	public void onSaveMapRequest(SaveMapEvent save) {
		JsonValue value = Mappings.INSTANCE.toMapping();
		try {
			Path path = save.getDestination().toPath();
			byte[] content = value.toString(WriterConfig.PRETTY_PRINT).getBytes(StandardCharsets.UTF_8);
			Files.write(path, content, StandardOpenOption.CREATE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Listener
	public void onSaveJarRequest(SaveJarEvent save) {
		Map<String, byte[]> content = new HashMap<>();
		for (Entry<String, byte[]> e : input.rawNodeMap.entrySet()) {
			byte[] clazz = e.getValue();
			String key = Mappings.INSTANCE.getTransformedName(e.getKey()) + ".class";
			content.put(key, Mappings.INSTANCE.intercept(clazz));
		}
		for (Entry<String, byte[]> e : input.resourceMap.entrySet()) {
			content.put(e.getKey(), e.getValue());
		}
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(save.getDestination()))) {
			for (Map.Entry<String, byte[]> entry : content.entrySet()) {
				output.putNextEntry(new JarEntry(entry.getKey()));
				output.write(entry.getValue());
				output.closeEntry();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Resets the tree to match content of input.
	 *
	 * @param remap
	 * 		Mapping update that caused input content to change.
	 */
	@Listener
	public void onMappingUpdate(MappingChangeEvent remap) {
		if (remap.getMapping() instanceof CMap) {
			String original = remap.getMapping().getOriginalName();
			String current = remap.getMapping().getCurrentName();
			FileTreeItem item = getNode(current);
			FileTreeItem parent = (FileTreeItem) item.getParent();
			parent.remove(item);
			try {
				addToRoot((FileTreeItem) tree.getRoot(), remap.getNewName(), original);
			} catch (Exception e) {
				tree.setRoot(getNodesForDirectory(input));
			}
		}
	}

	/**
	 * @param name
	 *            Path of node, subdirs split with "/".
	 * @return Tree item by path/name.
	 */
	private FileTreeItem getNode(String name) {
		FileTreeItem r = (FileTreeItem) tree.getRoot();
		String[] parts = name.split("/");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (i == parts.length - 1) {
				// get final file
				r = r.getFile(part);
			} else {
				// get sub-dir
				r = r.getDir(part);
			}
		}
		return r;
	}

	/**
	 * Create root for input.
	 *
	 * @param input
	 * 		Content to build.
	 *
	 * @return {@code FileTreeItem}.
	 */
	private final FileTreeItem getNodesForDirectory(Input input) {
		FileTreeItem root = new FileTreeItem("root", null);
		input.names().forEach(name -> addToRoot(root, name, name));
		return root;
	}

	/**
	 * Add name to root assuming it is loaded in the current Input.
	 * 
	 * @param root
	 *            Root node.
	 * @param currentName
	 *            Current name of class in input.
	 * @param originalName
	 *            The original name of the node.
	 */
	private void addToRoot(FileTreeItem root, String currentName, String originalName) {
		FileTreeItem r = root;
		String[] parts = currentName.split("/");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (i == parts.length - 1) {
				// add final file
				r.addFile(part, originalName);
			} else if (r.hasDir(part)) {
				// navigate to sub-directory
				r = r.getDir(part);
			} else {
				// add sub-dir
				r = r.addDir(part);
			}
		}
	}

	/**
	 * Trim the text to a last section, if needed.
	 * 
	 * @param item
	 *            Internal class name.
	 * @return Simple name.
	 */
	private static String trim(String item) {
		return item.indexOf("/") > 0 ? item.substring(item.lastIndexOf("/") + 1) : item;
	}

	/**
	 * Wrapper for TreeItem children set. Allows more file-system-like access.
	 * 
	 * @author Matt
	 */
	public class FileTreeItem extends TreeItem<String> implements Comparable<String> {
		// Split in case of cases like:
		// a/a/a.class
		// a/a/a/a.class
		private final Map<String, FileTreeItem> dirs = new TreeMap<>();
		private final Map<String, FileTreeItem> files = new TreeMap<>();
		final boolean isDir;
		final String fullPath;

		private FileTreeItem(String part, String full) {
			isDir = full == null;
			fullPath = full;
			setValue(isDir ? part : full);
		}

		FileTreeItem addDir(String part) {
			FileTreeItem fti = new FileTreeItem(part, null);
			dirs.put(part, fti);
			addOrdered(fti);
			return fti;
		}

		void addFile(String part, String name) {
			FileTreeItem fti = new FileTreeItem(part, name);
			files.put(part, fti);
			addOrdered(fti);
		}

		private void addOrdered(FileTreeItem fti) {
			try {
				int sizeD = dirs.size();
				int sizeF = files.size();
				int size = sizeD + sizeF;
				if (size == 0) {
					getChildren().add(fti);
					return;
				}
				if (fti.isDir) {
					FileTreeItem[] array = dirs.values().toArray(new FileTreeItem[0]);
					int index = Arrays.binarySearch(array, fti.getValue());
					if (index < 0) {
						index = (index * -1) - 1;
					}
					getChildren().add(index, fti);
				} else {
					FileTreeItem[] array = files.values().toArray(new FileTreeItem[0]);
					int index = Arrays.binarySearch(array, fti.getValue());
					if (index < 0) {
						index = (index * -1) - 1;
					}
					getChildren().add(sizeD + index, fti);
				}
			} catch (Exception e) {
				Logging.fatal(e);
			}
		}

		public void remove(FileTreeItem item) {
			String name = trim(item.getValue());
			if (item.isDir) {
				dirs.remove(name);
			} else {
				files.remove(name);
			}
			getChildren().remove(item);
		}

		FileTreeItem getDir(String name) {
			return dirs.get(name);
		}

		boolean hasDir(String name) {
			return dirs.containsKey(name);
		}

		FileTreeItem getFile(String name) {
			return files.get(name);
		}

		@Override
		public int compareTo(String s) {
			return getValue().compareTo(s);
		}
	}
}