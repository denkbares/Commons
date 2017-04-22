/*
 * Copyright (C) 2017 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections.test;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.MinimizedHashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 22.04.2017
 */
public class MinimizedHashSetTest {

	@Test
	public void basic() throws Exception {
		Set<String> set = new MinimizedHashSet<>();
		Assert.assertEquals(0, set.size());

		set.add("a");
		Assert.assertEquals(1, set.size());
		set.add("a");
		Assert.assertEquals(1, set.size());
		Assert.assertTrue(set.contains("a"));
		Assert.assertFalse(set.contains("b"));
		Assert.assertFalse(set.contains("c"));

		set.add("b");
		Assert.assertEquals(2, set.size());
		set.add("b");
		Assert.assertEquals(2, set.size());
		Assert.assertTrue(set.contains("a"));
		Assert.assertTrue(set.contains("b"));
		Assert.assertFalse(set.contains("c"));

		set.add("c");
		Assert.assertEquals(3, set.size());
		set.add("c");
		Assert.assertEquals(3, set.size());
		Assert.assertTrue(set.contains("a"));
		Assert.assertTrue(set.contains("b"));
		Assert.assertTrue(set.contains("c"));
	}

	@Test
	public void remove() throws Exception {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		set.add("b");
		set.add("c");

		set.remove("a");
		Assert.assertEquals(2, set.size());
		set.remove("a");
		Assert.assertEquals(2, set.size());
		set.remove("foo");
		Assert.assertEquals(2, set.size());
		set.remove("b");
		Assert.assertEquals(1, set.size());
		set.remove("b");
		Assert.assertEquals(1, set.size());
		set.remove("c");
		Assert.assertEquals(0, set.size());
		set.remove("c");
		Assert.assertEquals(0, set.size());
	}

	@Test
	public void iteratorRemove() throws Exception {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		set.add("b");
		set.add("c");
		set.add("a");
		Assert.assertEquals(3, set.size());

		Iterator<String> iterator = set.iterator();
		iterator.next();
		iterator.remove();
		Assert.assertEquals(2, set.size());
		iterator.next();
		iterator.remove();
		Assert.assertEquals(1, set.size());
		iterator.next();
		iterator.remove();
		Assert.assertEquals(0, set.size());
	}

	@Test
	public void withSetElements() throws Exception {
		Set<Set<String>> set = new MinimizedHashSet<>();
		set.add(Collections.singleton("a"));
		set.add(Collections.singleton("b"));
		set.add(Collections.singleton("c"));
		set.add(Collections.singleton("a"));
		Assert.assertEquals(3, set.size());

		Iterator<Set<String>> iterator = set.iterator();
		iterator.next();
		iterator.remove();
		Assert.assertEquals(2, set.size());
		iterator.next();
		iterator.remove();
		Assert.assertEquals(1, set.size());
		iterator.next();
		iterator.remove();
		Assert.assertEquals(0, set.size());
	}

	@Test
	public void iterateEmpty() {
		//noinspection MismatchedQueryAndUpdateOfCollection
		Set<String> set = new MinimizedHashSet<>();
		assertFalse(set.iterator().hasNext());
	}

	@Test
	public void iterateSingle() {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		Iterator<String> iterator = set.iterator();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test
	public void iterateMultiple() {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		set.add("b");
		Iterator<String> iterator = set.iterator();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertTrue(iterator.hasNext());
		iterator.next();
		assertFalse(iterator.hasNext());
	}

	@Test(expected = NoSuchElementException.class)
	public void nextOfEmpty() {
		//noinspection MismatchedQueryAndUpdateOfCollection
		Set<String> set = new MinimizedHashSet<>();
		set.iterator().next();
	}

	@Test(expected = NoSuchElementException.class)
	public void nextNextOfSingle() {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		Iterator<String> iterator = set.iterator();
		iterator.next();
		iterator.next();
	}

	@Test(expected = NoSuchElementException.class)
	public void nextNextNextOfTwo() {
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		set.add("b");
		Iterator<String> iterator = set.iterator();
		iterator.next();
		iterator.next();
		iterator.next();
	}

	@Test(expected = IllegalStateException.class)
	public void minimizedHashSetException2() {
		//noinspection MismatchedQueryAndUpdateOfCollection
		Set<String> set = new MinimizedHashSet<>();
		set.add("a");
		// remove without next should fail, even if there are elements available
		set.iterator().remove();
	}
}
