/*
 * Copyright (C) 2022 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for stream handling
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 09.09.2013
 */
public class Streams {
	private static final Logger LOGGER = LoggerFactory.getLogger(Streams.class);

	/**
	 * Streams the specified inputStream to the specified outputStream and returns after the stream has completely been
	 * written. Before the method returns, both stream will be closed.
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
			LOGGER.error("Exception during close()", e);
		}
	}

	/**
	 * Streams the specified inputStream to the specified outputStream and returns after the stream has completely been
	 * written.
	 *
	 * @param inputStream  the source stream to read the data from
	 * @param outputStream the target stream to write the data to
	 * @throws IOException if any of the streams has an error
	 * @created 09.09.2013
	 */
	public static void stream(InputStream inputStream, OutputStream outputStream) throws IOException {
		stream(inputStream, outputStream, 8 * 1024);
	}

	/**
	 * Streams the specified inputStream to the specified outputStream and returns after the stream has completely been
	 * written.
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
	 * Creates a asynchronous streaming task from the specified source {@link InputStream} to the specified target
	 * {@link OutputStream}.
	 *
	 * @param inputStream  the source stream
	 * @param outputStream the target stream
	 * @created 27.04.2011
	 */
	public static void streamAsync(InputStream inputStream, OutputStream outputStream) {
		streamAsync(inputStream, outputStream, null);
	}

