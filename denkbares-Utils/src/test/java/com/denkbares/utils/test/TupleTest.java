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
import com.denkbares.utils.Tuple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link Tuple}
 * 
 * @author Marc-Oliver Ochlast (denkbares GmbH)
 * @created 03.09.2010
 */
public class TupleTest {

	String stringObjectOne;
	Double doubleObjectTwo;
	Integer integerObjectThree;

	Tuple tupleUnderTest;

	@Before
	public void setUp() throws Exception {
		stringObjectOne = "stringObjectOne";
		doubleObjectTwo = 2.0;
		integerObjectThree = 3;

		tupleUnderTest = new NAryTuple(stringObjectOne, doubleObjectTwo, integerObjectThree);
	}

	/**
	 * Test method for {@link Tuple#hashCode()}.
	 */
	@Test
	public void testHashCode() {
		assertThat(tupleUnderTest.hashCode(), is(not(0)));
	}

	/**
	 * Test method for {@link Tuple#get(int)}.
	 */
	@Test
	public void testGet() {
		Object object = tupleUnderTest.get(0);
		assertThat(object instanceof String, is(true));
		assertThat(object, is("stringObjectOne"));

		object = tupleUnderTest.get(1);
		assertThat(object instanceof Double, is(true));
		assertThat(object, is(2.0));

		object = tupleUnderTest.get(2);
		assertThat(object instanceof Integer, is(true));
		assertThat(object, is(3));
	}

	/**
	 * Test method for {@link Tuple#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		assertThat(tupleUnderTest.equals(tupleUnderTest), is(true));
		assertThat(tupleUnderTest.equals(null), is(false));
		assertThat(tupleUnderTest.equals(stringObjectOne), is(false));

		// instantiate new tuple
		Tuple newTuple = new NAryTuple(stringObjectOne, doubleObjectTwo);
		assertThat(tupleUnderTest.equals(newTuple), is(false));
		newTuple = new NAryTuple(stringObjectOne, doubleObjectTwo, integerObjectThree);
		assertThat(tupleUnderTest.equals(newTuple), is(true));
	}

}
