/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MappingMultiMap;
import com.denkbares.collections.MultiMap;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 21.03.2020
 */
public class MappingMultiMapTest {

	@Test
	public void basic() {
		DefaultMultiMap<Object, Object> base = DefaultMultiMap.newLinked();
		base.putAll("1", "a", "b");
		base.putAll("2", "b", "c", "d", "e");
		MappingMultiMap<Object, Object> mapped = new MappingMultiMap<>(base.keySet(), base::getValues);

		assertTrue(mapped.contains("1", "a"));
		assertTrue(mapped.contains("1", "b"));
		assertFalse(mapped.contains("1", "c"));
		assertTrue(mapped.contains("2", "b"));
		assertTrue(mapped.contains("2", "c"));
		assertTrue(mapped.contains("2", "d"));
		assertTrue(mapped.contains("2", "e"));

		assertTrue(mapped.containsKey("1"));
		assertTrue(mapped.containsKey("2"));
		assertFalse(mapped.containsKey("3"));

		assertTrue(mapped.containsValue("a"));
		assertTrue(mapped.containsValue("c"));
		assertTrue(mapped.containsValue("e"));
		assertFalse(mapped.containsValue("f"));

		assertEquals(2, mapped.keySet().size());
		assertEquals(5, mapped.valueSet().size());
		assertEquals(6, mapped.size());

		assertEquals(2, mapped.getKeys("b").size());
		assertTrue(mapped.getKeys("b").contains("1"));
		assertTrue(mapped.getKeys("b").contains("2"));

		assertEquals(2, mapped.getValues("1").size());
		assertTrue(mapped.getValues("1").contains("a"));
		assertTrue(mapped.getValues("1").contains("b"));

		assertEquals(6, mapped.entrySet().size());
		assertTrue(mapped.entrySet().stream().map(Map.Entry::getKey).anyMatch("1"::equals));
		assertTrue(mapped.entrySet().stream().map(Map.Entry::getKey).anyMatch("2"::equals));
	}

	@Test
	public void mapOnlyKeys() {
		MultiMap<String, String> mapped = new MappingMultiMap<>(Collections.emptySet(), k -> {
			// throw an exception is mapping called for any (non-existing) key
			throw new NoSuchElementException(k);
		});

		assertFalse(mapped.containsKey("1"));
		assertFalse(mapped.containsValue("a"));
		assertFalse(mapped.contains("1", "a"));
		assertFalse(mapped.entrySet().stream().iterator().hasNext());
	}
}
