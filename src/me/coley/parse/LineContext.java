package me.coley.parse;

public enum LineContext {
	//@formatter:off
	PACKAGE,
	IMPORT,
	CLASS_DEC,
	IMPLEMENTS,
	EXTENDS,
	VALUE_DEC,
	METHOD_DEC,
	METHOD_BODY,
	UNKNOWN
}
