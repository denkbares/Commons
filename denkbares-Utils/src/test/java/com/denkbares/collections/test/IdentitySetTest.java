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
package com.denkbares.collections.test;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.IdentitySet;

/**
 * Tests the special features of the IdentityTest
 *
 * @author Markus Friedrich (denkbares GmbH)
 * @created 15.03.2011
 */
public class IdentitySetTest {

	final Set<String> set = IdentitySet.newIdentityHashSet();

	@Test
	public void test() {
		Assert.assertTrue(set.isEmpty());
		// Attention: don't remove use of constructor,
		// we need it here to get different objects regardless of underlying JVM
		String s1 = new String("test");
		String s2 = new String("test");
		Assert.assertTrue(set.add(s1));
		Assert.assertFalse(set.add(s1));
		set.add(s2);
		Assert.assertEquals(2, set.size());
		Assert.assertTrue(set.contains(s2));
		Assert.assertTrue(set.remove(s2));
		Assert.assertFalse(set.remove(s2));
		// s2 is not contained, s1 is contained and equals s2, this should not
		// match
		Assert.assertFalse(set.contains(s2));
		set.clear();
		Assert.assertEquals(0, set.size());
	}
}
