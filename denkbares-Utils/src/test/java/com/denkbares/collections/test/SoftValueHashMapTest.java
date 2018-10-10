/*
 * Copyright (C) 2018 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections.test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.denkbares.collections.SoftValueHashMap;
import com.denkbares.utils.Log;
import com.denkbares.utils.Stopwatch;

import static org.junit.Assert.*;

public class SoftValueHashMapTest {

	@Rule
	public RetryRule retry = new RetryRule(3);

	@Test
	public void basic() {
		Map<Key, Value> map = new SoftValueHashMap<>();
		Key key1 = new Key("key1");
		Key key2 = new Key("key2");
		Key key2b = new Key("key2");

		assertEquals(0, map.size());
		assertTrue(map.isEmpty());
		assertFalse(map.containsKey(key1));
		assertFalse(map.containsKey(key2));
		assertFalse(map.containsKey(key2b));
		assertEquals(null, map.get(key1));
		assertEquals(null, map.get(key2));

		// add some entries
		Value value1 = new Value("value1");
		Value value2 = new Value("value2");
		map.put(key1, value1);
		map.put(key2, value2);

		assertEquals(2, map.size());
		assertFalse(map.isEmpty());
		assertTrue(map.containsKey(key1));
		assertTrue(map.containsKey(key2));
		assertTrue(map.containsKey(key2b));
		assertEquals(value1, map.get(key1));
		assertEquals(value2, map.get(key2));

		// check for the entries
		assertEquals("[value1, value2]", new TreeSet<>(map.values()).toString());
		assertEquals("[key1, key2]", new TreeSet<>(map.keySet()).toString());

		map.remove(new Key("key3"));
		assertEquals(2, map.size());

		map.remove(new Key("key1"));
		assertEquals(1, map.size());
		assertTrue(map.entrySet().iterator().hasNext());
		assertEquals(key2, map.entrySet().iterator().next().getKey());
		assertEquals(value2, map.entrySet().iterator().next().getValue());

		map.clear();
		assertTrue(map.isEmpty());
		assertFalse(map.entrySet().iterator().hasNext());
	}

	@Test
	public void iteratorRemove() {
		Map<Key, Value> map = new SoftValueHashMap<>();
		Key key = new Key("key");
		Value value = new Value("value");

		map.put(key, value);
		Iterator<?> iterator = map.values().iterator();
		iterator.next();
		iterator.remove();
		assertTrue(map.isEmpty());

		map.put(key, value);
		iterator = map.keySet().iterator();
		iterator.next();
		iterator.remove();
		assertTrue(map.isEmpty());

		map.put(key, value);
		iterator = map.entrySet().iterator();
		iterator.next();
		iterator.remove();
		assertTrue(map.isEmpty());
	}

	@SuppressWarnings("UnusedAssignment")
	@Test
	public void weakness() {
		Map<Key, Value> map = new SoftValueHashMap<>();
		Key key1 = new Key("key1");
		Key key2 = new Key("key2");
		Key key2b = new Key("key2");
		Value value1 = new Value("value1");
		Value value2 = new Value("value2");
		Value value3 = new Value("value3");

		map.put(key1, value1);
		map.put(key2, value2);

		// overwrite key2 with new value and garbage collect values 1 and 2
		map.put(key2, value3);
		value1 = null;
		value2 = null;
		performSecureGC();

		// then only key2 shall be contained with value3
		assertEquals(1, map.size());
		assertFalse(map.isEmpty());
		assertFalse(map.containsKey(key1));
		assertTrue(map.containsKey(key2));
		assertTrue(map.containsKey(key2b));
		assertNull(map.get(key1));
		assertEquals(value3, map.get(key2));
	}

	@SuppressWarnings("UnusedAssignment")
	@Test
	public void nullSecure() {
		Map<Key, Value> map = new SoftValueHashMap<>();
		Value value = new Value("value");

		map.put(null, null);
		assertEquals(1, map.size());

		// add value for null that is garbage collected
		map.put(null, value);
		assertEquals(1, map.size());
		value = null;
		performSecureGC();
		assertEquals(0, map.size());

		// add value for null that is garbage collected
		// but overwritten by null before
		value = new Value("value");
		map.put(null, value);
		assertEquals(value, map.get(null));
		map.put(null, null);
		assertNull(map.get(null));
		value = null;
		performSecureGC();
		assertEquals(1, map.size());

		assertNull(map.values().iterator().next());
		assertNull(map.keySet().iterator().next());
		assertNull(map.entrySet().iterator().next().getKey());
		assertNull(map.entrySet().iterator().next().getValue());

		map.remove(null);
		assertEquals(0, map.size());
		assertNull(map.get(null));
	}

	private static void performSecureGC() {
		try {
			int blockSize = 1024 * 1024 * 1024; // 1 GB
			// force out of memory (by adding blocks or memory to a list)
			// --> all soft references are granted to be cleared
			//noinspection EndlessStream
			Stream.iterate(0, i -> i + 1).map(i -> new byte[blockSize]).collect(Collectors.toList());
			throw new IllegalStateException();
		}
		catch (OutOfMemoryError e) {
			// Ignore, we are fine now
			Log.info("memory cleared");
			System.gc();
		}
	}

	static class Item implements Comparable<Item> {
		private final String text;

		Item(String text) {
			if (text == null) throw new NullPointerException();
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Item item = (Item) o;
			return text.equals(item.text);
		}

		@Override
		public int hashCode() {
			return text.hashCode();
		}

		@Override
		public int compareTo(Item o) {
			return text.compareTo(o.text);
		}
	}

	static class Key extends Item {
		Key(String key) {
			super(key);
		}
	}

	static class Value extends Item {
		Value(String value) {
			super(value);
		}
	}

	public static class RetryRule implements TestRule {

		private final int retryCount;

		public RetryRule(int retryCount) {
			this.retryCount = retryCount;
		}

		@Override
		public Statement apply(Statement base, Description description) {
			return statement(base, description);
		}

		private Statement statement(final Statement base, final Description description) {

			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					Throwable caughtThrowable = null;
					Stopwatch timer = new Stopwatch().reset();
					for (int i = 0; i < retryCount; i++) {
						timer.start();
						try {
							base.evaluate();
							Log.info("Retry number " + i + 1 + ": " + timer.getDisplay());
							timer.reset();
							return;
						}
						catch (Throwable t) {
							timer.reset();
							caughtThrowable = t;
							Log.severe("Run " + (i + 1) + "/" + retryCount + " of '" + description.getDisplayName() + "' failed", t);
						}

					}
					Log.severe("Giving up after " + retryCount + " failures of '" + description.getDisplayName() + "'");
					if (caughtThrowable != null) {
						throw caughtThrowable;
					}
				}
			};
		}
	}
}