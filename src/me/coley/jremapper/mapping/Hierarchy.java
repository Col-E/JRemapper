package me.coley.jremapper.mapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.jremapper.event.MappingChangeEvent;
import me.coley.jremapper.event.NewInputEvent;
import me.coley.jremapper.util.Logging;
import me.coley.jremapper.util.Threads;

/**
 * ClassNode inheritance/method override utility.
 * 
 * @author Matt
 */
public enum Hierarchy {
	INSTANCE;
	/**
	 * Key: Class name.<br>
	 * Value: Class wrapper as graph vertex.
	 */
	private final Map<String, CVert> classes = new HashMap<>();
	/**
	 * Key: String representation of NameType.<br>
	 * Value: NameType instance.
	 */
	private final Map<String, NameType> types = new HashMap<>();
	/**
	 * Key: String representation of NameType.<br>
	 * Value: Set of groups of the NameType. Each group belongs to a different
	 * collection of classes.
	 */
	private final Map<String, Set<MGroup>> groupMap = new HashMap<>();
	/**
	 * Set of classes already visited during hierarchy generation.
	 */
	private final Set<CVert> visitedGroupHosts = new HashSet<>();
	/**
	 * Status of what has been loaded.
	 */
	private LoadStatus status = LoadStatus.NONE;

	private Hierarchy() {
			Bus.subscribe(this);
	}

	public void onClassRename(CMap mapping, String past, String updated) {
		Bus.post(new MappingChangeEvent(mapping, updated));
	}

	public void onMemberRename(MMap mapping, String past, String updated) {
		NameType type = type(mapping.getOriginalName(), mapping.getOriginalDesc());
		MGroup group = getGroup(type, mapping.getOwner().getOriginalName());
		if (group == null) {
			throw new RuntimeException("Failed to update method-hierarchy: Failed to get method-group");
		}
		group.setName(updated);
	}

	@Listener
	private void onNewInput(NewInputEvent input) {
		// Reset values
		classes.clear();
		groupMap.clear();
		types.clear();
		visitedGroupHosts.clear();
		Threads.run(() -> {
			try {
				long start = System.currentTimeMillis();
				status = LoadStatus.NONE;
				Logging.info("Generating inheritence hierarchy");
				setupVertices(input.get().genNodes());
				setupEdges();
				status = LoadStatus.CLASSES;
				setupNameType();
				setupMethodGroups();
				setupMethodLocks();
				status = LoadStatus.METHODS;
				long now = System.currentTimeMillis();
				Logging.info("Finished generating inheritence hierarchy: took " + (now - start) + "ms");
			} catch (Exception e) {
				Logging.error(e);
			}
		});
	}

	/**
	 * Setup CVert lookup for all ClassNodes.
	 * 
	 * @param nodes
	 *            Map of ClassNodes.
	 */
	private void setupVertices(Map<String, ClassNode> nodes) {
		ExecutorService pool = Threads.pool(40);
		// Typically iterating the keyset and fetching the value is a bad idea.
		// But due to the nature of lazy-loading and the ability to multithread
		// the loads, this should be faster.
		for (String name : nodes.keySet()) {
			pool.execute(() -> {
				ClassNode cn = nodes.get(name);
				classes.put(cn.name, new CVert(cn));
			});
		}
		Threads.waitForCompletion(pool);
	}

	/**
	 * Setup parent-child relations in the {@link #classes CVert map} based on
	 * superclass/interface relations of the wrapped ClassNode value.
	 */
	private void setupEdges() {
		for (CVert vert : classes.values()) {
			// Get superclass vertex
			CVert other = classes.get(vert.data.superName);
			if (other != null) {
				vert.parents.add(other);
				other.children.add(vert);
			} else {
				// Could not find superclass, note that for later.
				vert.externalParents.add(vert.data.superName);
			}
			// Iterate interfaces, get interface vertex and do the same
			for (String inter : vert.data.interfaces) {
				other = classes.get(inter);
				if (other != null) {
					vert.parents.add(other);
					other.children.add(vert);
				} else {
					// Could not find superclass, note that for later.
					vert.externalParents.add(inter);
				}
			}
		}
	}

	/**
	 * Setup NameType lookup for all methods.
	 */
	private void setupNameType() {
		for (CVert vert : classes.values()) {
			for (MethodNode mn : vert.data.methods) {
				type(mn);
			}
		}
	}

	/**
	 * Setup method-groups. Requires all types and edges to be generated first.
	 */
	private void setupMethodGroups() {
		for (CVert cv : classes.values()) {
			createGroups(cv);
		}
	}

