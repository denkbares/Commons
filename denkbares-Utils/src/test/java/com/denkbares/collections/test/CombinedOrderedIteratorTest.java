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
