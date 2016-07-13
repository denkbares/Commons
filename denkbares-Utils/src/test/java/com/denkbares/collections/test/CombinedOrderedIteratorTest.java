/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.denkbares.collections.CombinedOrderedIterator;

import static org.junit.Assert.assertEquals;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 07.07.2016
 */
public class CombinedOrderedIteratorTest {

	@Test
	public void basic() {
		CombinedOrderedIterator<Integer> combine = new CombinedOrderedIterator<>(Arrays.asList(
				Arrays.asList(1, 3, 5).iterator(),
				Arrays.asList(2, 8, 9).iterator(),
				Collections.emptyIterator(),
				Arrays.asList(4, 6, 7).iterator()
		));

		List<Integer> all = new ArrayList<>();
		combine.forEachRemaining(all::add);

		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), all);
	}

	@Test
	public void specialOrder() {
		CombinedOrderedIterator<Integer> combine = new CombinedOrderedIterator<>(Arrays.asList(
				Arrays.asList(5, 3, 1).iterator(),
				Arrays.asList(9, 8, 2).iterator(),
				Collections.emptyIterator(),
				Arrays.asList(7, 6, 4).iterator()),
				(a, b) -> Integer.compare(b, a));

		List<Integer> all = new ArrayList<>();
		combine.forEachRemaining(all::add);

		assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1), all);
	}
}
