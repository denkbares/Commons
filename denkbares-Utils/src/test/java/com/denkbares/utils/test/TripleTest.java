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

import org.junit.Before;
import org.junit.Test;

import com.denkbares.utils.NAryTuple;
import com.denkbares.utils.Triple;
import com.denkbares.utils.Tuple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link Triple}
 *
 * @author Marc-Oliver Ochlast (denkbares GmbH)
 * @created 03.09.2010
 */
public class TripleTest {

	String stringObjectOne;
	Double doubleObjectTwo;
	Integer integerObjectThree;

	Triple<String, Double, Integer> tripleUnderTest;

	@Before
	public void setUp() throws Exception {
		stringObjectOne = "stringObjectOne";
		doubleObjectTwo = 2.0;
		integerObjectThree = 3;

		tripleUnderTest = new Triple<>(stringObjectOne,
				doubleObjectTwo, integerObjectThree);
	}

	/**
	 * Test method for {@link Triple#getA()}.
	 */
	@Test
	public void testGetA() {
		String objectA = tripleUnderTest.getA();
		assertThat(objectA, is(equalTo(stringObjectOne)));
	}

	/**
	 * Test method for {@link Triple#getB()}.
	 */
	@Test
	public void testGetB() {
		Double objectB = tripleUnderTest.getB();
		assertThat(objectB, is(equalTo(doubleObjectTwo)));
	}

	/**
	 * Test method for {@link Triple#getC()}.
	 */
	@Test
	public void testGetC() {
		Integer objectC = tripleUnderTest.getC();
		assertThat(objectC, is(equalTo(integerObjectThree)));
	}

	/**
	 * Test method for {@link Triple#toString()}.
	 */
	@Test
	public void testToString() {
		String string = tripleUnderTest.toString();
		assertThat(string, is("#Triple[stringObjectOne; 2.0; 3]"));
	}

	/**
	 * Make sure that all tuple implementations are interoperable with each other.
	 */
	@Test
	public void interoperability() {
		Triple<String, String, String> pair = new Triple<>("a", "b", "c");
		Tuple tuple = new NAryTuple("a", "b", "c");
		assertEquals(pair, tuple);
		assertEquals(pair.hashCode(), tuple.hashCode());
		assertEquals(pair.asList(), tuple.asList());
		assertEquals(pair.getSize(), tuple.getSize());
		assertEquals(pair.get(0), tuple.get(0));
		assertEquals(pair.get(1), tuple.get(1));
		assertEquals(pair.get(2), tuple.get(2));
	}
}
