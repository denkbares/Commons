/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.denkbares.collections.FilterIterator;

import static org.junit.Assert.assertEquals;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 05.07.2016
 */
public class FilterIteratorTest {

	@Test
	public void basic() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

		assertItems(FilterIterator.filter(list.iterator(), i -> i >= 8), 8, 9);
		assertItems(FilterIterator.filter(list, i -> i >= 8).iterator(), 8, 9);
		assertItems(FilterIterator.filter(list.iterator(), i -> i <= 3), 1, 2, 3);
		assertItems(FilterIterator.filter(list, i -> i <= 3).iterator(), 1, 2, 3);

		assertItems(FilterIterator.filter(list.iterator(), i -> i != 3), 1, 2, 4, 5, 6, 7, 8, 9);
		assertItems(FilterIterator.filter(list, i -> i != 3).iterator(), 1, 2, 4, 5, 6, 7, 8, 9);
		assertItems(FilterIterator.takeWhile(list.iterator(), i -> i != 3), 1, 2);
		assertItems(FilterIterator.takeWhile(list, i -> i != 3).iterator(), 1, 2);
	}

	@SuppressWarnings("unchecked")
	private <T> void assertItems(Iterator<T> actual, T... expected) {
		List<T> actualList = new ArrayList<>();
		actual.forEachRemaining(actualList::add);
		assertEquals(Arrays.asList(expected), actualList);
	}
}
