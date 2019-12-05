package me.coley.jremapper.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Stream utilities.
 * 
 * @author Matt
 */
public class Streams {
	private final static int BUFF_SIZE = (int) Math.pow(128, 2);

	/**
	 * Reads the bytes from the InputStream into a byte array.
	 *
	 * @param is
	 *            InputStream to read from.
	 * @return byte array representation of the input stream.
	 * @throws IOException
	 *             Thrown if the given input stream cannot be read from.
	 */
	public static byte[] from(InputStream is) throws IOException {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			int r;
			byte[] data = new byte[BUFF_SIZE];
			while ((r = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, r);
			}
			buffer.flush();
			return buffer.toByteArray();
		}
	}

	/**
	 * Read InputStream into a string.
	 *
	 * @param input
	 * 		Stream to read from.
	 *
	 * @return UTF8 string.
	 *
	 * @throws IOException
	 * 		When the stream could not be read.
	 */
	public static String toString(final InputStream input) throws IOException {
		return new String(from(input), StandardCharsets.UTF_8);
	}
}