/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
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
