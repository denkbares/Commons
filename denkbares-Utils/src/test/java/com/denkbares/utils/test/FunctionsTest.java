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

package com.denkbares.utils.test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import com.denkbares.utils.Functions;

import static org.junit.Assert.assertEquals;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 28.04.2020
 */
public class FunctionsTest {

	@Test
	public void cachedFunction() {
		AtomicInteger count = new AtomicInteger(0);
		Function<String, String> counter = (prefix) -> prefix + ": " + count.incrementAndGet();
		Function<String, String> cached = Functions.cache(counter);

		// we already created the cached supplier, but wait for usage until counter increased
		assertEquals("A: 1", counter.apply("A"));
		assertEquals("B: 2", counter.apply("B"));

		// check cached
		assertEquals("A: 3", cached.apply("A"));
		assertEquals("B: 4", cached.apply("B"));
		assertEquals("A: 3", cached.apply("A"));
		assertEquals("B: 4", cached.apply("B"));

		// and check original supplier that is has been called only once
		assertEquals("A: 5", counter.apply("A"));
		assertEquals("A: 6", counter.apply("A"));

		// and cached is not influenced
		assertEquals("A: 3", cached.apply("A"));
	}

	@Test
	public void cachedSupplier() {
		AtomicInteger count = new AtomicInteger(0);
		Supplier<Integer> counter = count::incrementAndGet;
		Supplier<Integer> cached = Functions.cache(counter);

		// we already created the cached supplier, but wait for usage until counter increased
		assertEquals(1, (int) counter.get());
		assertEquals(2, (int) counter.get());

		// check cached
		assertEquals(3, (int) cached.get());
		assertEquals(3, (int) cached.get());

		// and check original supplier that is has been called only once
		assertEquals(4, (int) counter.get());
		assertEquals(5, (int) counter.get());

		// and cached is not influenced
		assertEquals(3, (int) cached.get());
	}
}
