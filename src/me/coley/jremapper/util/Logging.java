package me.coley.jremapper.util;

import java.io.IOException;

import me.coley.logging.*;

/**
 * Simple logging to console and file.
 * 
 * @author Matt
 */
public class Logging {
	private final static Logging INSTANCE = new Logging();
	private final static int TRACE_MAX_LEN = 16;
	private Logger<?> lgConsole, lgFile;
	private int indentSize = 3;

	Logging() {
		try {
			lgConsole = new ConsoleLogger(Level.TRACE);
			lgFile = new FileLogger("jrlog.txt", Level.TRACE);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Print an informational message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public static void info(String message) {
		INSTANCE.log(Level.INFO, message);
	}

	/**
	 * Print an informational message with an indent level pre-pended.
	 * 
	 * @param message
	 *            Message to print.
	 * @param indent
	 *            Level of indentation.
	 */
	public static void info(String message, int indent) {
		String formatted = pad(message, (indent * INSTANCE.indentSize), ' ');
		INSTANCE.log(Level.INFO, formatted);
	}

	/**
	 * Print an error message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public static void error(String message) {
		INSTANCE.log(Level.ERRR, message);
	}

	/**
	 * Print an exception and displays it in the UI.
	 * 
	 * @param exception
	 *            Exception to print.
	 */
	public static void error(Exception exception) {
		Logging.error(Logging.getErrorMessage(exception));
	}

	/**
	 * Print an exception.
	 * 
	 * @param exception
	 *            Exception to print.
	 * @param terminate
	 *            Stop program after printing.
	 */
	private static void error(Exception exception, boolean terminate) {
		Logging.error(getErrorMessage(exception));
		if (terminate) {
			System.exit(0);
		}
	}

	/**
	 * Print an exception and terminate the program.
	 * 
	 * @param exception
	 *            Exception to print.
	 */
	public static void fatal(Exception exception) {
		Logging.error(exception, true);
	}

	/**
	 * Print the message of the given level.
	 * 
	 * @param level
	 *            Detail level.
	 * @param message
	 *            Message to print.
	 */
	private void log(Level level, String message) {
		if (message != null) {
			lgConsole.log(level, message);
			lgFile.log(level, message);
		}
	}

	/**
	 * Set the console's logging level.
	 * 
	 * @param level
	 *            Detail level.
	 */
	public static void setLevelConsole(Level level) {
		Logging.setLevel(INSTANCE.lgConsole, level);
	}

	/**
	 * Set the file's logging level.
	 * 
	 * @param level
	 *            Detail level.
	 */
	public static void setLevelFile(Level level) {
		Logging.setLevel(INSTANCE.lgFile, level);
	}

	/**
	 * Set the logger's level to the given one.
	 * 
	 * @param logger Logger to modify.
	 * @param level Level to set.
	 */
	private static void setLevel(Logger<?> logger, Level level) {
		logger.setLevel(level);
	}

	/**
	 * Pad the given message with the given amount of characters.
	 * 
	 * @param message Base message.
	 * @param padding Level of padding.
	 * @param padChar Pad character.
	 * @return Message with prepended padding.
	 */
	private static String pad(String message, int padding, char padChar) {
		StringBuilder padded = new StringBuilder(message);
		for (int i = 0; i < padding; i++) {
			padded.insert(0, padChar);
		}
		return padded.toString();
	}

	/**
	 * Convert exception to string.
	 * 
	 * @param exception
	 *            Exception to convert.
	 * @return Formatted string containing the type, message, and trace.
	 */
	private static String getErrorMessage(Exception exception) {
		StringBuilder message = new StringBuilder(exception.getClass().getSimpleName() + ": " + exception.getMessage() + "\n");
		int trace = 0;
		for (StackTraceElement element : exception.getStackTrace()) {
			String formatted = pad(element.toString(), INSTANCE.indentSize, ' ');
			message.append(formatted).append("\n");
			trace++;
			if (trace >= TRACE_MAX_LEN) break;
		}
		return message.toString();
	}

}