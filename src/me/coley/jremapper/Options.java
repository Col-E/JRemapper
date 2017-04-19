package me.coley.jremapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Program options not related to CFR decompilation
 */
public class Options {
	public static final String REGEX_REPLACE_CLASSES = "Regex Replace Classes";
	public static final String REGEX_REPLACE_MEMBERS = "Regex Replace Members";
	public static final String REFRESH_ON_SELECT = "Refresh classes when re-decompiled.";
	private Map<String, Boolean> options = new HashMap<String, Boolean>();

	public Options() {
		// Default values
		options.put(REGEX_REPLACE_CLASSES, false);
		options.put(REGEX_REPLACE_MEMBERS, false);
		options.put(REFRESH_ON_SELECT, true);
	}

	public void set(String setting, boolean selected) {
		options.put(setting, selected);
	}

	public boolean get(String option) {
		return options.getOrDefault(option, false);
	}

	public Map<String, Boolean> getOptions() {
		return options;
	}

}
