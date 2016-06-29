package de.d3web.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import org.jetbrains.annotations.Nullable;

/**
 * Utility class for stream handling
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 09.09.2013
 */
public class Streams {

	/**
	 * Streams the specified inputStream to the specified outputStream and
	 * returns after the stream has completely been written. Before the method
	 * returns, both stream will be closed.
	 *
	 * @param inputStream  the source stream to read the data from
	 * @param outputStream the target stream to write the data to
	 * @throws IOException if any of the streams has an error
	 * @created 09.09.2013
	 */
	public static void streamAndClose(InputStream inputStream, OutputStream outputStream) throws IOException {
		try {
			Streams.stream(inputStream, outputStream);
		}
		finally {
			closeQuietly(inputStream);
			closeQuietly(outputStream);
		}
	}

	/**
	 * Closes the closable and logs away exception that might be thrown. Use this inside finally blocks.
	 *
	 * @param closeable the closable to be closed
	 */
	public static void closeQuietly(@Nullable Closeable closeable) {
		if (closeable == null) return;
		try {
			closeable.close();
		}
		catch (Exception e) {
			Log.severe("Exception during close()", e);
		}
	}

	/**
	 * Streams the specified inputStream to the specified outputStream and
	 * returns after the stream has completely been written.
	 *
	 * @param inputStream  the source stream to read the data from
	 * @param outputStream the target stream to write the data to
	 * @throws IOException if any of the streams has an error
	 * @created 09.09.2013
	 */
	public static void stream(InputStream inputStream, OutputStream outputStream) throws IOException {
		stream(inputStream, outputStream, 1024);
	}

	/**
	 * Streams the specified inputStream to the specified outputStream and
	 * returns after the stream has completely been written.
	 *
	 * @param inputStream  the source stream to read the data from
	 * @param outputStream the target stream to write the data to
	 * @param chunkSize    the size of the particular chunks to be copied
	 * @throws IOException if any of the streams has an error
	 * @created 09.09.2013
	 */
	public static void stream(InputStream inputStream, OutputStream outputStream, int chunkSize) throws IOException {
		byte[] buffer = new byte[chunkSize];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Creates a asynchronous streaming task from the specified source
	 * {@link InputStream} to the specified target {@link OutputStream}.
	 *
	 * @param inputStream  the source stream
	 * @param outputStream the target stream
	 * @created 27.04.2011
	 */
	public static void streamAsync(InputStream inputStream, OutputStream outputStream) {
		final InputStream in = inputStream;
		final OutputStream out = outputStream;
		Thread thread = new Thread("asynchronous streaming task") {

			@Override
			public void run() {
				try {
					stream(in, out);
				}
				catch (IOException e) {
					throw new IllegalStateException("unexpected error while piping streams", e);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Returns the contents of the specified input stream as a byte[].
	 *
	 * @param input the input stream to read from
	 * @return the content of the stream
	 * @throws IOException if the specified streams cannot be read completely
	 * @created 01.10.2013
	 */
	public static byte[] getBytes(InputStream input) throws IOException {
		ByteArrayOutputStream result = getContent(input);
		return result.toByteArray();
	}

	/**
	 * Returns the contents of the specified input stream as a byte[]. The
	 * method closes the specified stream before it returns the contents.
	 *
	 * @param input the input stream to read from
	 * @return the content of the stream
	 * @throws IOException if the specified streams cannot be read completely
	 * @created 01.10.2013
	 */
	public static byte[] getBytesAndClose(InputStream input) throws IOException {
		try {
			return getBytes(input);
		}
		finally {
			closeQuietly(input);
		}
	}

	/**
	 * Returns the contents of the specified input stream as a String. The stream is not closed!
	 *
	 * @param input the input stream to read from
	 * @return the content of the stream
	 * @created 01.10.2013
	 */
	public static String getText(InputStream input) {
		return readStream(input);
	}

	/**
	 * Reads the contents of a stream into a String and return the string. The InputStream is not
	 * closed!
	 *
	 * @param inputStream the stream to load from
	 * @return the contents of the file
	 * @created 20.06.2015
	 */
	public static String readStream(InputStream inputStream) {
		// The reason it works is because Scanner iterates over tokens in the stream,
		// and in this case we separate tokens using "beginning of the input boundary"
		// (\A) thus giving us only one token for the entire contents of the stream.
		Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}

	/**
	 * Returns the contents of the specified input stream as a String. The
	 * method closes the specified stream before it returns the contents.
	 *
	 * @param input the input stream to read from
	 * @return the content of the stream
	 * @throws IOException if the specified streams cannot be read completely
	 * @created 01.10.2013
	 */
	public static String getTextAndClose(InputStream input) throws IOException {
		try {
			return getText(input);
		}
		finally {
			closeQuietly(input);
		}
	}

	private static ByteArrayOutputStream getContent(InputStream input) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream(input.available());
		stream(input, result);
		result.close();
		return result;
	}

}
