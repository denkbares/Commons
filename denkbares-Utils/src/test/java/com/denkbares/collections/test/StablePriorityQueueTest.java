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
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.denkbares.collections.StablePriorityQueue;

import static org.junit.Assert.assertEquals;

/**
 * Test {@link com.denkbares.collections.StablePriorityQueue}.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 15.03.17
 */
public class StablePriorityQueueTest {


	@Test
	public void testNormalPriorityQueue() {
		StablePriorityQueue<TestElement> queue = new StablePriorityQueue<>();
		queue.add(new TestElement("C", "C"));
		queue.add(new TestElement("A", "A"));
		queue.add(new TestElement("D", "D"));
		queue.add(new TestElement("B", "B"));

		assertEquals(4, queue.size());
		assertEquals("A", queue.peek().name);
		assertEquals("A", queue.poll().name);
		assertEquals("B", queue.poll().name);
		assertEquals("C", queue.peek().name);
		assertEquals("C", queue.poll().name);
		assertEquals("D", queue.poll().name);
	}

	@Test
	public void testNormalPriorityQueueWithComparator() {
		//noinspection Convert2Diamond needed here...
		StablePriorityQueue<TestElement> queue = new StablePriorityQueue<TestElement>(Comparator.reverseOrder());
		queue.add(new TestElement("C", "C"));
		queue.add(new TestElement("A", "A"));
		queue.add(new TestElement("D", "D"));
		queue.add(new TestElement("B", "B"));

		assertEquals(4, queue.size());
		assertEquals("D", queue.peek().name);
		assertEquals("D", queue.poll().name);
		assertEquals("C", queue.poll().name);
		assertEquals("B", queue.poll().name);
		assertEquals("A", queue.poll().name);
	}

	@Test
	public void testStablePriorityQueue() {
		StablePriorityQueue<TestElement> queue = new StablePriorityQueue<>();
		queue.add(new TestElement("C", "C"));
		queue.add(new TestElement("A1", "A"));
		queue.add(new TestElement("A2", "A"));
		queue.add(new TestElement("A3", "A"));
		queue.add(new TestElement("A4", "A"));
		queue.add(new TestElement("D", "D"));
		queue.add(new TestElement("B1", "B"));
		queue.add(new TestElement("B2", "B"));
		queue.add(new TestElement("B3", "B"));

		assertEquals(9, queue.size());
		assertEquals("A1", queue.peek().name);
		assertEquals("A1", queue.poll().name);
		assertEquals("A2", queue.peek().name);
		assertEquals("A2", queue.poll().name);
		assertEquals("A3", queue.poll().name);
		assertEquals("A4", queue.poll().name);
		assertEquals(5, queue.size());
		List<TestElement> list = new ArrayList<>();
		queue.iterator().forEachRemaining(list::add);
		assertEquals(5, list.size());
		assertEquals("B1", queue.poll().name);
		assertEquals("B2", queue.poll().name);
		assertEquals("B3", queue.peek().name);
		assertEquals("B3", queue.poll().name);
		assertEquals("C", queue.poll().name);
		assertEquals("D", queue.poll().name);
		assertEquals(0, queue.size());
	}

	private static final class TestElement implements Comparable<TestElement> {
		private final String name;
		private final String sort;

		public TestElement(String name, String sort) {
			this.name = name;
			this.sort = sort;
		}

		@Override
		public int compareTo(TestElement o) {
			return sort.compareTo(o.sort);
		}

		@Override
		public String toString() {
			return name + ": " + sort;
		}
	}

}
