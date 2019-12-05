package me.coley.jremapper.mapping;

import java.util.Objects;

public abstract class AbstractMapping {
	/**
	 * Original mapping name.
	 */
	private final String originalName;
	/**
	 * Current mapping name.
	 */
	private String currentName;
	/**
	 * Has the mapping been modified.
	 */
	private boolean dirty;


	public AbstractMapping(String originalName) {
		currentName = (this.originalName = originalName);
	}

	/**
	 * @return Original mapping name.
	 */
	public String getOriginalName() {
		return originalName;
	}

	/**
	 * @return Current mapping name.
	 */
	public String getCurrentName() {
		return currentName;
	}

	/**
	 * @param currentName
	 *            New name to set.
	 */
	public void setCurrentName(String currentName) {
		dirty = true;
		this.currentName = currentName;
	}

	/**
	 * @return {@code true} if the original and current names do not match.
	 */
	public boolean isRenamed() {
		return !getOriginalName().equals(getCurrentName());
	}

	public boolean isDirty() {
		return dirty;
	}

	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public String toString() {
		if (isRenamed()) {
			return getOriginalName() + " -> " + getCurrentName();
		}
		return getOriginalName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(originalName, currentName);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AbstractMapping) {
			// I don't think collisions are common enough for this to be a concern.
			return hashCode() == o.hashCode();
		}
		return false;
	}
}
