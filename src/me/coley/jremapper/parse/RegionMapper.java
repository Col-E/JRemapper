package me.coley.jremapper.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.Range;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.*;

import me.coley.jremapper.asm.Input;
import me.coley.jremapper.mapping.CMap;
import me.coley.jremapper.mapping.CVert;
import me.coley.jremapper.mapping.Hierarchy;
import me.coley.jremapper.mapping.MMap;
import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.util.Logging;

/**
 * Allows linking regions of text do different mappings using the JavaParser
 * library.
 * 
 * For reference:		
 * <ul>		
 * <li>Quantified name: Full name of a class, such as		
 * <i>com.example.MyType</i></li>		
 * <li>Simple name: Short-hand name of a class, such as <i>MyType</i></li>		
 * </ul>
 * 
 * @author Matt
 */
public class RegionMapper {
	private final Input input;
	/**
	 * The declaration of the parsed class.
	 */
	private final CDec decClass;
	/**
	 * The AST of the parsed class.
	 */
	private final CompilationUnit cu;
	/**
	 * Package name -> List of classes in package.
	 */
	private final Map<String, Set<CDec>> packageToClasses = new HashMap<>();
	/**
	 * Simple name -> List of classes with the simple name, regardless of package.
	 */
	private final Map<String, Set<CDec>> simpleToQuantified = new HashMap<>();
	/**
	 * Quanitied name -> class dec
	 */
	private final Map<String, CDec> quantifiedToDec = new HashMap<>();
	/**
	 * Class dec -> set of ranges linked to the class
	 */
	private final Map<CDec, Set<Range>> classRanges = new HashMap<>();
	/**
	 * Member dec -> set of ranges linked to the member
	 */
	private final Map<MDec, Set<Range>> memberRanges = new HashMap<>();

	public RegionMapper(Input input, String className, CompilationUnit cu) {
		this.input = input;
		this.cu = cu;
		this.decClass = CDec.fromClass(Mappings.INSTANCE.getTransformedName(className));
		populateLookups();
		populateMembers();
		markClassRanges();
		markMemberRanges();
		markOtherRanges();
	}

	public CDec getHost() {
		return decClass;
	}

	/**
	 * Mark ranges in the text that denote classes.
	 */
	private void markClassRanges() {
		// Add ranges for references
		List<ReferenceType> references = cu.findAll(ReferenceType.class);
		for (ReferenceType clazz : references) {
			if (clazz.getRange().isPresent()) {
				Range range = clazz.getRange().get();
				CDec dec = fetchClassDec(clazz);
				if (dec != null) {
					getClassRanges(dec).add(range);
				}
			}
		}
		// Add ranges for declarations
		@SuppressWarnings("rawtypes")
		List<TypeDeclaration> declarations = cu.findAll(TypeDeclaration.class);
		for (TypeDeclaration<?> clazz : declarations) {
			SimpleName name = clazz.getName();
			if (name.getRange().isPresent()) {
				Range range = name.getRange().get();
				CDec dec = fetchClassDec(name);
				if (dec != null) {
					getClassRanges(dec).add(range);
				}
			}
		}
		// Add ranges for constructors
		List<ConstructorDeclaration> constructors = cu.findAll(ConstructorDeclaration.class);
		for (ConstructorDeclaration clazz : constructors) {
			SimpleName name = clazz.getName();
			if (name.getRange().isPresent()) {
				Range range = name.getRange().get();
				CDec dec = fetchClassDec(name);
				if (dec != null) {
					getClassRanges(dec).add(range);
				}
			}
		}
		// Add ranges for "new MyType()"
		List<ObjectCreationExpr> newOperators = cu.findAll(ObjectCreationExpr.class);
		for (ObjectCreationExpr newExpr : newOperators) {
			if (!newExpr.getScope().isPresent()) {
				// No scope on creation, so it will be "new Type()"
				CDec cdec = fetchClassDec(newExpr.getTypeAsString(), true);
				if (cdec != null) {
					getClassRanges(cdec).add(newExpr.getType().getRange().get());
				}
			}
		}

	}

