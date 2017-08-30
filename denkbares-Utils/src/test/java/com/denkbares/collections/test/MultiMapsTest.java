/*
 * Copyright (C) 2013 denkbares GmbH
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
package com.denkbares.collections.test;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MultiMap;
import com.denkbares.collections.MultiMaps;
import com.denkbares.collections.MultiMaps.CollectionFactory;
import com.denkbares.collections.N2MMap;

import static org.junit.Assert.*;

public class MultiMapsTest {

	private N2MMap<String, String> baseMap;

	@Before
	public void initBase() {
		baseMap = new N2MMap<>();
		baseMap.put("a", "1");
		baseMap.put("a", "2");
		baseMap.put("b", "2");
		baseMap.put("b", "3");
	}

	@Test
	public void factories() {
		checkFactory(MultiMaps.hashFactory());
		checkFactory(MultiMaps.minimizedFactory());
		checkFactory(MultiMaps.treeFactory());
		//noinspection RedundantTypeArguments
		checkFactory(MultiMaps.treeFactory(Comparator.<String>reverseOrder()));
		checkFactory(MultiMaps.linkedFactory());
	}

	@Test
	public void minimizedHashSet() {
		Set<String> set = MultiMaps.<String>minimizedFactory().createSet();
		// test empty set
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());
		assertFalse(set.iterator().hasNext());

		// one element
		set.add("test");
		assertTrue(set.contains("test"));
		assertFalse(set.contains(null));
		assertFalse(set.contains("?"));

		assertFalse(set.isEmpty());
		assertEquals(1, set.size());
		Iterator<String> iterator = set.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("test", iterator.next());
		assertFalse(iterator.hasNext());
		iterator.remove();
		assertTrue(set.isEmpty());

		// three elements
		set.add("1");
		set.add(null);
		set.add("3");
		assertTrue(set.contains("1"));
		assertTrue(set.contains(null));
		assertTrue(set.contains("3"));
		assertFalse(set.contains("2"));

		assertFalse(set.isEmpty());
		assertEquals(3, set.size());

		iterator = set.iterator();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertFalse(iterator.hasNext());

		iterator = set.iterator();
		iterator.next();
		iterator.remove();
		assertEquals(2, set.size());
		assertTrue(iterator.hasNext());
		iterator.next();
		iterator.remove();
		assertEquals(1, set.size());
		assertTrue(iterator.hasNext());
		iterator.next();
		iterator.remove();
		assertEquals(0, set.size());
	}

	@Test(expected = NoSuchElementException.class)
	public void minimizedHashSetException1() {
		Set<String> set = MultiMaps.<String>minimizedFactory().createSet();
		set.iterator().next();
	}

	@Test(expected = IllegalStateException.class)
	public void minimizedHashSetException2() {
		Set<String> set = MultiMaps.<String>minimizedFactory().createSet();
		set.iterator().remove();
	}

	@Test(expected = IllegalStateException.class)
	public void minimizedHashSetException3() {
		Set<String> set = MultiMaps.<String>minimizedFactory().createSet();
		set.add("test");
		set.iterator().remove();
	}

	@Test(expected = IllegalStateException.class)
	public void minimizedHashSetException4() {
		Set<String> set = MultiMaps.<String>minimizedFactory().createSet();
		set.add("test");
		Iterator<String> iterator = set.iterator();
		iterator.next();
		iterator.remove();
		iterator.remove();
	}

	private void checkFactory(CollectionFactory<String> factory) {
		N2MMap<String, String> map = new N2MMap<>(factory, factory);
		map.putAll(baseMap);
		assertEquals(baseMap, map);
	}

	@Test
	public void treeFactoryComparator() {
		N2MMap<String, String> map = new N2MMap<>(MultiMaps.treeFactory(Comparator.<String>reverseOrder()), MultiMaps.<String>treeFactory(Comparator
				.reverseOrder()));
		map.putAll(baseMap);
		assertEquals("[b, a]", map.keySet().toString());
		assertEquals("[3, 2, 1]", map.valueSet().toString());
	}

	@Test
	public void singletonMultiMap() {
		MultiMap<String, String> map = MultiMaps.singletonMultiMap("a", "b");

		assertTrue(map.containsKey("a"));
		assertFalse(map.containsValue("a"));
		assertFalse(map.containsKey("b"));
		assertTrue(map.containsValue("b"));

		assertTrue(map.contains("a", "b"));
		assertFalse(map.contains("a", "a"));
		assertFalse(map.contains("b", "b"));
		assertFalse(map.contains("b", "a"));

		assertFalse(map.isEmpty());
		assertEquals(1, map.size());

		assertEquals(Collections.singleton("a"), map.keySet());
		assertEquals(Collections.singleton("b"), map.valueSet());

		assertEquals(Collections.singleton("a"), map.getKeys("b"));
		assertEquals(Collections.singleton("b"), map.getValues("a"));
		assertTrue(map.getKeys("a").isEmpty());
		assertTrue(map.getValues("b").isEmpty());

		assertEquals("b", map.toMap().get("a").iterator().next());
		assertNull(map.toMap().get("b"));

		MultiMap<Object, Object> other = new DefaultMultiMap<>();
		assertNotEquals(other, map);
		assertNotEquals(other.hashCode(), map.hashCode());
		assertNotEquals(other.toString(), map.toString());
		other.put("a", "b");
		assertEquals(other, map);
		assertEquals(other.hashCode(), map.hashCode());
		assertEquals(other.toString(), map.toString());
		other.put("b", "a");
		assertNotEquals(other, map);
		assertNotEquals(other.hashCode(), map.hashCode());
		assertNotEquals(other.toString(), map.toString());
	}

	@Test
	public void synchronizedMultiMap() {
		DefaultMultiMap<String, String> normalMap = new DefaultMultiMap<>();
		normalMap.put("hi", "ho");
		normalMap.put("hi", "he");
		normalMap.put("hey", "hu");
		DefaultMultiMap<String, String> normalMap2 = new DefaultMultiMap<>();
		normalMap2.put("hi", "ho");
		normalMap2.put("hi", "he");
		normalMap2.put("hey", "hu");
		MultiMap<String, String> synchronizedMap = MultiMaps.synchronizedMultiMap(normalMap2);

		assertEquals(normalMap.size(), synchronizedMap.size());
		assertEquals(normalMap.isEmpty(), synchronizedMap.isEmpty());
		assertEquals(normalMap.containsKey("hi"), synchronizedMap.containsKey("hi"));
		assertEquals(normalMap.containsValue("ho"), synchronizedMap.containsValue("ho"));
		assertEquals(normalMap.contains("hi", "ho"), synchronizedMap.contains("hi", "ho"));
		assertEquals(normalMap.getValues("hi"), synchronizedMap.getValues("hi"));
		assertEquals(normalMap.getKeys("ho"), synchronizedMap.getKeys("ho"));
		assertEquals(normalMap.put("a", "b"), synchronizedMap.put("a", "b"));
		assertEquals(normalMap.getValues("a"), synchronizedMap.getValues("a"));
		assertEquals(normalMap.removeKey("a"), synchronizedMap.removeKey("a"));
		assertEquals(normalMap.getValues("a"), synchronizedMap.getValues("a"));
		assertEquals(normalMap.put("a", "b"), synchronizedMap.put("a", "b"));
		assertEquals(normalMap.removeValue("b"), synchronizedMap.removeValue("b"));
		assertEquals(normalMap.getValues("a"), synchronizedMap.getValues("a"));
		assertEquals(normalMap.put("a", "b"), synchronizedMap.put("a", "b"));
		assertEquals(normalMap.remove("a", "b"), synchronizedMap.remove("a", "b"));
		assertEquals(normalMap.getValues("a"), synchronizedMap.getValues("a"));
		assertEquals(normalMap.putAll(synchronizedMap), synchronizedMap.putAll(normalMap));
		assertEquals(normalMap.getValues("hi"), synchronizedMap.getValues("hi"));
		assertEquals(normalMap.keySet(), synchronizedMap.keySet());
		assertEquals(normalMap.valueSet(), synchronizedMap.valueSet());
		assertEquals(normalMap.entrySet(), synchronizedMap.entrySet());
		assertEquals(normalMap.toMap(), synchronizedMap.toMap());


		normalMap.clear();
		synchronizedMap.clear();
		assertEquals(normalMap.isEmpty(), synchronizedMap.isEmpty());

	}

	@Test
	public void reversedMultiMap() {
		DefaultMultiMap<String, String> normalMap = new DefaultMultiMap<>();
		normalMap.put("hi", "ho");
		normalMap.put("hi", "he");
		normalMap.put("hey", "hu");
		DefaultMultiMap<String, String> hidden = new DefaultMultiMap<>();
		hidden.put("ho", "hi");
		hidden.put("he", "hi");
		MultiMap<String, String> reversed = MultiMaps.reversed(hidden);
		reversed.put("hey", "hu");

		// check if new entry is written through the reversed to the original map
		assertEquals("hu", reversed.getAnyValue("hey"));
		assertEquals("hey", reversed.getAnyKey("hu"));
		assertEquals("hu", hidden.getAnyKey("hey"));
		assertEquals("hey", hidden.getAnyValue("hu"));

		assertEquals(normalMap.size(), reversed.size());
		assertEquals(normalMap.isEmpty(), reversed.isEmpty());
		assertEquals(normalMap.containsKey("hi"), reversed.containsKey("hi"));
		assertEquals(normalMap.containsValue("ho"), reversed.containsValue("ho"));
		assertEquals(normalMap.contains("hi", "ho"), reversed.contains("hi", "ho"));
		assertEquals(normalMap.getValues("hi"), reversed.getValues("hi"));
		assertEquals(normalMap.getKeys("ho"), reversed.getKeys("ho"));
		assertEquals(normalMap.put("a", "b"), reversed.put("a", "b"));
		assertEquals(normalMap.getValues("a"), reversed.getValues("a"));
		assertEquals(normalMap.removeKey("a"), reversed.removeKey("a"));
		assertEquals(normalMap.getValues("a"), reversed.getValues("a"));
		assertEquals(normalMap.put("a", "b"), reversed.put("a", "b"));
		assertEquals(normalMap.removeValue("b"), reversed.removeValue("b"));
		assertEquals(normalMap.getValues("a"), reversed.getValues("a"));
		assertEquals(normalMap.put("a", "b"), reversed.put("a", "b"));
		assertEquals(normalMap.remove("a", "b"), reversed.remove("a", "b"));
		assertEquals(normalMap.getValues("a"), reversed.getValues("a"));
		assertEquals(normalMap.putAll(reversed), reversed.putAll(normalMap));
		assertEquals(normalMap.getValues("hi"), reversed.getValues("hi"));
		assertEquals(normalMap.keySet(), reversed.keySet());
		assertEquals(normalMap.valueSet(), reversed.valueSet());
		assertEquals(normalMap.entrySet(), reversed.entrySet());
		assertEquals(normalMap.toMap(), reversed.toMap());


		normalMap.clear();
		reversed.clear();
		assertEquals(normalMap.isEmpty(), reversed.isEmpty());
		assertEquals(normalMap, reversed);
		assertEquals(normalMap.isEmpty(), hidden.isEmpty());
	}

	@Test
	public void emptyMultiMap() {
		MultiMap<Object, Object> map = MultiMaps.emptyMultiMap();
		assertEquals(0, map.size());
		assertEquals(false, map.containsKey("x"));
		assertEquals(false, map.containsValue("x"));
		assertEquals(false, map.contains("x", "y"));
		assertEquals(Collections.emptySet(), map.getValues("x"));
		assertEquals(Collections.emptySet(), map.getKeys("x"));
		assertEquals(Collections.emptySet(), map.keySet());
		assertEquals(Collections.emptySet(), map.valueSet());
		assertEquals(Collections.emptySet(), map.entrySet());
		assertEquals(Collections.emptyMap(), map.toMap());
		map.clear();
		assertEquals(MultiMaps.emptyMultiMap(), map);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException1() {
		MultiMaps.emptyMultiMap().put("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException2() {
		MultiMaps.emptyMultiMap().removeKey("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException3() {
		MultiMaps.emptyMultiMap().removeValue("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException4() {
		MultiMaps.emptyMultiMap().remove("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException5() {
		MultiMaps.emptyMultiMap().putAll(Collections.emptyMap());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void emptyMapException6() {
		MultiMaps.emptyMultiMap().putAll(MultiMaps.emptyMultiMap());
	}

	@Test
	public void unmodifiableMultiMap() {
		DefaultMultiMap<String, String> normalMap = new DefaultMultiMap<>();
		normalMap.put("hi", "ho");
		normalMap.put("hi", "he");
		normalMap.put("hey", "hu");
		DefaultMultiMap<String, String> normalMap2 = new DefaultMultiMap<>();
		normalMap2.put("hi", "ho");
		normalMap2.put("hi", "he");
		normalMap2.put("hey", "hu");
		MultiMap<String, String> unmodifiableMap = MultiMaps.unmodifiableMultiMap(normalMap2);

		assertEquals(normalMap.size(), unmodifiableMap.size());
		assertEquals(normalMap.isEmpty(), unmodifiableMap.isEmpty());
		assertEquals(normalMap.containsKey("hi"), unmodifiableMap.containsKey("hi"));
		assertEquals(normalMap.containsValue("ho"), unmodifiableMap.containsValue("ho"));
		assertEquals(normalMap.contains("hi", "ho"), unmodifiableMap.contains("hi", "ho"));
		assertEquals(normalMap.getValues("hi"), unmodifiableMap.getValues("hi"));
		assertEquals(normalMap.getKeys("ho"), unmodifiableMap.getKeys("ho"));
		assertEquals(normalMap.getValues("a"), unmodifiableMap.getValues("a"));
		assertEquals(normalMap.keySet(), unmodifiableMap.keySet());
		assertEquals(normalMap.valueSet(), unmodifiableMap.valueSet());
		assertEquals(normalMap.entrySet(), unmodifiableMap.entrySet());
		assertEquals(normalMap.toMap(), unmodifiableMap.toMap());
	}


	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException1() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).put("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException2() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).removeKey("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException3() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).removeValue("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException4() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).remove("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException5() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).putAll(Collections.emptyMap());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableMapException6() {
		MultiMaps.unmodifiableMultiMap(new DefaultMultiMap<>()).putAll(MultiMaps.emptyMultiMap());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException1() {
		MultiMaps.singletonMultiMap("a", "b").put("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException2() {
		MultiMaps.singletonMultiMap("a", "b").removeKey("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException3() {
		MultiMaps.singletonMultiMap("a", "b").removeValue("a");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException4() {
		MultiMaps.singletonMultiMap("a", "b").remove("a", "b");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException5() {
		MultiMaps.singletonMultiMap("a", "b").putAll(Collections.emptyMap());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void singletonMapException6() {
		MultiMaps.singletonMultiMap("a", "b").putAll(MultiMaps.emptyMultiMap());
	}

	@Test
	public void toMultiMap1() {
		MultiMap<String, String> map = Stream.of("a1", "a2", "a3", "b1", "b2")
				.collect(MultiMaps.toMultiMap(string -> string.substring(0, 1)));

		assertTrue(map.contains("a", "a1"));
		assertTrue(map.contains("a", "a2"));
		assertTrue(map.contains("a", "a3"));
		assertTrue(map.contains("b", "b1"));
		assertTrue(map.contains("b", "b2"));
	}

	@Test
	public void toMultiMap2() {
		MultiMap<String, String> map = Stream.of("a1", "a2", "a3", "b1", "b2")
				.collect(MultiMaps.toMultiMap(string -> string.substring(0, 1), string -> string.substring(1)));

		assertTrue(map.contains("a", "1"));
		assertTrue(map.contains("a", "2"));
		assertTrue(map.contains("a", "3"));
		assertTrue(map.contains("b", "1"));
		assertTrue(map.contains("b", "2"));
	}
}
