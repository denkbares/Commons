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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.ConcatenateIterator;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 07.07.2016
 */
public class ConcatenateIteratorTest {

	@Test
	public void concatIterators() {
		Iterator<Integer> concat = ConcatenateIterator.concat(
				Collections.emptyIterator(), Arrays.asList(1, 2, 3).iterator(),
				Collections.emptyIterator(), Arrays.asList(4, 5, 6).iterator());

		ArrayList<Integer> all = new ArrayList<>();
		concat.forEachRemaining(all::add);
		Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), all);
	}

	@Test
	public void concatCollections() {
		Iterator<Integer> concat = ConcatenateIterator.concat(
				Collections.emptyList(), Arrays.asList(1, 2, 3),
				Collections.emptySet(), Arrays.asList(4, 5, 6));

		ArrayList<Integer> all = new ArrayList<>();
		concat.forEachRemaining(all::add);
		Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), all);
	}

	@Test
	public void flatMap() throws Exception {
		Iterator<String> flatMap = ConcatenateIterator.flatMap(Arrays.asList(1, 2, 3).iterator(),
				i -> Arrays.asList("a", "b", "c").stream().map(s -> s + i).iterator());

		ArrayList<String> all = new ArrayList<>();
		flatMap.forEachRemaining(all::add);
		Assert.assertEquals(Arrays.asList("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"), all);
	}
}