	/**
	 * Mark ranges in the text that denote members of the {@link #decClass current
	 * class}.
	 */
	private void markMemberRanges() {
		// Mark declared fields
		List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
		for (FieldDeclaration fd : fields) {
			Optional<Range> nameRange = fd.getVariable(0).getRange();
			if (!nameRange.isPresent())
				continue;
			String name = fd.getVariable(0).getNameAsString();
			String desc = getDescriptor(fd.getCommonType());
			MDec member = decClass.getMember(name, desc);
			if (member != null) getMemberRanges(member).add(nameRange.get());
		}
		// Mark declared methods
		List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
		for (MethodDeclaration md : methods) {
			Optional<Range> nameRange = md.getName().getRange();
			if (!nameRange.isPresent())
				continue;
			String name = md.getNameAsString();
			String desc = getMethodDesc(md);
			MDec member = decClass.getMember(name, desc);
			if (member != null) getMemberRanges(member).add(nameRange.get());
		}
		// Mark field references like "this.myField"
		List<FieldAccessExpr> fieldRefs = cu.findAll(FieldAccessExpr.class);
		for (FieldAccessExpr fa : fieldRefs) {
			Optional<Range> nameRange = fa.getName().getRange();
			if (!nameRange.isPresent())
				continue;
			String name = fa.getNameAsString();
			Expression scope = fa.getScope();
			if (scope != null && scope.toString().equals("this")) {
				Optional<MDec> fdec = getFieldDecByNameInHierarchy(decClass, name);
				if (fdec.isPresent()) {
					getMemberRanges(fdec.get()).add(nameRange.get());
				}
			} else if (scope != null) {
				Optional<CDec> scopeHost = getDecFromScope(scope);
				if (scopeHost.isPresent()) {
					CDec cdec = scopeHost.get();
					Optional<MDec> mdec = getFieldDecByNameInHierarchy(cdec, name);
					if (mdec.isPresent()) {
						getMemberRanges(mdec.get()).add(nameRange.get());
					}
				}
			}
		}
		// Mark method calls like "this.myMethod()"
		List<MethodCallExpr> methodRefs = cu.findAll(MethodCallExpr.class);
		for (MethodCallExpr mc : methodRefs) {
			Optional<Range> nameRange = mc.getName().getRange();
			if (!nameRange.isPresent())
				continue;
			String name = mc.getNameAsString();
			Expression scope = mc.getScope().isPresent() ? mc.getScope().get() : null;
			if (scope != null && scope.toString().equals("this")) {
				Optional<MDec> mdec = getMDecByNameAndArgs(decClass, name, mc.getArguments());
				if (mdec.isPresent()) {
					getMemberRanges(mdec.get()).add(nameRange.get());
				}
			} else if (scope != null) {
				Optional<CDec> scopeHost = getDecFromScope(scope);
				if (scopeHost.isPresent()) {
					CDec cdec = scopeHost.get();
					Optional<MDec> mdec = getMDecByNameAndArgs(cdec, name, mc.getArguments());
					if (mdec.isPresent()) {
						getMemberRanges(mdec.get()).add(nameRange.get());
					}
				}
			}
		}
	}

	private void markOtherRanges() {
		// TODO: range marking - other
		// field assignment: "myField = ..." AssignExpr
		// casts: "(MyClass) object"
		// imports:
		// catch exception type: CatchClause
		//
		// Add ranges for generic name expressions.
		// - Includes class names
		// - Includes field names
		List<NameExpr> names = cu.findAll(NameExpr.class);
		for (NameExpr clazz : names) {
			SimpleName name = clazz.getName();
			// Never event attempt to lookup 'this'
			if (name.toString().equals("this")) {
				continue;
			}
			if (name.getRange().isPresent()) {
				Range range = name.getRange().get();
				CDec cdec = fetchClassDec(name.asString(), true);
				if (cdec != null) {
					getClassRanges(cdec).add(range);
					continue;
				}
				Optional<MDec> mdec = decClass.getMembers().stream().filter(md -> {
					return md.isField() && md.hasMappings() && name.toString().equals(md.map().getCurrentName());
				}).findFirst();
				if (mdec.isPresent()) {
					getMemberRanges(mdec.get()).add(range);
				}
			}
		}
	}

