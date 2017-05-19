package me.coley.jremapper.parse;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;

public class IndexableStringReader extends StringReader {
	private Field next;
	private Field length;

	/**
	 * Create the indexable reader by making the index fields of the parent
	 * class accessible.
	 * 
	 * @param text
	 *            Text to read.
	 */
	public IndexableStringReader(String text) {
		super(text);
		try {
			next = StringReader.class.getDeclaredField("next");
			next.setAccessible(true);
			length = StringReader.class.getDeclaredField("length");
			length.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {}
	}

	/**
	 * Find the next word in the text by passing over delimiter characters.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String nextWord() throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean isQuote = false;
		while (true) {
			char c = (char) this.read();
			// TODO: Verify this reads quotes as whole words.
			// Context system doesn't yet handle method bodies, but eventually
			// it should.
			if (isDelimiter(c) && !isQuote) {
				break;
			} else if (c == '"') {
				isQuote = !isQuote;
			} else {
				sb.append(c);
			}
		}
		String value = sb.toString();
		if (value == null || value.length() == 0) {
			return nextWord();
		}
		return value;
	}

	/**
	 * Repeated calls to {@link #nextWord()} to skip over words.
	 * 
	 * @param count
	 *            Number of words to skip.
	 * @throws IOException
	 */
	public void skipWords(int count) throws IOException {
		for (int i = 0; i < count; i++) {
			nextWord();
		}
	}

	private boolean isDelimiter(char c) {
		return c == ' ' || c == '\n';
	}

	/**
	 * Returns true if the current index is not equal to the end of the text.
	 * False if the index is at the end.
	 * 
	 * @return
	 */
	public boolean hasMore() {
		return getLength() > getIndex();
	}

	public int getLength() {
		try {
			return length == null ? 0 : length.getInt(this);
		} catch (Exception e) {
			return 0;
		}
	}

	public int getIndex() {
		try {
			return next == null ? 0 : next.getInt(this);
		} catch (Exception e) {
			return 0;
		}
	}
}
