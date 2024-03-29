/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.denkbares.collections.CountingSet;

import static org.junit.Assert.*;

public class CountingSetTest {

	private CountingSet<String> set = new CountingSet<>();
	private final String testString = "test";
	private final String testString2 = "test2";

	@Before
	public void setUp() {
		set = new CountingSet<>();
	}

	@Test
	public void toStringTest() {
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		Set<String> s = new HashSet<>();
		s.add("foo");
		set.add("foo");
		assertEquals("{foo=1}", set.toString());
		set.add("foo");
		assertEquals("{foo=2}", set.toString());
		set.clear();
		assertEquals("{}", set.toString());
	}

	@Test
	public void isEmpty() {
		assertTrue(set.isEmpty());
		set.add(testString);
		assertFalse(set.isEmpty());
		set.add(testString);
		set.remove(testString);
		assertFalse(set.isEmpty());
		set.remove(testString);
		assertTrue(set.isEmpty());
	}

	@Test
	public void size() {
		assertEquals(0, set.size());
		set.add(testString);
		assertEquals(1, set.size());
		set.remove(testString);
		assertEquals(0, set.size());
	}

	@Test
	public void contains() {
		assertFalse(set.contains(testString));
		set.add(testString);
		assertTrue(set.contains(testString));
	}

	@Test
	public void iterator() {
		set.add(testString);
		assertEquals(testString, set.iterator().next());
	}

	@Test
	public void toArray() {
		set.add(testString);
		assertArrayEquals(new Object[] { testString }, set.toArray());
		assertArrayEquals(new String[] { testString }, set.toArray(new String[1]));
	}

	@Test
	public void getCount() {
		assertEquals(0, set.getCount(testString));
		set.add(testString);
		assertEquals(1, set.getCount(testString));
		set.add(testString);
		assertEquals(2, set.getCount(testString));
		set.remove(testString);
		assertEquals(1, set.getCount(testString));
	}

	@Test
	public void addAndContainsAll() {
		List<String> list = Arrays.asList(testString, testString2);
		set.addAll(list);
		assertTrue(set.containsAll(list));
	}

	@Test
	public void retainAll() {
		List<String> list = Arrays.asList(testString, testString2);
		set.addAll(list);
		set.addAll(list);
		set.retainAll(Collections.singletonList(testString2));
		assertTrue(set.contains(testString));
		assertTrue(set.contains(testString2));
		set.retainAll(new HashSet<Object>(Collections.singletonList(testString2)));
		assertFalse(set.contains(testString));
		assertTrue(set.contains(testString2));
		set.clear();
		assertTrue(set.isEmpty());
	}

	@Test
	public void removeAll() {
		List<String> list = Arrays.asList(testString, testString2);
		assertFalse(set.containsAll(list));
		set.addAll(list);
		assertTrue(set.containsAll(list));
		set.addAll(list);
		set.removeAll(list);
		assertTrue(set.containsAll(list));
		set.removeAll(list);
		assertFalse(set.containsAll(list));
	}

	@Test
	public void misc() {
		assertEquals(-1, set.dec("huhu"));
		assertEquals(-1, set.dec("huhu"));
		assertFalse(set.remove("huhu"));
	}
}
