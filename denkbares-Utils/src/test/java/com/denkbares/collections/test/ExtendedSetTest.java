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
import java.util.Set;

import org.junit.Test;

import com.denkbares.collections.ExtendedSet;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 23.05.2016
 */
public class ExtendedSetTest {
	@Test
	public void basic() {
		String coreString = "core";
		Set<String> core = Collections.singleton(coreString);

		// check if entry is covered by a new value
		//noinspection RedundantStringConstructorCall
		ExtendedSet<String> covered = new ExtendedSet<>(core, new String("core"));
		assertEquals(1, covered.size());
		assertEquals(Collections.singleton("core"), covered.stream().collect(toSet()));
		assertEquals("core", covered.iterator().next());
		assertTrue(covered.contains("core"));
		//noinspection StringEquality
		assertTrue(covered.iterator().next() != coreString);

		// check if a new entry is extended
		ExtendedSet<String> extended = new ExtendedSet<>(core, "extended");
		assertEquals(2, extended.size());
		assertEquals(new HashSet<>(Arrays.asList("extended", "core")),
				extended.stream().collect(toSet()));
		assertEquals("extended", extended.iterator().next());
		assertTrue(extended.contains("core"));
		assertTrue(extended.contains("extended"));

		// check combination of both
		ExtendedSet<String> multiple = new ExtendedSet<>(new ExtendedSet<>(extended, "extended"), "core");
		assertEquals(2, multiple.size());
		assertEquals(new HashSet<>(Arrays.asList("extended", "core")),
				multiple.stream().collect(toSet()));
		assertTrue(multiple.contains("core"));
		assertTrue(multiple.contains("extended"));
	}
}
