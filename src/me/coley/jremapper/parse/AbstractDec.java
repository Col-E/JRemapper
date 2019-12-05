package me.coley.jremapper.parse;

import org.benf.cfr.reader.util.Troolean;

import me.coley.jremapper.mapping.AbstractMapping;

public abstract class AbstractDec<M extends AbstractMapping> {
	private Troolean mappingStatus = Troolean.NEITHER;
	private M mapping;

	/**
	 * @return Mappings for the class.
	 */
	public M map() {
		if (hasMappings()) {
			return mapping;
		}
		return null;
	}

	/**
	 * @return Check if this class has mappings.
	 */
	public boolean hasMappings() {
		switch (mappingStatus) {
		case FALSE:
			return false;
		case TRUE:
			return true;
		default:
			boolean mapped = ((mapping = lookup()) != null || (mapping = lookupReverse()) != null);
			mappingStatus = Troolean.get(mapped);
			return mapped;
		}
	}

	/**
	 * Find the mapping of this declared object as a non-mapped item.
	 * 
	 * @return
	 */
	protected abstract M lookup();

	/**
	 * Find the mapping of this declared object as a mapped item.
	 * 
	 * @return
	 */
	protected abstract M lookupReverse();

	/**
	 * Required mapping information could not be found.
	 */
	protected abstract void throwMappingFailure();

	/**
	 * @return {@code true} if this declaration is remapped.
	 */
	public boolean isRenamed() {
		if (mappingStatus == Troolean.FALSE)
			return false;
		if (mapping == null && mappingStatus == Troolean.NEITHER && !hasMappings())
			throwMappingFailure();
		return mapping != null && mapping.isRenamed();
	}

	/**
	 * Set {@link #mappingStatus} to false, ensuring that external components will
	 * be told that this declaration has no mappings.
	 */
	public void lock() {
		mappingStatus = Troolean.FALSE;
	}
}
