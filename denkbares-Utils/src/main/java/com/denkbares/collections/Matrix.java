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

package com.denkbares.collections;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import com.denkbares.strings.Strings;
import com.denkbares.utils.Consoles;
import com.denkbares.utils.Pair;

/**
 * Utility collection class that provides a two-dimensional array that dynamically expands in both dimensions as values
 * are added. Both dimensions have indices starting from 0, as usual for arrays.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 28.03.2014
 */
public class Matrix<E> {

	private final Map<Pair<Integer, Integer>, E> elements = new HashMap<>();
	private int rows = 0;
	private int cols = 0;

	/**
	 * Replaces the element at the specified position in this matrix with the specified element. If the current matrix
	 * is not big enough for the specified indices, it will be expanded in any direction to fit the indices.
	 *
	 * @param row     row of the element to replace
	 * @param col     col of the element to replace
	 * @param element element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException if any of the indices is negative
	 */
	public E set(int row, int col, E element) {
		rows = Math.max(rows, row + 1);
		cols = Math.max(cols, col + 1);
		return elements.put(getKey(row, col), element);
	}

	/**
	 * Removes all elements of this matrix. After this call the matrix is identical to a newly created one, with no cell
	 * contains any content and size is 0/0.
	 */
	public void clear() {
		rows = 0;
		cols = 0;
		elements.clear();
	}

	/**
	 * Returns the element that is stored at the specified position in this matrix. If the current matrix does not hold
	 * an element at the specified indices, null is returned.
	 *
	 * @param row row of the element to replace
	 * @param col col of the element to replace
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException if any of the indices is negative
	 */
	public E get(int row, int col) {
		return elements.get(getKey(row, col));
	}

	/**
	 * Returns the number of rows this matrix has. Therefore the valid row indices range from 0 inclusively to the
	 * returned value exclusively.
	 *
	 * @return the number of rows of this matrix
	 */
	public int getRowSize() {
		return rows;
	}

	/**
	 * Returns the number of columns this matrix has. Therefore the valid column indices range from 0 inclusively to the
	 * returned value exclusively.
	 *
	 * @return the number of columns of this matrix
	 */
	public int getColSize() {
		return cols;
	}

	/**
	 * Returns the entire row of this matrix as a list. The list has always the size returned by #getColSize(). If any
	 * elements are not set in the specified row, the items are encountered as null.
	 *
	 * @param row the row to return from this matrix
	 * @return the entire row as an array
	 */
	public List<E> getRow(int row) {
		int size = getColSize();
		List<E> result = new ArrayList<>(size);
		for (int col = 0; col < size; col++) {
			result.add(get(row, col));
		}
		return result;
	}

	/**
	 * Returns the entire col of this matrix as a list. The list has always the size returned by #getRowSize(). If any
	 * elements are not set in the specified col, the items are encountered as null.
	 *
	 * @param col the column to return from this matrix
	 * @return the entire column as an array
	 */
	public List<E> getColumn(int col) {
		int size = getRowSize();
		List<E> result = new ArrayList<>(size);
		for (int row = 0; row < size; row++) {
			result.add(get(row, col));
		}
		return result;
	}

	private Pair<Integer, Integer> getKey(int row, int col) {
		if (row < 0) throw new IndexOutOfBoundsException("row must not be negative");
		if (col < 0) throw new IndexOutOfBoundsException("col must not be negative");
		return new Pair<>(row, col);
	}

	/**
	 * Appends the specified matrix to this one, modifying this instance. The other matrix's row with row-index 0 of the
	 * specified table will be appended after the last row of this table, then other matrix's row with row-index 1, and
	 * so on.
	 *
	 * @param other the matrix to append to this one
	 */
	public void append(Matrix<E> other) {
		int thisRow = getRowSize();
		int rowSize = other.getRowSize();
		int colSize = other.getColSize();
		for (int otherRow = 0; otherRow < rowSize; thisRow++, otherRow++) {
			for (int col = 0; col < colSize; col++) {
				set(thisRow, col, other.get(otherRow, col));
			}
		}
	}

