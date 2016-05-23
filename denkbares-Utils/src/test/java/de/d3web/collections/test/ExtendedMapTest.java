/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package de.d3web.collections.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import de.d3web.collections.ExtendedMap;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 23.05.2016
 */
public class ExtendedMapTest {
	@SuppressWarnings("StringEquality")
	@Test
	public void basic() {
		String coreString = "core";
		Map<String, Integer> core = Collections.singletonMap(coreString, 1);

		// check if entry is covered by a new value
		//noinspection RedundantStringConstructorCall
		ExtendedMap<String, Integer> covered = new ExtendedMap<>(core, new String("core"), 2);
		assertEquals(1, covered.size());
		assertEquals(Collections.singleton("core"), covered.keySet());
		assertEquals(2, covered.entrySet().iterator().next().getValue().intValue());
		assertEquals(2, covered.get("core").intValue());
		assertTrue(covered.entrySet().iterator().next().getKey() != coreString);
		assertTrue(covered.keySet().iterator().next() != coreString);
		assertEquals(2, covered.values().iterator().next().intValue());

		// check if a new entry is extended
		ExtendedMap<String, Integer> extended = new ExtendedMap<>(core, "extended", 3);
		assertEquals(2, extended.size());
		assertEquals(new HashSet<>(Arrays.asList("extended", "core")), extended.keySet());
		assertEquals(3, extended.entrySet().iterator().next().getValue().intValue());
		assertEquals(1, extended.get("core").intValue());
		assertEquals(3, extended.get("extended").intValue());
		assertEquals("extended", extended.entrySet().iterator().next().getKey());
		assertEquals("extended", extended.keySet().iterator().next());
		assertEquals(3, extended.values().iterator().next().intValue());

		// check combination of both
		ExtendedMap<String, Integer> multiple = new ExtendedMap<>(new ExtendedMap<>(extended, "extended", 5), "core", 4);
		assertEquals(2, multiple.size());
		assertEquals(new HashSet<>(Arrays.asList("extended", "core")), multiple.keySet());
		assertTrue(multiple.containsKey("core"));
		assertTrue(multiple.containsKey("extended"));
		assertTrue(multiple.containsValue(4));
		assertTrue(multiple.containsValue(5));
		assertFalse(multiple.containsValue(1));
		assertFalse(multiple.containsValue(2));
		assertFalse(multiple.containsValue(3));
		assertEquals(4, multiple.get("core").intValue());
		assertEquals(5, multiple.get("extended").intValue());
	}
}
