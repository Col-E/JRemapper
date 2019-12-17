package me.coley.jremapper.mapping;

import java.util.ArrayList;
import java.util.List;

public class MMap extends AbstractMapping {
	/**
	 * Original member descriptor.
	 */
	private final String originalDesc;
	/**
	 * Mapping of class that holds this member.
	 */
	private final CMap owner;
	/**
	 * Variables.
	 */
	private final List<VMap> variables = new ArrayList<>();

	public MMap(CMap owner, String originalName, String originalDesc) {
		super(originalName);
		this.owner = owner;
		this.originalDesc = originalDesc;
	}

	public List<VMap> getVariables() {
		return variables;
	}

	public void addVariable(String name, String desc) {
		variables.add(new VMap(this, name, desc));
	}

	@Override
	public void setCurrentName(String currentName) {
		owner.setDirty(true);
		setDirty(true);
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

	public VMap getVariableByName(String original) {
		return variables.stream().filter(vm -> original.equals(vm.getOriginalName())).findFirst().orElse(null);
	}
}
