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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.DepthFirstOrder;
import com.denkbares.collections.MultiMap;

/**
 * Testing dept first order
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 22.09.2019
 */
public class DepthFirstOrderTest {

	@Test
	public void basic() {
		MultiMap<String, String> next = DefaultMultiMap.newLinked();
		next.putAll("A", "D", "B", "F");
		next.putAll("B", "C", "D");
		next.putAll("E", "F");
		next.putAll("F", "G");
		next.putAll("G", "F");

		DepthFirstOrder<String> order = new DepthFirstOrder<>(next::getValues, "A", "E");
		List<String> nodes = order.getOrderedNodes();
		Assert.assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F", "G"), nodes);
	}
}
