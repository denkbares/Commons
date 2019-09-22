/*
 * Copyright (C) 2019 denkbares GmbH. All rights reserved.
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
