package me.coley.jremapper.mapping;

public class MMap extends AbstractMapping {
	/**
	 * Original member descriptor.
	 */
	private final String originalDesc;
	/**
	 * Mapping of class that holds this member.
	 */
	private final CMap owner;

	public MMap(CMap owner, String originalName, String originalDesc) {
		super(originalName);
		this.owner = owner;
		this.originalDesc = originalDesc;
	}

	@Override
	public void setCurrentName(String currentName) {
		String past = getCurrentName();
		if (isMethod()) {
			Hierarchy.INSTANCE.onMemberRename(this, past, currentName);
		} else {
			setCurrentNameNoHierarchy(currentName);
		}
	}
	
	public void setCurrentNameNoHierarchy(String currentName) {
		super.setCurrentName(currentName);
	}
	
	/**
	 * @return Original member type descriptor.
	 */
	public String getOriginalDesc() {
		return originalDesc;
	}

	public String getCurrentDesc() {
		return Mappings.INSTANCE.getTransformedDesc(originalDesc);
	}

	public boolean isField() {
		return !originalDesc.contains("(");
	}

	public boolean isMethod() {
		return originalDesc.contains("(");
	}
	
	public CMap getOwner() {
		return owner;
	}
}
