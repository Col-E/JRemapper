package me.coley.jremapper.asm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.commons.SimpleRemapper;

import me.coley.jremapper.mapping.Mappings;

public class RemapperImpl extends SimpleRemapper {

	private RemapperImpl(Map<String, String> mapping) {
		super(mapping);
	}
	
	public static RemapperImpl create() {
		Map<String, String> mapping = new HashMap<>();
		Mappings.INSTANCE.getMappings().forEach(cm -> {
			mapping.put(cm.getOriginalName(), cm.getCurrentName());
			cm.getMembers().forEach(mm -> {
				if (mm.isMethod()) {
					mapping.put(cm.getOriginalName() + "." + mm.getOriginalName() + mm.getOriginalDesc(), mm.getCurrentName());
				} else {
					mapping.put(cm.getOriginalName() + "." + mm.getOriginalName(), mm.getCurrentName());
				}
			});
		});
		return new RemapperImpl(mapping);
	}

}