	/**
	 * @param scope
	 *            Some JavaParser expression.
	 * @return The CDec represented by the scope, if one exists.
	 */
	public Optional<CDec> getDecFromScope(Expression scope) {
		if (scope.toString().equals("this")) {
			return Optional.of(decClass);
		}
		if (scope instanceof NameExpr) {
			CDec cdec = fetchClassDec(scope.toString(), true);
			if (cdec != null) {
				return Optional.of(cdec);
			}
			// Check for variable-references
			Optional<VDec> vOpt = getVariableFromContext(scope, scope.toString());
			if (!vOpt.isPresent()) {
				// No variable by name.
				return Optional.empty();
			}
			CMap varTypeMap = vOpt.get().map();
			if (varTypeMap == null) {
				// No mappings for scope type.
				return Optional.empty();
			}
			CDec varTypeDec = quantifiedToDec.get(varTypeMap.getCurrentName());
			return Optional.ofNullable(varTypeDec);
		} else if (scope instanceof ObjectCreationExpr) {
			// new MyClass() : methodName
			ObjectCreationExpr scopeCreate = (ObjectCreationExpr) scope;
			if (!scopeCreate.getScope().isPresent()) {
				// No scope on creation, so it will be "new Type()"
				return Optional.ofNullable(fetchClassDec(scopeCreate.getTypeAsString(), true));
			}
		} else if (scope instanceof FieldAccessExpr) {
			// this.myField : methodName
			// System.out : println
			FieldAccessExpr scopeField = (FieldAccessExpr) scope;
			Optional<CDec> context = getDecFromScope(scopeField.getScope());
			if (context.isPresent()) {
				CDec contextDex = context.get();
				Optional<MDec> memberOpt = getFieldDecByNameInHierarchy(contextDex, scopeField.getNameAsString());
				// Get internal type of the variable, that will be our class declaration to
				// return
				if (memberOpt.isPresent()) {
					MDec md = memberOpt.get();
					return Optional.ofNullable(quantifiedToDec.get(md.getInternalType()));
				}
			}
		} else if (scope instanceof MethodCallExpr) {
			// MyClass.methodName() : print
			// myVar.methodName() : print
			// new MyClass().methodName() : print
			MethodCallExpr scopeMethod = (MethodCallExpr) scope;
			if (scopeMethod.getScope().isPresent()) {
				Optional<CDec> context = getDecFromScope(scopeMethod.getScope().get());
				if (context.isPresent()) {
					CDec contextDex = context.get();
					Optional<MDec> memberOpt = getMDecByNameAndArgs(contextDex, scopeMethod.getNameAsString(), scopeMethod.getArguments());
					// Get internal/return type of the method, that will be our class declaration to
					// return
					if (memberOpt.isPresent()) {
						MDec md = memberOpt.get();
						return Optional.ofNullable(quantifiedToDec.get(md.getInternalType()));
					}
				}
			} else {
				Logging.error("Could not resolve cdec for method-call, no context present: " + scope);
			}
		}
		return Optional.empty();
	}

