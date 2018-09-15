package me.coley.jremapper;

import me.coley.jremapper.mapping.Hierarchy;
import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.ui.FxWindow;

public class JRemapper {
	public static void main(String[] args) {
		// Initialize the mapping listeners
		Mappings.INSTANCE.init();
		// Register hierarchy listeners by calling an arbitrary method in the
		// class. This will load it.
		Hierarchy.getStatus();
		// Open the UI.
		FxWindow.init(args);
	}
}
