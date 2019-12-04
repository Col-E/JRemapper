package me.coley.jremapper.ui;

import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import jregex.Matcher;
import jregex.Pattern;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret.CaretVisibility;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import me.coley.event.Bus;
import me.coley.jremapper.asm.Input;
import me.coley.jremapper.event.OpenCodeEvent;
import me.coley.jremapper.mapping.Mappings;
import me.coley.jremapper.parse.*;
import me.coley.jremapper.util.*;

/**
 * Code pane with search bar. Syntax-highlighting powered by regex.
 * 
 * @author Matt
 */
public class CodePane extends BorderPane {
	private static final String[] KEYWORDS = new String[] { "abstract", "assert", "boolean", "break", "byte", "case",
			"catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
			"final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
			"long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
			"strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
			"volatile", "while" };
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String CONST_HEX_PATTERN = "(0[xX][0-9a-fA-F]+)+";
	private static final String CONST_VAL_PATTERN = "(\\b([\\d._]*[\\d])\\b)+|(true|false|null)";
	private static final String CONST_PATTERN = CONST_HEX_PATTERN + "|" + CONST_VAL_PATTERN;
	private static final String COMMENT_SINGLE_PATTERN = "//[^\n]*";
	private static final String COMMENT_MULTI_SINGLE_PATTERN = "/[*](.|\n|\r)+?\\*/";
	private static final String COMMENT_MULTI_JAVADOC_PATTERN = "/[*]{2}(.|\n|\r)+?\\*/";
	private static final String ANNOTATION_PATTERN = "\\B(@[\\w]+)\\b";
	private static final Pattern PATTERN = new Pattern(
			 "({COMMENTDOC}" + COMMENT_MULTI_JAVADOC_PATTERN + ")" + 
			"|({COMMENTMULTI}" + COMMENT_MULTI_SINGLE_PATTERN + ")" + 
			"|({COMMENTLINE}" + COMMENT_SINGLE_PATTERN + ")" + 
			"|({KEYWORD}" + KEYWORD_PATTERN + ")" + 
			"|({STRING}" + STRING_PATTERN + ")" + 
			"|({ANNOTATION}" + ANNOTATION_PATTERN + ")" + 
			"|({CONSTPATTERN}" + CONST_PATTERN + ")");
	private final CodeArea code = new CodeArea();
	private final HiddenSidesPane pane = new HiddenSidesPane();
	private final CustomTextField search = new CustomTextField();
	private final GridPane info = new GridPane();
	/**
	 * The jar input wrapper.
	 */
	private final Input input;
	/**
	 * The quantified name of the file opened by this pane.
	 */
	private final String path;
	/**
	 * The pane that was open before this one.
	 */
	private final CodePane previous;
	/**
	 * The unit sent to the {@link #regions RegionMapper}.
	 */
	private CompilationUnit cu;
	/**
	 * Utility for mapping regions of text to mappings.
	 */
	private RegionMapper regions;
	/**
	 * This is a dumb hack to prevent unnecessary parsing.
	 */
	private int pass;
	/**
	 * Stored position so the caret can go back to where it was <i>(Roughly)</i>
	 * before a reload occurred.
	 */
	private Position pos;
	/**
	 * Selected declaration item.
	 */
	private AbstractDec<?> selectedDec;

	private CodePane(Input input, String path) {
		this.input = input;
		this.path = path;
		this.previous = input.history.pop();
		String decompile = decompile();
		setupCode(decompile);
		setupSearch();
		setupTheRest();
	}

	public static CodePane open(Input input, String currentPath) {
		return new CodePane(input, currentPath);
	}

