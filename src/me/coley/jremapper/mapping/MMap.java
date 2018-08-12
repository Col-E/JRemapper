package me.coley.jremapper.mapping;

public class MMap extends AbstractMapping {
	/**
	 * Original member descriptor.
	 */
	private final String originalDesc;

	public MMap(String originalName, String originalDesc) {
		super(originalName);
		this.originalDesc = originalDesc;
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
}