	/**
	 * Creates a asynchronous streaming task from the specified source {@link InputStream} to the specified target
	 * {@link OutputStream}.
	 *
	 * @param inputStream  the source stream
	 * @param outputStream the target stream
	 * @param closeUp      code to be executed after stream transfer (also if terminated due to an error)
	 * @created 27.04.2011
	 */
	public static void streamAsync(InputStream inputStream, OutputStream outputStream, Runnable closeUp) {
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
				finally {
					if (closeUp != null) {
						closeUp.run();
					}
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
	 * Returns the contents of the specified input stream as a byte[]. The method closes the specified stream before it
	 * returns the contents.
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
		return getText(input, "UTF-8");
	}

	/**
	 * Returns the contents of the specified input stream as a String. The stream is not closed!
	 *
	 * @param input   the input stream to read from
	 * @param charset the charset to use when reading the stream
	 * @return the content of the stream
	 * @created 13.06.2018
	 */
	public static String getText(InputStream input, String charset) {
		return readStream(input, charset);
	}

	/**
	 * Reads the contents of a stream into a String and return the string it. The InputStream is not closed!
	 *
	 * @param inputStream the stream to load from
	 * @return the contents of the file
	 * @created 20.06.2015
	 */
	public static String readStream(InputStream inputStream) {
		return readStream(inputStream, "UTF-8");
	}

	/**
	 * Reads the contents of a stream into a String and return the string it (using the given charset). The InputStream
	 * is not closed!
	 *
	 * @param inputStream the stream to load from
	 * @param charset     the charset to use when reading the stream
	 * @return the contents of the file
	 * @created 20.06.2015
	 */
	public static String readStream(InputStream inputStream, String charset) {
		// The reason it works is because Scanner iterates over tokens in the stream,
		// and in this case we separate tokens using "beginning of the input boundary"
		// (\A) thus giving us only one token for the entire contents of the stream.
		if (charset == null) charset = "UTF-8";
		Scanner scanner = new Scanner(inputStream, charset).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}

	/**
	 * Returns the contents of the specified input stream as a String. The method closes the specified stream before it
	 * returns the contents.
	 *
	 * @param input the input stream to read from
	 * @return the content of the stream
	 * @created 01.10.2013
	 */
	public static String getTextAndClose(InputStream input) {
		return getTextAndClose(input, "UTF-8");
	}

	/**
	 * Returns the contents of the specified input stream as a String. The method closes the specified stream before it
	 * returns the contents.
	 *
	 * @param input   the input stream to read from
	 * @param charset the charset to use when reading the stream
	 * @return the content of the stream
	 * @created 01.10.2013
	 */
	public static String getTextAndClose(InputStream input, String charset) {
		try {
			return getText(input, charset);
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

	/**
	 * Checks if two streams have the same content. Returns true if both streams are of the same length, and the bytes
	 * of both streams are identical. If the streams are equal, they are fully consumed after this call. Otherwise they
	 * may be partially consumed. The streams are not closed by this method.
	 *
	 * @param in1 the first stream to compare
	 * @param in2 the second stream to compare
	 * @return if both streams had the same content
	 */
	public static boolean hasEqualContent(InputStream in1, InputStream in2) throws IOException {
		return hasEqualContent(in1, in2, 1024);
	}

	/**
	 * Checks if two streams have the same content. Returns true if both streams are of the same length, and the bytes
	 * of both streams are identical. If the streams are equal, they are fully consumed after this call. Otherwise they
	 * may be partially consumed. The streams are not closed by this method.
	 *
	 * @param in1        the first stream to compare
	 * @param in2        the second stream to compare
	 * @param bufferSize the size of the buffer used to compare
	 * @return if both streams had the same content
	 */
	public static boolean hasEqualContent(InputStream in1, InputStream in2, int bufferSize) throws IOException {
		byte[] buffer1 = new byte[bufferSize];
		byte[] buffer2 = new byte[bufferSize];
		while (true) {
			int count1 = in1.readNBytes(buffer1, 0, bufferSize);
			int count2 = in2.readNBytes(buffer2, 0, bufferSize);

			// if the streams are not of the same length, or contains different datam they are NOT equal
			if (count1 != count2) return false;
			if (!Arrays.equals(buffer1, buffer2)) return false;

			// we've reached the end of the streams, both streams has been equal
			if (count1 < bufferSize) return true;
		}
	}

	/**
	 * Returns a {@link Collector} that groups elements of type {@code T} by a key function and maps them into values,
	 * collecting those values into unmodifiable {@link List}s.
	 * <p>
	 * The resulting {@link Map} is unmodifiable: both the map itself and the value-lists cannot be mutated after
	 * collection. This makes it safe to expose directly to external callers without defensive copying.
	 * <p>
	 * Example usage:
	 * <pre>{@code
	 * record Person(String name, String city) {}
	 *
	 * List<Person> people = List.of(
	 *   new Person("Ada", "London"),
	 *   new Person("Grace", "New York"),
	 *   new Person("Alan", "London")
	 * );
	 *
	 * Map<String, List<String>> grouped =
	 *   people.stream().collect(
	 *     Streams.groupBy(Person::city, Person::name)
	 *   );
	 * // grouped is an unmodifiable Map<String, List<String>>
	 * }</pre>
	 *
	 * @param keyMapper   function that extracts the grouping key
	 * @param valueMapper function that maps input elements to values stored in lists
	 * @param <T> the type of input elements
	 * @param <K> the type of map keys
	 * @param <V> the type of values stored in lists
	 * @return a {@code Collector} that groups by keys into immutable map entries
	 */
	public static <T, K, V> Collector<T, Map<K, List<V>>, Map<K, List<V>>> groupBy(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends V> valueMapper
	) {

		return new Collector<>() {
			@Override
			public Supplier<Map<K, List<V>>> supplier() {
				return HashMap::new;
			}

			@Override
			public BiConsumer<Map<K, List<V>>, T> accumulator() {
				return (map, t) -> {
					K key = keyMapper.apply(t);
					V val = valueMapper.apply(t);
					List<V> bucket = map.computeIfAbsent(key, k -> new ArrayList<>());
					bucket.add(val);
				};
			}

			@Override
			public BinaryOperator<Map<K, List<V>>> combiner() {
				return (left, right) -> {
					for (Map.Entry<K, List<V>> entry : right.entrySet()) {
						left.merge(
								entry.getKey(),
								entry.getValue(),
								(list1, list2) -> {
									list1.addAll(list2);
									return list1;
								}
						);
					}
					return left;
				};
			}

			@Override
			public Function<Map<K, List<V>>, Map<K, List<V>>> finisher() {
				return m -> {
					Map<K, List<V>> out = new HashMap<>(m.size());
					for (Map.Entry<K, List<V>> e : m.entrySet()) {
						out.put(e.getKey(), List.copyOf(e.getValue()));
					}
					return Collections.unmodifiableMap(out);
				};
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Set.of();
			}
		};
	}
}
