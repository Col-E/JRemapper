package me.coley.jremapper;

import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.ui.FxWindow;

public class JRemapper {
	public static void main(String[] args) { 
		Mappings.INSTANCE.init();
		FxWindow.init(args);
	}
}
