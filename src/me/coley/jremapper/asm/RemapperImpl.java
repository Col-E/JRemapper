package me.coley.jremapper.asm;

import java.util.HashMap;
import java.util.Map;

import me.coley.jremapper.mapping.*;
import org.objectweb.asm.commons.SimpleRemapper;

public class RemapperImpl extends SimpleRemapper {

	private RemapperImpl(Map<String, String> mapping) {
		super(mapping);
	}

	/**
	 * @param owner
	 * 		Class containing the method.
	 * @param methodName
	 * 		Method name.
	 * @param methodDesc
	 * 		Method descriptor.
	 * @param name
	 * 		Name of variable in method.
	 *
	 * @return Remapped variable name.
	 */
	public String mapVariableName(String owner, String methodName, String methodDesc, String name) {
		String remappedName = map(owner + '.' + methodName + methodDesc + '.' + name);
		return remappedName == null ? name : remappedName;
	}

	/**
	 * @return Instance of the mapper, generated using current mappings.
	 */
	public static RemapperImpl create() {
		Map<String, String> mapping = new HashMap<>();
		Mappings.INSTANCE.getMappings().stream()
				.filter(AbstractMapping::isDirty)
				.forEach(cm -> {
			mapping.put(cm.getOriginalName(), cm.getCurrentName());
			cm.getMembers().stream()
					.filter(AbstractMapping::isDirty).forEach(mm -> {
				if(mm.isMethod()) {
					// Include renamed method
					if (mm.isRenamed())
						mapping.put(cm.getOriginalName() + "." + mm.getOriginalName() + mm.getOriginalDesc(), mm.getCurrentName());
					// Include renamed variables
					mm.getVariables().stream().filter(VMap::isRenamed).forEach(vm ->
						mapping.put(cm.getOriginalName() + "." + mm.getOriginalName() + mm.getOriginalDesc() + "." + vm.getOriginalName(), vm.getCurrentName())
					);
				} else if (mm.isRenamed()) {
					// Include renamed fields
					mapping.put(cm.getOriginalName() + "." + mm.getOriginalName(),
							mm.getCurrentName());
				}
			});
		});
		return new RemapperImpl(mapping);
	}
}
