package me.coley.jremapper.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CMap extends AbstractMapping {
	private final Map<String, MMap> members = new HashMap<>();
	private final List<CMap> inners = new ArrayList<>();

	public CMap(String originalName) {
		super(originalName);
	}

	public MMap lookup(String name, String desc) {
		return members.get(key(name, desc));
	}

	public MMap lookupReverse(String name, String desc) {
		Optional<MMap> optMap = members.values().stream().filter(mm -> mm.getCurrentName().equals(name) && mm.getCurrentDesc().equals(desc)).findFirst();
		if (optMap.isPresent()) {
			return optMap.get();
		}
		return null;
	}

	public void addMember(String name, String desc) {
		String key = key(name, desc);
		members.put(key, new MMap(this, name, desc));
	}

	private String key(String name, String desc) {
		return name + "#" + desc;
	}

	public Collection<MMap> getMembers() {
		return members.values();
	}
	
	@Override
	public void setCurrentName(String currentName) {
		String past = getCurrentName();
		Hierarchy.INSTANCE.onClassRename(this, past, currentName);
		super.setCurrentName(currentName);
		for (CMap inner : inners) {
			String ic = inner.getCurrentName();
			String innerPart = ic.substring(past.length());
			inner.setCurrentName(currentName + innerPart);
		}
	}

	public void addInner(CMap inner) {
		if (inner == null)
			throw new RuntimeException();
		inners.add(inner);
	}
}
