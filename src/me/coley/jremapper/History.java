package me.coley.jremapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.coley.bmf.mapping.AbstractMapping;

public class History {
	private final RollingList<String> selectedClasses = new RollingList<>(10);
	private final RollingList<RenameAction> renameActions = new RollingList<>(10);
	private final List<Runnable> selectionListeners = new ArrayList<>();
	private final List<Runnable> renameListeners = new ArrayList<>();
	private final Map<String, AbstractMapping> renameToOriginal = new HashMap<>();

	/**
	 * Updates the list of classes that were selected.
	 * 
	 * @param title
	 *            Name of class.
	 */
	public void onSelectClass(String title) {
		// check if latest selection is the same as the new one. If so do not
		// add it again.
		String s = selectedClasses.getLatest();
		if (s != null && s.equals(title)) {
			return;
		}
		selectedClasses.add(title);
		selectionListeners.stream().forEach((r) -> r.run());
	}

	/**
	 * Registers a runnable to execute when the selection history is updated.
	 * 
	 * @param runnable
	 */
	public void registerSelectionUpdate(Runnable runnable) {
		selectionListeners.add(runnable);
	}

	/**
	 * Returns the list of classes that were selected.
	 * 
	 * @return List<String> of class names.
	 */
	public List<String> getSelectedClasses() {
		return selectedClasses.list;
	}

	/**
	 * Updates the list of rename actions.
	 * 
	 * @param title
	 *            Name of class.
	 */
	public void onRename(AbstractMapping mapping, String before, String after) {
		renameActions.add(new RenameAction(mapping, before, after));
		renameToOriginal.put(after, mapping);
		renameListeners.stream().forEach((r) -> r.run());
	}

	/**
	 * Removes a rename action from the list of rename actions.
	 * 
	 * @param rename
	 */
	public void onUndo(RenameAction rename) {
		renameActions.remove(rename);
	}

	/**
	 * Registers a runnable to execute when the rename history is updated.
	 * 
	 * @param runnable
	 */
	public void registerRenameUpdate(Runnable runnable) {
		renameListeners.add(runnable);
	}

	/**
	 * Returns the list of rename actions done.
	 * 
	 * @return List<RenameAction>
	 */
	public List<RenameAction> getRenameActions() {
		return renameActions.list;
	}

	/**
	 * Returns the list of renamed names to the mapping instance associated with
	 * the renaming.
	 * 
	 * @return
	 */
	public Map<String, AbstractMapping> getRenamedToMappingMap() {
		return renameToOriginal;
	}

	public class RenameAction {
		private final AbstractMapping mapping;
		private final String before, after;

		RenameAction(AbstractMapping mapping, String before, String after) {
			this.mapping = mapping;
			this.before = before;
			this.after = after;
		}

		public AbstractMapping getMapping() {
			return mapping;
		}

		public String getBefore() {
			return before;
		}

		public String getAfter() {
			return after;
		}
	}

	class RollingList<T> {
		private final List<T> list = new ArrayList<>();
		private int capacity;

		RollingList(int capacity) {
			this.capacity = capacity;
		}

		public T getLatest() {
			int size = list.size();
			if (size == 0) {
				return null;
			}
			return list.get(size - 1);
		}

		public void add(T item) {
			if (list.size() == capacity) {
				list.remove(0);
			}
			list.add(item);
		}

		public void remove(T item) {
			remove(list.indexOf(item));
		}

		public void remove(int i) {
			list.remove(i);
		}
	}
}
