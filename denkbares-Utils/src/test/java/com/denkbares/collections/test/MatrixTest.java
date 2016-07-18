package com.denkbares.collections.test;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.denkbares.collections.Matrix;

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
		new Matrix().get(0, -5);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void exception2() {
		new Matrix().get(-1, 0);
	}

}