	/**
	 * @param nodeInMethod
	 *            Node in method that contains the variable.
	 * @param varExpr
	 *            The variable name as an expression.
	 * @return Variable in the method.
	 */
	private Optional<VDec> getVariableFromContext(Node nodeInMethod, String varName) {
		Optional<MethodDeclaration> mdOpt = nodeInMethod.findParent(MethodDeclaration.class);
		if (mdOpt.isPresent()) {
			MethodDeclaration md = mdOpt.get();
			String mdName = md.getNameAsString();
			String mdDesc = getMethodDesc(md);
			// MDec member = decClass.getMember(mdName, mdDesc);
			Optional<MDec> member = getMDecByNameAndDescInHierarchy(decClass, mdName, mdDesc);
			if (member.isPresent()) {
				return member.get().getVariableByName(varName);
			}
			// Failed to find method
			return Optional.empty();
		}
		return Optional.empty();
	}
	
	/**
	 * @param dec
	 *            Host declared class.
	 * @param name
	 *            Name of the method we want to return.
	 * @param desc
	 *            Descriptor of the method we want to return.
	 * @return Method declaration by the given name.
	 */
	private Optional<MDec> getFieldDecByNameInHierarchy(CDec dec, String name) {
		Optional<MDec> val = getFieldDecByName(dec, name);
		while (!val.isPresent()) {
			// Get parent CDec 
			CVert v = Hierarchy.INSTANCE.getVertex(dec.map().getOriginalName());
			if (v == null)
				break;
			v = v.getSuper();
			dec = quantifiedToDec.get(v.getName());
			if (dec == null) 
				break;
			// Research in parent class
			val = getFieldDecByName(dec, name);
		}
		return val;
	}

	/**
	 * @param dec
	 *            Host declared class.
	 * @param name
	 *            Name of the field we want to return.
	 * @return Field declaration by the given name.
	 */
	private Optional<MDec> getFieldDecByName(CDec dec, String name) {
		if (dec.equals(decClass)) {
			return dec.getMembers().stream().filter(md -> {
				return md.isField() && md.hasMappings() && name.equals(md.map().getCurrentName());
			}).findFirst();
		} else {
			if (dec.hasMappings()) {
				CMap map = dec.map();
				Optional<MMap> mappedMember = map.getMembers().stream().filter(mm -> mm.isField() && mm.getCurrentName().equals(name)).findFirst();
				if (mappedMember.isPresent()) {
					return Optional.of(MDec.fromMember(dec, name, mappedMember.get().getCurrentDesc()));
				}
			}
			return Optional.empty();
		}
	}

	/**
	 * @param dec
	 *            Host declared class.
	 * @param name
	 *            Name of the method we want to return.
	 * @param args
	 *            Arguments for the method.
	 * @return Method declaration by the given name.
	 */
	private Optional<MDec> getMDecByNameAndArgs(CDec dec, String name, NodeList<Expression> args) {
		if (dec.equals(decClass)) {
			return dec.getMembers().stream().filter(md -> {
				return md.isMethod() && md.hasMappings() && name.equals(md.map().getCurrentName()) && argCheck(args, md.getDesc());
			}).findFirst();
		} else {
			if (dec.hasMappings()) {
				CMap map = dec.map();
				Optional<MMap> mappedMember = map.getMembers().stream()
						.filter(mm -> mm.isMethod() && mm.getCurrentName().equals(name) && argCheck(args, mm.getCurrentDesc())).findFirst();
				if (mappedMember.isPresent()) {
					return Optional.of(MDec.fromMember(dec, name, mappedMember.get().getCurrentDesc()));
				}
			}
			return Optional.empty();
		}
	}
	
	/**
	 * @param dec
	 *            Host declared class.
	 * @param name
	 *            Name of the method we want to return.
	 * @param desc
	 *            Descriptor of the method we want to return.
	 * @return Method declaration by the given name.
	 */
	private Optional<MDec> getMDecByNameAndDescInHierarchy(CDec dec, String name, String desc) {
		Optional<MDec> val = getMDecByNameAndDesc(dec, name, desc);
		while (!val.isPresent()) {
			// Get parent CDec 
			CVert v = Hierarchy.INSTANCE.getVertex(dec.map().getOriginalName());
			if (v == null)
				break;
			v = v.getSuper();
			dec = quantifiedToDec.get(v.getName());
			if (dec == null) 
				break;
			// Research in parent class
			val = getMDecByNameAndDesc(dec, name, desc);
		}
		return val;
	}
	
