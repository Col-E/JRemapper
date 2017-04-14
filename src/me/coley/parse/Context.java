package me.coley.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import me.coley.Program;
import me.coley.util.StringUtil;

public class Context {
	private final static List<String> INVALID_CONTENT = Arrays.asList("for ", "try ", "do ", "if ", "catch ", "while ");
	private final static List<String> MODS = Arrays.asList("public", "protected", "private", "static", "volatile", "abstract", "transient");

	private List<LineContext> context;
	private Program callback;

	public Context(Program callback) {
		this.callback = callback;
	}

	public LineContext getContextForLine(int line) {
		return line < context.size() ? context.get(line) : LineContext.UNKNOWN;
	}

	public void parse(String text) {
		// parse lines for context
		Scanner scan = new Scanner(text);
		String current = callback.getCurrentClass().name.getValue();
		String currentSimple = current.substring(current.lastIndexOf("/") + 1);
		context = new ArrayList<>(text.split("\n").length);
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("package")) {
				context.add(LineContext.PACKAGE);
			} else if (line.startsWith("import ")) {
				context.add(LineContext.IMPORT);
			} else if ((line.contains("class ") || line.contains("enum ") || line.contains("interface ")) && line.contains(currentSimple)) {
				context.add(LineContext.CLASS_DEC);
			} else if (line.startsWith("implements ")) {
				context.add(LineContext.IMPLEMENTS);
			} else if (line.startsWith("extends ")) {
				context.add(LineContext.EXTENDS);
			} else if (line.endsWith(";")) {
				// Get the last word in the line (substring if assignment
				// detected)
				if (line.contains(" = ")) {
					// I mean, it's assignment so it's some kind of assignment.
					context.add(LineContext.VALUE_DEC);
				} else {
					// Split trimed line
					String trim = line.substring(0, line.length() - 1).trim();
					String[] decSplit = trim.split(" ");
					// If it only has 1 or no args, just call it unknown.
					if (decSplit.length >= 2) {
						// Get name and type
						String name = decSplit[decSplit.length - 1];
						String type = decSplit[decSplit.length - 2];
						// Check if there are invalid chars in either
						String invalidCharInName = StringUtil.getFirstNonWordChar(name);
						String invalidCharInType = StringUtil.getFirstNonWordChar(type);
						// No invalid chars? Declaration.
						// else, who knows!
						if (invalidCharInName == null && invalidCharInType == null) {
							context.add(LineContext.VALUE_DEC);
						} else {
							// TODO: edge cases
							// private static /* synthetic */ int[] $NAME$
							context.add(LineContext.UNKNOWN);
						}
					} else context.add(LineContext.UNKNOWN);
				}
			} else if (!line.endsWith(";") && line.contains("(") && line.contains(")") && line.endsWith("{")) {
				boolean isBody = false;
				// Detect control flow being false-pos for method declaration
				for (String x : INVALID_CONTENT) {
					if (line.contains(x)) {
						isBody = true;
						break;
					}
				}
				if (isBody) {
					context.add(LineContext.METHOD_BODY);
				} else {
					String sub = line.substring(0, line.indexOf("("));
					sub = sub.substring(sub.lastIndexOf(" ") + 1);
					// Does the substring return an empty list(checked against
					// members)?
					// if so: Method body / not a member
					// else: Detected member
					if (callback.getCurrentClass().getMembersByOriginalName(sub).isEmpty()) {
						context.add(LineContext.METHOD_BODY);
					} else {
						// TODO: See if there are any false positives and re-do
						// this logic if so.
						context.add(LineContext.METHOD_DEC);
					}
				}
			} else {
				context.add(LineContext.UNKNOWN);
			}
		}
		scan.close();
	}
}
