package me.coley.jremapper;

import java.io.IOException;
import java.util.Map;

import me.coley.bmf.ClassNode;
import me.coley.bmf.ClassWriter;
import me.coley.bmf.JarReader;
import me.coley.bmf.mapping.ClassMapping;

/**
 * Lookup helper for CFR since it requests this data in order to show anonymous
 * inner classes and such. It'll be requesting renamed names though so that's
 * where this comes in.
 */
public class CFRResourceLookup {
	private Program program;

	public CFRResourceLookup(Program program) {
		this.program = program;
	}

	public byte[] get(String path) {
		JarReader jar = program.getJarReader();
		byte[] bytes = null;
		try {
			Map<String, ClassNode> classes = jar.getClassEntries();
			// check if the class name is contained in the class entries.
			// if not, check mappings for it being the renamed value of
			// something.
			if (jar.getClassEntries().containsKey(path)) {
				bytes = ClassWriter.write(classes.get(path));
			} else {
				// It sucks that we need to iterate all the mappings, but for
				// now I don't have a way to make an easy rename-->oldname
				// lookup.
				for (ClassMapping cm : jar.getMapping().getMappings().values()) {
					boolean updated = !cm.name.original.equals(cm.name.getValue());
					if (updated && cm.name.getValue().equals(path)) {
						bytes = ClassWriter.write(classes.get(cm.name.original));
						break;
					}
				}
			}
		} catch (IOException e) {
			program.getWindow().openTab("Error",
					"Error: Failed to sending bytes to CFR (Could not recompile via BMF)\n\n" + e.getMessage());
		}
		return bytes;
	}

}
