package me.coley.jremapper.mapping;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import me.coley.jremapper.asm.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.jremapper.event.NewInputEvent;
import me.coley.jremapper.util.Files;
import me.coley.jremapper.util.Logging;

/**
 * Mapping manager.
 * 
 * @author Matt
 */
public enum Mappings {
	/**
	 * Singleton access is much less of a hassle in this case. Events keep
	 * everything up-to-date.
	 */
	INSTANCE;
	/**
	 * Map of internal class names, to their class mappings.
	 */
	private final Map<String, CMap> mappings = new HashMap<>();
	/**
	 * The current input.
	 */
	private Input input;

	// ------------------------------------------------------------- //

	/**
	 * Get mappings for the class.
	 * 
	 * @param originalName
	 *            The non-mapped name of the class.
	 * @return Mapping wrapper for the class.
	 */
	public CMap getClassMapping(String originalName) {
		return mappings.get(originalName);
	}

	/**
	 * Get the mappings for the class, specified by its current name.
	 * 
	 * @param currentName
	 *            The mapped name of a class.
	 * @return Mapping wrapper for the class.
	 */
	public CMap getClassReverseMapping(String currentName) {
		return mappings.values().stream()
				.filter(AbstractMapping::isDirty)
				.filter(cm -> cm.getCurrentName().equals(currentName))
				.findFirst().orElse(null);
	}

	/**
	 * @return Collection of class mappings, which contain the member mappings.
	 */
	public Collection<CMap> getMappings() {
		return mappings.values();
	}

	// ------------------------------------------------------------- //

	/**
	 * @param name
	 *            Internal class name.
	 * @return Current mapped name.
	 */
	public String getTransformedName(String name) {
		CMap map = getClassMapping(name);
		return map == null ? name : map.getCurrentName();
	}

	/**
	 * @param desc
	 *            Internal member descriptor.
	 * @return Current mapped descriptor.
	 */
	public String getTransformedDesc(String desc) {
		return RemapperImpl.create().mapDesc(desc);
	}

	/**
	 * @param raw
	 *            Bytecode input.
	 * @return Bytecode output.
	 */
	public byte[] intercept(byte[] raw) {
		try {
			RemapperImpl mapper = RemapperImpl.create();
			ClassReader cr = new ClassReader(raw);
			ClassWriter cw = new ClassWriter(0);
			ClassRemapper adapter = new ClassRemapperExt(cw, mapper, cr.getClassName());
			cr.accept(adapter, 0);
			return cw.toByteArray();
		} catch (Exception e) {
			Logging.error(e);
			return raw;
		}
	}

	// ------------------------------------------------------------- //

	@Listener
	public void onInput(NewInputEvent event) {
		input = event.get();
		// reset and repopulate mappings.
		mappings.clear();
		Map<CMap, Set<String>> inners = new HashMap<>();
		input.rawNodeMap.forEach((name, raw) -> {
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM7) {
				private CMap cm;

				@Override
				public void visit(int version, int access, String name, String signature, String superName,
						String[] interfaces) {
					mappings.put(name, cm = new CMap(name));
				}

				@Override
				public void visitInnerClass(final String name, final String outerName, final String innerName,
						final int access) {
					if (cm.getOriginalName().endsWith(name)) {
						return;
					}
					Set<String> value = inners.getOrDefault(cm, new HashSet<>());
					value.add(name);
					inners.put(cm, value);
				}

				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature,
						Object value) {
					cm.addMember(name, descriptor);
					return null;
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
						String[] exceptions) {
					MMap mm = cm.addMember(name, descriptor);
					return new MethodVisitor(Opcodes.ASM7) {
						@Override
						public void visitLocalVariable(String name, String desc, String s, Label start, Label end, int index) {
							if (index == 0 && "this".equals(name))
								return;
							mm.addVariable(name, desc);
						}
					};
				}

			};
			new ClassReader(raw).accept(cv, 0);
		});
		for (Entry<CMap, Set<String>> entry : inners.entrySet()) {
			for (String name : entry.getValue()) {
				entry.getKey().addInner(mappings.get(name));
			}

		}
	}

	// ------------------------------------------------------------- //

	private Mappings() {
		Bus.subscribe(this);
	}

	public void init() {}

	public void loadMapping(File file) {
		try {
			JsonValue value = Json.parse(Files.readFile(file.getAbsolutePath()));
			if (value.isArray()) {
				value.asArray().forEach(mc -> {
					JsonObject mcc = mc.asObject();
					CMap cm = mappings.get(mcc.getString("name-in", null));
					if (cm != null) {
						String out = mcc.getString("name-out", null);
						if (out != null) {
							cm.setCurrentName(out);
						}
					}
					JsonValue members = mcc.get("members");
					if (members == null)
						return;
					members.asArray().forEach(mm -> {
						JsonObject mmm = mm.asObject();
						String nameIn = mmm.getString("name-in", null);
						String descIn = mmm.getString("desc-in", null);
						if (nameIn != null && descIn != null && cm != null) {
							MMap a = cm.lookup(nameIn, descIn);
							if (a != null) {
								String nameOut = mmm.getString("name-out", null);
								if (nameOut != null) {
									a.setCurrentName(nameOut);
								}
								JsonValue variables = mmm.get("variables");
								if (variables == null)
									return;
								variables.asArray().forEach(vm -> {
									JsonObject mmmm = vm.asObject();
									String vnameIn = mmmm.getString("name-in", null);
									String vdescIn = mmmm.getString("desc-in", null);
									if(vnameIn != null && vdescIn != null && a != null) {
										VMap varMap = a.getVariableByName(vnameIn);
										String vnameOut = mmmm.getString("name-out", null);
										if(vnameOut != null) {
											varMap.setCurrentName(vnameOut);
										}
									}
								});
							}

						}

					});
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JsonValue toMapping() {
		JsonArray array = Json.array();
		for (CMap cm : mappings.values()) {
			if (!cm.isDirty())
				continue;
			JsonObject mapClass = Json.object();
			mapClass.add("name-in", cm.getOriginalName());
			if (cm.isRenamed())
				mapClass.add("name-out", cm.getCurrentName());
			JsonArray members = Json.array();
			for (MMap mm : cm.getMembers()) {
				if (!mm.isDirty())
					continue;
				JsonObject mapMember = Json.object();
				mapMember.add("name-in", mm.getOriginalName());
				if (mm.isRenamed())
					mapMember.add("name-out", mm.getCurrentName());
				mapMember.add("desc-in", mm.getOriginalDesc());
				mapMember.add("desc-out", mm.getCurrentDesc());
				members.add(mapMember);
				JsonArray variables = Json.array();
				for (VMap vm : mm.getVariables()){
					if (!vm.isDirty())
						continue;
					JsonObject mapVariable = Json.object();
					mapVariable.add("name-in", vm.getOriginalName());
					if (vm.isRenamed())
						mapVariable.add("name-out", vm.getCurrentName());
					mapVariable.add("desc-in", vm.getOriginalDesc());
					mapVariable.add("desc-out", vm.getCurrentDesc());
					variables.add(mapVariable);
				}
				if (variables.size() > 0)
					mapMember.add("variables", variables);
			}
			if (members.size() > 0)
				mapClass.add("members", members);
			array.add(mapClass);
		}
		return array;
	}
}