	/**
	 * @param dec
	 *            Host declared class.
	 * @param name
	 *            Name of the method we want to return.
	 * @param desc
	 *            Descriptor of the method we want to return.
	 * @return Method declaration by the given name.
	 */
	private Optional<MDec> getMDecByNameAndDesc(CDec dec, String name, String desc) {
		if (dec.equals(decClass)) {
			return dec.getMembers().stream().filter(md -> {
				return md.hasMappings() && name.equals(md.map().getCurrentName()) && desc.equals(md.getDesc());
			}).findFirst();
		} else {
			if (dec.hasMappings()) {
				CMap map = dec.map();
				Optional<MMap> mappedMember = map.getMembers().stream()
						.filter(mm -> mm.getCurrentName().equals(name) && desc.equals(mm.getCurrentDesc())).findFirst();
				if (mappedMember.isPresent()) {
					return Optional.of(MDec.fromMember(dec, name, mappedMember.get().getCurrentDesc()));
				}
			}
			return Optional.empty();
		}
	}

	private boolean argCheck(NodeList<Expression> args, String desc) {
		// TODO: To proper type checking of arguments
		// This will combat aggressive overloading
		return args.size() == org.objectweb.asm.Type.getArgumentTypes(desc).length;
	}

	/**
	 * Add members to the {@link #decClass current class} to be used for range
	 * population in {@link #markMemberRanges()}.
	 */
	private void populateMembers() {
		List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
		for (FieldDeclaration fd : fields) {
			String name = fd.getVariable(0).getNameAsString();
			String desc = getDescriptor(fd.getCommonType());
			if (desc != null) {
				decClass.addMember(MDec.fromMember(decClass, name, desc));
			}
		}
		List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
		for (MethodDeclaration md : methods) {
			String name = md.getNameAsString();
			String desc = getMethodDesc(md);
			if (desc != null) {
				MDec mdec = MDec.fromMember(decClass, name, desc);
				decClass.addMember(mdec);
				List<VariableDeclarationExpr> variables = md.findAll(VariableDeclarationExpr.class);
				for (VariableDeclarationExpr vars : variables) {
					// I think CFR will split multi-var-per-line into separate variables.
					String varName = vars.getVariable(0).getNameAsString();
					String varDesc = getDescriptor(vars.getCommonType());
					if (varName != null && varDesc != null) {
						mdec.addVariable(VDec.fromVariable(mdec, varName, varDesc));
					}
				}
				for (Parameter param : md.getParameters()) {
					String varName = param.getNameAsString();
					String varDesc = getDescriptor(param.getType());
					if (varName != null && varDesc != null) {
						mdec.addVariable(VDec.fromVariable(mdec, varName, varDesc));
					}
				}
			}
		}
	}

	/**
	 * Populate {@link #packageToClasses} and {@link #simpleToQuantified} so that
	 * their wrapper methods can be used later.
	 */
	private void populateLookups() {
		// add self
		getClassesInPackage(decClass.getPackage()).add(decClass);
		getNameLookup(decClass.getSimpleName()).add(decClass);
		quantifiedToDec.put(decClass.getFullName(), decClass);
		// read classes from code imports
		List<ImportDeclaration> imports = cu.findAll(ImportDeclaration.class);
		for (ImportDeclaration imp : imports) {
			CDec dec = CDec.fromClass(imp.getNameAsString());
			// add to import lookup
			getClassesInPackage(dec.getPackage()).add(dec);
			getNameLookup(dec.getSimpleName()).add(dec);
			quantifiedToDec.put(dec.getFullName(), dec);
		}
		// read classes from input, based on package
		Optional<PackageDeclaration> optPack = cu.findFirst(PackageDeclaration.class);
		if (optPack.isPresent()) {
			// specified package
			String pack = optPack.get().getNameAsString().replace(".", "/");
			input.names().forEach(name -> {
				CDec dec = CDec.fromClass(name);
				if (dec.getPackage().equals(pack)) {
					getClassesInPackage(dec.getPackage()).add(dec);
					getNameLookup(dec.getSimpleName()).add(dec);
					quantifiedToDec.put(dec.getFullName(), dec);
				}
			});
		} else {
			// default package
			input.names().forEach(name -> {
				CDec dec = CDec.fromClass(name);
				if (dec.isDefaultPackage()) {
					getClassesInPackage(dec.getPackage()).add(dec);
					getNameLookup(dec.getSimpleName()).add(dec);
					quantifiedToDec.put(dec.getFullName(), dec);
				}
			});
		}
	}

