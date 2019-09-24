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
