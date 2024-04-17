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
import java.util.Collections;

import org.junit.Test;

import com.denkbares.collections.Matrix;
import com.denkbares.utils.Consoles;

import static org.junit.Assert.assertEquals;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 18.07.16
 */
public class MatrixTest {

	@Test
	public void basic() {
		Matrix<String> matrix = new Matrix<>();

		matrix.set(0, 0, "Hi");
		assertEquals("Hi", matrix.get(0, 0));
		assertEquals(1, matrix.getColSize());
		assertEquals(1, matrix.getRowSize());
		assertEquals(Collections.singletonList("Hi"), matrix.getRow(0));
		assertEquals(Collections.singletonList("Hi"), matrix.getColumn(0));


		matrix.set(3, 5, "What");

		assertEquals(6, matrix.getColSize());
		assertEquals(4, matrix.getRowSize());
		assertEquals(Arrays.asList("Hi", null, null, null, null, null), matrix.getRow(0));
		assertEquals(Arrays.asList("Hi", null, null, null), matrix.getColumn(0));

		assertEquals(Arrays.asList(null, null, null, null, null, "What"), matrix.getRow(3));
		assertEquals(Arrays.asList(null, null, null, "What"), matrix.getColumn(5));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void exception1() {
		new Matrix<>().get(0, -5);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void exception2() {
		new Matrix<>().get(-1, 0);
	}

	@Test
	public void dump() {
		Matrix<String> matrix = new Matrix<>();

		matrix.set(0, 0, "Hi");
		matrix.set(0, 1, "there");
		matrix.set(0, 2, "folks");

		matrix.set(0, 0, "Nothing");
		matrix.set(0, 1, "to");
		matrix.set(0, 2, Consoles.formatText("see here", Consoles.Decoration.REVERSED));

		matrix.dumpTable("Col 0","Header", Consoles.formatText("Formatted Header", Consoles.Color.CYAN));
	}
}