	/**
	 * @param pack
	 *            Package in internal format.
	 * @return Set of all classes in the given package.
	 */
	private Set<CDec> getClassesInPackage(String pack) {
		if (pack == null) {
			throw new RuntimeException("Requested classes from a package, but gave 'null'");
		}
		// Get or create list.
		Set<CDec> classes = packageToClasses.get(pack);
		if (classes == null) {
			packageToClasses.put(pack, classes = new HashSet<>());
		}
		return classes;
	}

	/**
	 * @param dec
	 *            Specified class.
	 * @return Set of all ranges for the specified class.
	 */
	private Set<Range> getClassRanges(CDec dec) {
		// Get or create list.
		Set<Range> ranges = classRanges.get(dec);
		if (ranges == null) {
			classRanges.put(dec, ranges = new HashSet<>());
		}
		return ranges;
	}

	/**
	 * @param dec
	 *            Specified member.
	 * @return Set of all ranges for the specified member.
	 */
	private Set<Range> getMemberRanges(MDec dec) {
		// Get or create list.
		Set<Range> ranges = memberRanges.get(dec);
		if (ranges == null) {
			memberRanges.put(dec, ranges = new HashSet<>());
		}
		return ranges;
	}

	/**
	 * 
	 * @param simple
	 *            Simple name of the class <i>(The name without the package)</i>.
	 * @return Set of all potential quantified classes. Ideally the set will be only
	 *         one element.
	 */
	private Set<CDec> getNameLookup(String simple) {
		if (simple == null) {
			throw new RuntimeException("Requested name lookup, but gave 'null'");
		}
		// Get or create list.
		Set<CDec> classes = simpleToQuantified.get(simple);
		if (classes == null) {
			simpleToQuantified.put(simple, classes = new HashSet<>());
		}
		return classes;
	}