	/**
	 * Dumps the content of the Matrix to the console, as a human-readable ascii formatted table. The first row is
	 * assumed to contain the headings of the table, the column widths are adjusted to the content of each column.
	 */
	public void dumpTable() {
		dumpTable((List<String>) null);
	}

	/**
	 * Dumps the content of the Matrix to the console, as a human-readable ascii formatted table. The column widths are
	 * adjusted to the content of each column (including the header). If column headings is null, the first row is used
	 * as column headings.
	 *
	 * @param headings the column names
	 */
	public void dumpTable(String... headings) {
		dumpTable((headings == null) ? null : Arrays.asList(headings));
	}

	/**
	 * Dumps the content of the Matrix to the console, as a human-readable ascii formatted table. The column widths are
	 * adjusted to the content of each column (including the header). If column headings is null, the first row is used
	 * as column headings.
	 *
	 * @param headings the column names
	 */
	@SuppressWarnings({ "UseOfSystemOutOrSystemErr", "unused" })
	public void dumpTable(List<String> headings) {
		dump(headings, Integer.MAX_VALUE, System.out);
	}

	private void dump(List<String> headings, int maxRows, PrintStream out) {
		int firstRow = (headings == null) ? 0 : -1;
		int lastRow = Math.min(rows, maxRows); // excluding

		String[] cache = new String[lastRow * cols];
		BiFunction<Integer, Integer, String> textFun = (row, col) -> {
			if (row == -1 && headings != null) return (col < headings.size()) ? headings.get(col) : "";
			int index = row * cols + col;
			if (cache[index] == null) cache[index] = String.valueOf(get(row, col));
			return cache[index];
		};

		// prepare column lengths and alignment
		int[] lengths = new int[cols];
		boolean[] lefts = new boolean[cols];
		for (int row = firstRow; row < lastRow; row++) {
			for (int col = 0; col < cols; col++) {
				String text = Consoles.toPlainText(textFun.apply(row, col));
				lengths[col] = Math.max(lengths[col], text.length());
				if (row > firstRow) {
					lefts[col] |= !text.matches("\\s*(-*[0-9.,]*|\\?*|NaN|Infinity)\\s*");
				}
			}
		}

		// dump the matrix
		out.println();
		for (int row = firstRow; row < lastRow; row++) {
			for (int col = 0; col < cols; col++) {
				if (col > 0) out.print(" | ");
				String value = textFun.apply(row, col);
				String pad = Strings.nTimes(' ', lengths[col] - Consoles.toPlainText(value).length());
				if (!lefts[col]) out.print(pad);
				out.print(value);
				if (lefts[col]) out.print(pad);
			}
			out.println();
			if (row == firstRow) {
				int len = IntStream.of(lengths).map(x -> x + 3).sum() - 3;
				out.println(Strings.nTimes('=', len));
			}
		}
		if (lastRow < rows) {
			out.println("... (" + Strings.pluralOf(rows - lastRow, "more row") + ")");
		}
	}

	@Override
	public String toString() {
		return toString(5);
	}

	/**
	 * Dumps the first up to maxRows lines of the Matrix to a string, as a human-readable ascii formatted table. The
	 * column widths are adjusted to the content of each column (including the header). The first row is used as column
	 * headings.
	 *
	 * @param maxRows the maximum numbers of rows to be appended
	 */
	public String toString(int maxRows) {
		return toString(null, maxRows);
	}

	/**
	 * Dumps the content of the Matrix to a string, as a human-readable ascii formatted table. The column widths are
	 * adjusted to the content of each column (including the header). If column headings is null, the first row is used
	 * as column headings.
	 *
	 * @param headings the column names
	 */
	public String toString(String... headings) {
		return toString((headings == null) ? null : Arrays.asList(headings));
	}

	/**
	 * Dumps the content of the Matrix to a string, as a human-readable ascii formatted table. The column widths are
	 * adjusted to the content of each column (including the header). If column headings is null, the first row is used
	 * as column headings.
	 *
	 * @param headings the column names
	 */
	public String toString(List<String> headings) {
		return toString(headings, Integer.MAX_VALUE);
	}

