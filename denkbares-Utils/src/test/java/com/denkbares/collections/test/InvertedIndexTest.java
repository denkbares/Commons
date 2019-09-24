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
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.denkbares.collections.InvertedIndex;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 18.07.16
 */
public class InvertedIndexTest {

	@Test
	public void basic() {
		InvertedIndex<String> invertedIndex = new InvertedIndex<>();
		invertedIndex.put("key", "value1");
		invertedIndex.put("key", "value2");
		invertedIndex.put("key2", "value2");
		invertedIndex.put("key", "value3");
		invertedIndex.put("key2", "value4");

		Set<String> any = invertedIndex.getAny("key key2");
		List<String> anyExpected = Arrays.asList("value1", "value2", "value3", "value4");
		assertTrue(anyExpected.containsAll(any) && any.containsAll(anyExpected));

		Set<String> all = invertedIndex.getAll("key key2");
		List<String> allExpected = Collections.singletonList("value2");
		assertTrue(allExpected.containsAll(all) && all.containsAll(allExpected));

		all = invertedIndex.getAll("key2");
		allExpected = Arrays.asList("value2", "value4");
		assertTrue(allExpected.containsAll(all) && all.containsAll(allExpected));

		invertedIndex.remove("key", "value1");

		all = invertedIndex.getAll("key");
		allExpected = Arrays.asList("value2", "value3");
		assertTrue(allExpected.containsAll(all) && all.containsAll(allExpected));
	}

	@Test
	public void matches() {
		assertEquals(7, InvertedIndex.matches("Hi, how are you?", 4, "how"));
		assertEquals(-1, InvertedIndex.matches("Hi, how are you?", 4, "what"));
	}
}