	/**
	 * @param md
	 *            JavaParser method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	private String getMethodDesc(MethodDeclaration md) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the method parameters for the descriptor
		NodeList<Parameter> params = md.getParameters();
		for (Parameter param : params) {
			Type pType = param.getType();
			String pDesc = getDescriptor(pType);
			if (pDesc == null) {
				return null;
			}
			sbDesc.append(pDesc);
		}
		// Append the return type for the descriptor
		Type typeRet = md.getType();
		String retDesc = getDescriptor(typeRet);
		if (retDesc == null) {
			return null;
		}
		sbDesc.append(")");
		sbDesc.append(retDesc);
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type.
	 * @return Internal descriptor from type, assuming the type is available through
	 *         {@link #getNameLookup(String)} or if it is a primitive or void type.
	 */
	private String getDescriptor(Type type) {
		if (type.isArrayType())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return isPrim(type) ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available through
	 *         {@link #getNameLookup(String)}.
	 */
	private String typeToDesc(Type type) {
		CDec dec = fetchClassDec(type);
		if (dec == null) {
			return null;
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++) {
			sbDesc.append("[");
		}
		sbDesc.append("L");
		sbDesc.append(dec.getFullName());
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser name.
	 * @return CDec from specified type, assuming the type is available through
	 *         {@link #getNameLookup(String)}.
	 */
	private CDec fetchClassDec(SimpleName name) {
		return fetchClassDec(name.asString(), false);
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return CDec from specified type, assuming the type is available through
	 *         {@link #getNameLookup(String)}.
	 */
	private CDec fetchClassDec(Type type) {
		return fetchClassDec(type.asString(), false);
	}

	/**
	 * @param type
	 *            Class name.
	 * @param silent
	 *            If lookup fails, log the reason if silent is {@code false}.
	 * @return CDec from specified type, assuming the type is available through
	 *         {@link #getNameLookup(String)}.
	 */
	private CDec fetchClassDec(String key, boolean silent) {
		if (key.contains("<")) {
			key = key.substring(0, key.indexOf("<"));
		}
		if (key.contains("[")) {
			key = key.substring(0, key.indexOf("["));
		}
		Set<CDec> set = getNameLookup(key);
		if (set.size() > 1) {
			// more than one match...
			if (!silent)
				Logging.error("Multiple simple->quantified for '" + key + "'");
		} else if (set.size() > 0) {
			// only one result
			return set.iterator().next();
		} else {
			if (!silent)
				Logging.error("Could not find simple->quantified for '" + key + "'");
		}
		return null;
	}

	/**
	 * @param line
	 *            Caret line in editor.
	 * @param column
	 *            Caret column in editor.
	 * @return CDec at position. May be {@code null}.
	 */
	public CDec getClassFromPosition(int line, int column) {
		for (Entry<CDec, Set<Range>> e : classRanges.entrySet()) {
			for (Range range : e.getValue()) {
				if (isInRange(range, line, column)) {
					return e.getKey();
				}
			}
		}
		return null;
	}

	/**
	 * @param line
	 *            Caret line in editor.
	 * @param column
	 *            Caret column in editor.
	 * @return MDec at position. May be {@code null}.
	 */
	public MDec getMemberFromPosition(int line, int column) {
		for (Entry<MDec, Set<Range>> e : memberRanges.entrySet()) {
			for (Range range : e.getValue()) {
				if (isInRange(range, line, column)) {
					return e.getKey();
				}
			}
		}
		return null;
	}

	/**
	 * @param range
	 *            Range to check bounds.
	 * @param line
	 *            Caret line in editor.
	 * @param column
	 *            Caret column in editor.
	 * @return {@code true} if caret position is within the range.
	 */
	private boolean isInRange(Range range, int line, int column) {
		if (range.begin.line != range.end.line)
			throw new RuntimeException("Invalid range: " + range);
		return line == range.begin.line && column >= range.begin.column && column <= (range.end.column + 1);
	}

	/**
	 * @return Map of class declarations, to their ranges in the editor.
	 */
	public Map<CDec, Set<Range>> getClassRanges() {
		return classRanges;
	}

	/**
	 * @return Map of member declarations, to their ranges in the editor.
	 */
	public Map<MDec, Set<Range>> getMemberRanges() {
		return memberRanges;
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(Type type) {
		String desc = null;
		switch (type.asString()) {
		case "boolean":
			desc = "Z";
			break;
		case "int":
			desc = "I";
			break;
		case "long":
			desc = "J";
			break;
		case "short":
			desc = "S";
			break;
		case "byte":
			desc = "B";
			break;
		case "double":
			desc = "D";
			break;
		case "float":
			desc = "F";
			break;
		case "void":
			desc = "V";
			break;
		default:
			throw new RuntimeException("Unknown primitive type field '" + type.asString() + "'");
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++) {
			sbDesc.append("[");
		}
		sbDesc.append(desc);
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type.
	 * @return {@code true} if the type denotes a primitive or void type.
	 */
	private static boolean isPrim(Type type) {
		// void is not a primitive, but lets just pretend it is.
		return type.isVoidType() || type.isPrimitiveType();
	}
}
