package me.coley.jremapper.mapping;

public class VMap extends AbstractMapping {
	/**
	 * Original variable descriptor.
	 */
	private final String originalDesc;
	/**
	 * Mapping of member that holds this variable.
	 */
	private final MMap owner;

	public VMap(MMap owner, String originalName, String originalDesc) {
		super(originalName);
		this.owner = owner;
		this.originalDesc = originalDesc;
	}

	@Override
	public void setCurrentName(String currentName) {
		super.setCurrentName(currentName);
		owner.setDirty(true);
		owner.getOwner().setDirty(true);
	}

	/**
	 * @return Original variable type descriptor.
	 */
	public String getOriginalDesc() {
		return originalDesc;
	}

	public String getCurrentDesc() {
		return Mappings.INSTANCE.getTransformedDesc(originalDesc);
	}

	public MMap getOwner() {
		return owner;
	}
}
