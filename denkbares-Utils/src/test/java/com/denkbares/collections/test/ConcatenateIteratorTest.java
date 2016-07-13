/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
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