	/**
	 * Dumps the first up to maxRows lines of the Matrix to a string, as a human-readable ascii formatted table. The
	 * column widths are adjusted to the content of each column (including the header). If column headings is null, the
	 * first row is used as column headings.
	 *
	 * @param headings the column names
	 * @param maxRows  the maximum numbers of rows to be appended
	 */
	private String toString(List<String> headings, int maxRows) {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (PrintStream out = new PrintStream(buffer, true, "UTF-8")) {
			dump(headings, maxRows, out);
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Returns a {@code Collector} implementing a convert-to-table operation on input elements of type {@code E},
	 * creating a row for each input element, starting with row 0, and a column for each of the specified
	 * columnExtractor function. The resulting matrix has columns numbered from 0 (inclusively) to
	 * columnExtractors.length (exclusively).
	 *
	 * @param <E>              the type of the input elements
	 * @param <R>              the type of the matrix's result elements
	 * @param columnExtractors the function mapping an input element to a column (cell) value
	 * @return a {@code Collector} implementing the convert-to-table operation
	 */
	@SuppressWarnings("unchecked")
	public static <E, R> Collector<E, ?, Matrix<R>> toMatrix(Function<E, R>... columnExtractors) {
		return toMatrix(Matrix::new, columnExtractors);
	}

	/**
	 * Returns a {@code Collector} implementing a convert-to-table operation on input elements of type {@code E},
	 * creating a row for each input element, starting with the first row behind the existing rows, and a column for
	 * each of the specified columnExtractor function. The resulting matrix has columns numbered from 0 (inclusively) to
	 * columnExtractors.length (exclusively).
	 *
	 * @param <E>              the type of the input elements
	 * @param <R>              the type of the matrix's result elements
	 * @param target           the existing matrix to append the rows to
	 * @param columnExtractors the function mapping an input element to a column (cell) value
	 * @return a {@code Collector} implementing the convert-to-table operation
	 */
	@SuppressWarnings("unchecked")
	public static <E, R> Collector<E, ?, Matrix<R>> toMatrix(Matrix<R> target, Function<E, R>... columnExtractors) {
		return toMatrix(() -> target, columnExtractors);
	}

	/**
	 * Returns a {@code Collector} implementing a convert-to-table operation on input elements of type {@code E},
	 * creating a row for each input element, starting with row 0, and a column for each of the specified
	 * columnExtractor function. The resulting matrix has columns numbered from 0 (inclusively) to
	 * columnExtractors.length (exclusively).
	 *
	 * @param <E>              the type of the input elements
	 * @param <R>              the type of the matrix's result elements
	 * @param supplier         the creator for the new matrix to fill the rows in
	 * @param columnExtractors the function mapping an input element to a column (cell) value
	 * @return a {@code Collector} implementing the convert-to-table operation
	 */
	@SuppressWarnings("unchecked")
	public static <E, R> Collector<E, ?, Matrix<R>> toMatrix(Supplier<Matrix<R>> supplier, Function<E, R>... columnExtractors) {
		return new Collector<E, Matrix<R>, Matrix<R>>() {

			@Override
			public Supplier<Matrix<R>> supplier() {
				return supplier;
			}

			@Override
			public BiConsumer<Matrix<R>, E> accumulator() {
				return this::accumulate;
			}

			private void accumulate(Matrix<R> matrix, E item) {
				int row = matrix.getRowSize();
				int col = 0;
				for (Function<E, R> column : columnExtractors) {
					matrix.set(row, col++, column.apply(item));
				}
			}

			@Override
			public BinaryOperator<Matrix<R>> combiner() {
				// append matrix2 to matrix1, behind the last row of matrix1
				return (matrix1, matrix2) -> {
					matrix1.append(matrix2);
					return matrix1;
				};
			}

			@Override
			public Function<Matrix<R>, Matrix<R>> finisher() {
				return Function.identity();
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Collections.singleton(Collector.Characteristics.IDENTITY_FINISH);
			}
		};
	}
}