	private void createGroups(CVert root) {
		// Check if the vertex has been visited already.
		if (visitedGroupHosts.contains(root)) {
			return;
		}
		// Track visited vertices / queue vertices to visit later.
		Deque<CVert> queue = new LinkedList<>();
		Set<CVert> visited = new HashSet<>();
		// Since this will visit all classes in the to-be generated groups we
		// can make a map that will contain the NameTypes of these groups and
		// link them with the groups.
		Map<NameType, MGroup> typeGroups = new HashMap<>();
		// Initial vertex
		queue.add(root);
		visited.add(root);
		while (!queue.isEmpty()) {
			CVert node = queue.poll();
			// Mark so that future calls to createGroups can be skipped.
			visitedGroupHosts.add(node);
			// Iterate over methods, fetch the group by the NameType and then
			// add the current vertext to the group.
			for (MethodNode mn : node.data.methods) {
				NameType type = type(mn);
				MGroup group = typeGroups.get(type);
				if (group == null) {
					group = new MGroup(type);
					typeGroups.put(type, group);
				}
				group.definers.add(node);
			}
			// Queue up the unvisited parent and child nodes.
			Set<CVert> v = new HashSet<>();
			v.addAll(node.parents);
			v.addAll(node.children);
			for (CVert other : v) {
				if (other != null && !visited.contains(other)) {
					queue.add(other);
					visited.add(other);
				}
			}
		}
		// For every NameType/Group add to the main group-map.
		for (Entry<NameType, MGroup> e : typeGroups.entrySet()) {
			Set<MGroup> groups = groupMap.get(e.getKey().toString());
			if (groups == null) {
				groups = new HashSet<>();
				groupMap.put(e.getKey().toString(), groups);
			}
			groups.add(e.getValue());
		}
	}

	/**
	 * @param type
	 *            NameType of a method.
	 * @param owner
	 *            Class that has declared the method.
	 * @return Group representing method by the given NameType, group also
	 *         contains the given owner as a defining class of the method. May
	 *         be {@code null} if the owner does not declare a method by the
	 *         given NameType.
	 */
	private MGroup getGroup(NameType type, String owner) {
		CVert vert = classes.get(owner);
		if (vert == null) {
			throw new RuntimeException("Cannot fetch-group with passed owner: null");
		}
		Set<MGroup> groupSet = groupMap.get(type.toString());
		if (groupSet != null) {
			// Check for owner match.
			for (MGroup group : groupSet) {
				if (group.definers.contains(vert)) {
					return group;
				}
			}
		}
		return null;
	}

	/**
	 * Recall that in {@link #setupEdges()} that edges to external classes were
	 * logged. Now we will iterate over those external names and check if they
	 * can be loaded via Reflection. If so we will check the methods in these
	 * classes and lock all groups with matching NameTypes to prevent renaming
	 * of core-methods.
	 */
	private void setupMethodLocks() {
		Set<String> externalRefs = new HashSet<>();
		for (CVert cv : classes.values()) {
			for (String external : cv.externalParents) {
				if (external != null) {
					externalRefs.add(external);
				}
			}
		}
		ExecutorService pool = Threads.pool(10);
		for (String external : externalRefs) {
			pool.execute(() -> {
				String className = external.replace("/", ".");
				try {
					// Load class without initialization
					Class<?> cls = Class.forName(className, false, ClassLoader.getSystemClassLoader());
					for (Method method : cls.getDeclaredMethods()) {
						// Get NameType from method
						String name = method.getName();
						String desc = Type.getMethodDescriptor(method).toString();
						NameType type = type(name, desc);
						// Lock groups
						Set<MGroup> groups = groupMap.get(type.toString());
						if (groups != null) {
							for (MGroup group : groups) {
								group.locked = true;
							}
						}
					}
				} catch (Exception e) {}
			});
		}
		Threads.waitForCompletion(pool);
	}

	/**
	 * @param mn
	 *            MethodNode.
	 * @return NameType of MethodNode.
	 */
	private NameType type(MethodNode mn) {
		return type(mn.name + mn.desc, mn.name, mn.desc);
	}

	/**
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method descriptor.
	 * @return NameType of declared method.
	 */
	private NameType type(String name, String desc) {
		return type(name + desc, name, desc);
	}

	/**
	 * 
	 * @param def
	 *            Key for NameType lookup.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method descriptor.
	 * @return NameType of declared method.
	 */
	private NameType type(String def, String name, String desc) {
		NameType type = types.get(def);
		if (type == null) {
			type = new NameType(name, desc);
			types.put(def, type);
		}
		return type;
	}
	
	/**
	 * @param name
	 *            Internal name.
	 * @return Vertex of class.
	 */
	public CVert getVertex(String name) {
		return classes.get(name);
	}

	/**
	 * @return Content of hierarchy loaded.
	 */
	public static LoadStatus getStatus() {
		return INSTANCE.status;
	}

	/**
	 * Status to define what has been loaded.
	 * 
	 * @author Matt
	 */
	public enum LoadStatus {
		/**
		 * Nothing has been loaded.
		 */
		NONE,
		/**
		 * Class hierarchy has been loaded.
		 */
		CLASSES,
		/**
		 * Class + method hierarchies have been loaded
		 */
		METHODS;
	}
}