	/**
	 * Set the properties of the {@link #code CodeArea}.
	 * 
	 * @param decompile
	 *            Decompiled java code.
	 */
	private void setupCode(String decompile) {
		// Only allow navigation.
		code.setEditable(false);
		code.setShowCaret(CaretVisibility.ON);
		// Regenerate styles when the text is updated.
		code.richChanges().filter(ch -> !ch.getInserted().equals(ch.getRemoved())).subscribe(change -> {
			updateStyleAndRegions();
		});
		// Update selected classes/members when the caret moves.
		code.caretPositionProperty().addListener((obs, old, cur) -> {
			if (regions == null) {
				return;
			}
			Position pos = code.offsetToPosition(cur, Bias.Backward);
			int line = pos.getMajor() + 1;
			int column = pos.getMinor() + 1;
			CDec c = regions.getClassFromPosition(line, column);
			if (c != null) {
				this.pos = pos;
				updateSelection(c);
				return;
			}
			MDec m = regions.getMemberFromPosition(line, column);
			if (m != null) {
				this.pos = pos;
				updateSelection(m);
				return;
			}
			resetSelection();
		});
		// Setup code-lines.
		code.setParagraphGraphicFactory(LineNumberFactory.get(code));
		// Setup keybind operations.
		final CodePane cp = this;
		KeyCombination bindOpenDec = new KeyCodeCombination(KeyCode.N);
		KeyCombination bindGoBack = new KeyCodeCombination(KeyCode.BACK_SPACE);
		code.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (bindOpenDec.match(event)) {
					// jump to declaration selected
					if (selectedDec instanceof CDec) {
						CDec dec = (CDec) selectedDec;
						CodePane pane = input.history.push(open(input, dec.getFullName()));
						Bus.post(new OpenCodeEvent(pane));
					} else if (selectedDec instanceof MDec) {
						CodePane pane = cp;
						MDec dec = (MDec) selectedDec;
						if (!dec.getOwner().equals(regions.getHost())) {
							pane = input.history.push(open(input, dec.getOwner().getFullName()));
							Bus.post(new OpenCodeEvent(pane));
						}
						try {
							pane.selectMember(dec);
						} catch (Exception e) {}

					}
				} else if (bindGoBack.match(event)) {
					// Go back to last open pane
					if (previous != null) {
						Bus.post(new OpenCodeEvent(previous));
					}
				}
			}
		});
		refreshCode();
	}

	/**
	 * Setup the search-bar.
	 */
	private void setupSearch() {
		// Main panel, has hidden bottom-node for search bar.
		search.setLeft(new ImageView(Icons.FIND));
		search.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (KeyCode.ESCAPE == e.getCode()) {
				pane.setPinnedSide(null);
			} else if (KeyCode.ENTER == e.getCode()) {
				int caret = code.getCaretPosition();
				String codeText = code.getText();
				int index = codeText.indexOf(search.getText(), caret + 1);
				if (index == -1) {
					// not found after caret, so search unbound (wrap around)
					index = codeText.indexOf(search.getText(), 0);
				}
				// set caret to index
				if (index >= 0) {
					code.selectRange(index, index + search.getText().length());
					code.requestFollowCaret();
				} else {
					Toolkit.getDefaultToolkit().beep();
				}
			}
		});
		// Show search bar
		addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (e.isControlDown() && KeyCode.F == e.getCode()) {
				if (pane.getPinnedSide() == null) {
					pane.setPinnedSide(Side.BOTTOM);
					// "search.requestFocus()" doesnt work because of the rules
					// limiting focus.
					// So we have to wait a bit for it to be focusable.... which
					// is really ugly.
					uglyFocus(search);
				} else {
					pane.setPinnedSide(null);
				}
			}
		});
		// Place at bottom
		pane.setBottom(search);
	}

	/**
	 * Setup everything else that isn't the code-area.
	 */
	private void setupTheRest() {
		info.setPrefHeight(95);
		info.getStyleClass().add("infopane");
		ColumnConstraints colInfo = new ColumnConstraints();
		ColumnConstraints colEdit = new ColumnConstraints();
		colInfo.setPercentWidth(15);
		colEdit.setPercentWidth(85);
		info.getColumnConstraints().addAll(colInfo, colEdit);
		setTop(info);
		pane.animationDurationProperty().setValue(Duration.millis(50));
		pane.setContent(new VirtualizedScrollPane<>(code));
		setCenter(pane);
	}

	/**
	 * Update the current selected class.
	 * 
	 * @param c
	 *            The newly selected class.
	 */
	private void updateSelection(CDec c) {
		selectedDec = c;
		info.getChildren().clear();
		info.add(new Label("Class name"), 0, 0);
		TextField name = new TextField();
		if (c.hasMappings()) {
			name.setText(c.map().getCurrentName());
		} else {
			name.setText(c.getFullName());
			name.setEditable(false);
		}
		info.add(name, 1, 0);
		name.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (KeyCode.ENTER == e.getCode()) {
				pass = -2;
				c.map().setCurrentName(name.getText());
				refreshCode();
				resetSelection();
				updateStyleAndRegions();
			}
		});
	}

	/**
	 * Update the current selected member.
	 * 
	 * @param m
	 *            The newly selected member.
	 */
	private void updateSelection(MDec m) {
		selectedDec = m;
		CDec c = m.getOwner();
		info.getChildren().clear();
		// Owner
		TextField owner = new TextField();
		if (c.hasMappings()) {
			owner.setText(c.map().getCurrentName());
		} else {
			owner.setText(c.getFullName());
			owner.setDisable(true);
		}
		info.add(new Label("Class name"), 0, 0);
		info.add(owner, 1, 0);
		owner.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (KeyCode.ENTER == e.getCode()) {
				pass = -2;
				c.map().setCurrentName(owner.getText());
				refreshCode();
				resetSelection();
				updateStyleAndRegions();
			}
		});
		// Member
		TextField name = new TextField();
		if (m.hasMappings()) {
			name.setText(m.map().getCurrentName());
		} else {
			// Failed mapping lookup
			name.setText(m.getName());
			name.setDisable(true);
		}
		TextField desc = new TextField(m.getDesc());
		desc.setDisable(true);
		info.add(new Label(m.isMethod() ? "Method name" : "Field name"), 0, 1);
		info.add(name, 1, 1);
		info.add(new Label(m.isMethod() ? "Method desc" : "Field desc"), 0, 2);
		info.add(desc, 1, 2);
		name.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (KeyCode.ENTER == e.getCode()) {
				m.map().setCurrentName(name.getText());
				refreshCode();
				resetSelection();
				updateStyleAndRegions();
			}
		});

	}

	/**
	 * Reset selected class / member.
	 */
	private void resetSelection() {
		selectedDec = null;
		info.getChildren().clear();
	}

	/**
	 * Regenerate styles for the current code-area text.
	 */
	private void updateStyleAndRegions() {
		// This event gets fired twice, this prevents multiple executions.
		// This may look redundant, but if you don't believe me, fiddle with it.
		pass++;
		if (pass == 1) {
			String text = code.getText();
			code.setStyleSpans(0, computeStyle(text));
			setupRegions(text);
			pass = 0;
		}
	}

	/**
	 * Refresh the decompiled code. This will apply mapping changes.
	 */
	public void refreshCode() {
		// The text is set this way so that the change listener can run.
		code.clear();
		code.appendText(decompile());
		// Dont allow undo to remove the initial text.
		code.getUndoManager().forgetHistory();
		Threads.runFx(() -> {
			// set position
			try {
				if (pos != null) {
					int offset = pos.toOffset();
					code.moveTo(offset);
					code.requestFollowCaret();
					return;
				}
			} catch (Exception e) {}
			code.moveTo(0);
			code.requestFollowCaret();
			onShow();
		});
	}

	/**
	 * Move the caret position to the specified member declaration.
	 * 
	 * @param dec
	 */
	protected void selectMember(MDec dec) {
		if (dec.isMethod()) {
			List<MethodDeclaration> decs = cu.findAll(MethodDeclaration.class);
			for (MethodDeclaration md : decs) {
				int line = md.getName().getRange().get().begin.line;
				int column = md.getName().getRange().get().begin.column;
				MDec decMD = regions.getMemberFromPosition(line, column);
				if (decMD != null && dec.toString().equals(decMD.toString())) {
					Threads.runFx(() -> {
						code.moveTo(line - 1, column - 1);
						code.requestFollowCaret();
					});
				}
			}
		} else {
			List<FieldDeclaration> decs = cu.findAll(FieldDeclaration.class);
			for (FieldDeclaration md : decs) {
				int line = md.getVariable(0).getName().getRange().get().begin.line;
				int column = md.getVariable(0).getName().getRange().get().begin.column;
				MDec decMD = regions.getMemberFromPosition(line, column);
				if (decMD != null && dec.toString().equals(decMD.toString())) {
					Threads.runFx(() -> {
						code.moveTo(line - 1, column - 1);
						code.requestFollowCaret();
					});
				}
			}
		}
	}

	public void onShow() {
		code.requestFocus();
	}

	/**
	 * Generate member regions for the given text.
	 * 
	 * @param decompile
	 *            Decompiled java code.
	 */
	private void setupRegions(String decompile) {
		ParserConfiguration configuration = new ParserConfiguration();
		JavaParser parser = new JavaParser(configuration);
		ParseResult<CompilationUnit> result = parser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(decompile));
		if (!result.isSuccessful()) {
			Logging.error("Parse fail!");
			for (Problem problem : result.getProblems()) {
				Logging.error("\t" + problem.toString());
			}
		}
		// Use generated AST to apply mappings to ranges in the code.
		if (result.getResult().isPresent()) {
			Logging.info("Parse returned result!");
			cu = result.getResult().get();
			regions = new RegionMapper(input, path, cu);
		} else {
			regions = null;
		}
	}

	/**
	 * Run decompiler and fetch result.
	 */
	private String decompile() {
		CFRResourceLookup lookupHelper = new CFRResourceLookup();
		Map<String, String> options = CFROpts.toStringMap();
		SinkFactory sink = new SinkFactory();
		// Setup driver
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(new CFRSourceImpl(lookupHelper))
				.withOutputSink(sink)
				.withOptions(options)
				.build();
		// Decompile
		driver.analyse(Collections.singletonList(path));
		String decompilation = sink.getDecompilation();
		if (decompilation.startsWith("/*\n" + " * Decompiled with CFR.\n" + " */"))
			decompilation = decompilation.substring(decompilation.indexOf("*/") + 3);
		// JavaParser does NOT like inline comments like this.
		decompilation = decompilation.replace("/* synthetic */ ", "");
		decompilation = decompilation.replace("/* bridge */ ", "");
		decompilation = decompilation.replace("/* enum */ ", "");
		// and some extra hacky bullshit JavaParser doesn't like
		decompilation = decompilation.replace(".$SwitchMap$", "");
		// <clinit> with modifiers
		decompilation = decompilation.replace("public static {", "static {");
		return decompilation;
	}

	/**
	 * A very ugly way of focusing on some text... Focusing is odd.
	 *
	 * @param textField
	 * 		Textfield to focus on.
	 */
	private void uglyFocus(CustomTextField textField) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Threads.runFx(new Runnable() {
						@Override
						public void run() {
							try {
								textField.requestFocus();
							} catch (Exception e) {}
						}
					});
				} catch (Exception e) {}
			}
		}.start();
	}

	/**
	 * @param text
	 * 		Text to apply styles to.
	 *
	 * @return Stylized regions of the text <i>(via css tags)</i>.
	 */
	private StyleSpans<Collection<String>> computeStyle(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			// @formatter:off
			String styleClass = getStyleClass(matcher);
			// @formatter:on
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	/**
	 * Intended usage follows the pattern:
	 * 
	 * <pre>
	matcher.group("GROUP1")   != null ? "group1"
	: matcher.group("GROUP2") != null ? "group2"
	: matcher.group("GROUP3") != null ? "group3" :null;
	 * </pre>
	 * 
	 * @param matcher
	 *            Regex matcher.
	 * @return CSS class name associated with the discovered group found by the
	 *         matcher.
	 */
	private String getStyleClass(Matcher matcher) {
		//@formatter:off
		return matcher.group("STRING") != null ? "string"
			: matcher.group("KEYWORD") != null ? "keyword"
			: matcher.group("COMMENTDOC") != null ? "comment-javadoc"
			: matcher.group("COMMENTMULTI") != null ? "comment-multi"
			: matcher.group("COMMENTLINE") != null ? "comment-line"
			: matcher.group("CONSTPATTERN") != null ? "const"
			: matcher.group("ANNOTATION") != null ? "annotation" : null;
		//@formatter:on
	}

	/**
	 * Extension of CFR's front-end for looking up resources. Successful lookups
	 * allow more accurate decompilations.
	 * 
	 * @author Matt
	 */
	private static class CFRSourceImpl implements ClassFileSource {
		/**
		 * Lookup assister for inner classes and other references.
		 */
		private final CFRResourceLookup resources;

		private CFRSourceImpl(CFRResourceLookup resources) {
			this.resources = resources;
		}

		@Override
		public void informAnalysisRelativePathDetail(String s, String s1) {}

		@Override
		public Collection<String> addJar(String s) {
			throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
		}

		@Override
		public String getPossiblyRenamedPath(String s) {
			return s;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String pathOrName) throws IOException {
			pathOrName = pathOrName.substring(0, pathOrName.length() - ".class".length());
			return Pair.make(resources.get(pathOrName), pathOrName);
		}
	}

	/**
	 * Lookup helper for CFR since it requests extra data <i>(Other classes)</i> for
	 * more accurate decompilation.
	 * 
	 * @author Matt
	 */
	private class CFRResourceLookup {
		public byte[] get(String path) {
			if (input.hasRawClass(path)) {
				byte[] raw = input.getRawClass(path);
				return Mappings.INSTANCE.intercept(raw);
			} else {
				// Logging.info("Decompile 'get' failed for '" + path + "'");
				return null;
			}
		}
	}
	
	/**
	 * CFR decompile output sink.
	 * 
	 * @author Matt
	 */
	private static class SinkFactory implements OutputSinkFactory {
		private String decompile = "Failed to get CFR output";

		@Override
		public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
			return Arrays.asList(SinkClass.STRING);
		}

		@Override
		public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
			switch (sinkType) {
			case EXCEPTION:
				return sinkable -> {
					Logging.error("CFR: " + sinkable);
				};
			case JAVA:
				return sinkable -> {
					decompile = sinkable.toString();
				};
			case PROGRESS:
				return sinkable -> {
					Logging.info("CFR: " + sinkable);
				};
			default:
				break;
			}
			return ignore -> {};
		}

		public String getDecompilation() {
			return decompile;
		}
	};

	/**
	 * CFR option map.
	 * 
	 * @author Matt
	 *
	 */
	public static class CFROpts {
		public static boolean aexAgg = true;
		public static boolean allowCorrecting = true;
		public static boolean arrayIter = true;
		public static boolean collectionIter = true;
		public static boolean commentMonitors = false;
		public static boolean comments = false;
		public static boolean decodeEnumSwitch = true;
		public static boolean decodeFinally = true;
		public static boolean decodeLambdas = true;
		public static boolean decodeStringSwitch = true;
		public static boolean dumpClasspath = false;
		public static boolean eclipse = true;
		public static boolean elidescala = true;
		public static boolean tryresources = true;
		public static boolean forceCondPropagate = true;
		public static boolean forceExceptionPrune = false;
		public static boolean forceReturningIfs = true;
		public static boolean forceTopSort = true;
		public static boolean forceTopSortAggress = true;
		public static boolean forLoopAggCapture = false;
		public static boolean hideBridgeMethods = false;
		public static boolean hideLangImports = false;
		public static boolean hideLongStrings = false;
		public static boolean hideUTF = true;
		public static boolean innerclasses = false;
		public static boolean skipbatchinnerclasses = true;
		public static boolean j14ClassObj = false;
		public static boolean labelledBlocks = true;
		public static boolean lenient = true;
		public static boolean liftConstructorInit = true;
		public static boolean override = true;
		public static boolean pullCodeCase = false;
		public static boolean recover = true;
		public static boolean recoverTypeClash = true;
		public static boolean recoverTypeHints = true;
		public static int recpass = 0;
		public static boolean removeBadGenerics = true;
		public static boolean removeBoilerPlate = true;
		public static boolean removeDeadMethods = true;
		public static boolean removeInnerClassSynthetics = true;
		public static boolean renameDupMembers = false;
		public static boolean renameEnumIdents = true;
		public static boolean renameIllegalIdents = false;
		public static boolean renameSmallMembers = false;
		public static boolean showInferrable = true;
		public static int showOps = 0;
		public static boolean showVersion = false;
		public static boolean silent = true;
		public static boolean staticInitReturn = false;
		public static boolean stringBuffer = false;
		public static boolean stringBuilder = true;
		public static boolean sugarAsserts = true;
		public static boolean sugarBoxing = true;
		public static boolean sugarEnums = true;
		public static boolean tidyMonitors = true;
		public static boolean useNameTable = true;

		/**
		 * @return &lt;String, String(of boolean)&gt; map of the settings and their
		 *         current status.
		 */
		public static Map<String, String> toStringMap() {
			Map<String, String> options = new HashMap<>();
			for (Field f : Reflect.fields(CFROpts.class)) {
				try {
					String name = f.getName().toLowerCase();
					String text = String.valueOf(f.get(null));
					options.put(name, text);
				} catch (Exception e) {
					Logging.error(e);
				}
			}
			return options;
		}
	}
}