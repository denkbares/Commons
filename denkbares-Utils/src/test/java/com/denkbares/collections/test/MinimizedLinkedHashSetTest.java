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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.MinimizedLinkedHashSet;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 22.04.2017
 */
public class MinimizedLinkedHashSetTest {

	@Test
	public void basic() {
		Set<String> set = new MinimizedLinkedHashSet<>();
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

		// check that the order is preserved
		Assert.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));
		set.remove("a");
		set.add("a");
		Assert.assertEquals(Arrays.asList("b", "c", "a"), new ArrayList<>(set));
	}

	@Test
	public void remove() {
		Set<String> set = new MinimizedLinkedHashSet<>();
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

	@SuppressWarnings("OverwrittenKey")
	@Test
	public void iteratorRemove() {
		Set<String> set = new MinimizedLinkedHashSet<>();
		set.add("a");
		set.add("b");
		set.add("c");
		set.add("a");
		Assert.assertEquals(3, set.size());
		Assert.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));

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
	public void withSetElements() {
		Set<Set<String>> set = new MinimizedLinkedHashSet<>();
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
		Set<String> set = new MinimizedLinkedHashSet<>();
		//noinspection RedundantOperationOnEmptyContainer
		assertFalse(set.iterator().hasNext());
	}

	@Test
	public void iterateSingle() {
		Set<String> set = new MinimizedLinkedHashSet<>();
		set.add("a");
		Iterator<String> iterator = set.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void iterateNull() {
		Set<String> set = new MinimizedLinkedHashSet<>();
		set.add(null);
		Iterator<String> iterator = set.iterator();
		assertTrue(iterator.hasNext());
		assertNull(iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void iterateMultiple() {
		Set<String> set = new MinimizedLinkedHashSet<>();
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
		Set<String> set = new MinimizedLinkedHashSet<>();
		//noinspection RedundantOperationOnEmptyContainer
		set.iterator().next();
	}

	@Test(expected = NoSuchElementException.class)
	public void nextNextOfSingle() {
		Set<String> set = new MinimizedLinkedHashSet<>();
		set.add("a");
		Iterator<String> iterator = set.iterator();
		iterator.next();
		iterator.next();
	}

	@Test(expected = NoSuchElementException.class)
	public void nextNextNextOfTwo() {
		Set<String> set = new MinimizedLinkedHashSet<>();
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
		Set<String> set = new MinimizedLinkedHashSet<>();
		set.add("a");
		// remove without next should fail, even if there are elements available
		set.iterator().remove();
	}
}
