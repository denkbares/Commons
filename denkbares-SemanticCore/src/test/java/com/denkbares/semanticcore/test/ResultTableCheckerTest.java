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

package com.denkbares.semanticcore.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Assert;
import org.junit.Test;

import com.denkbares.semanticcore.utils.ResultTableChecker;
import com.denkbares.semanticcore.utils.ResultTableModel;
import com.denkbares.semanticcore.utils.TableRow;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 14.10.19
 */
public class ResultTableCheckerTest {

	final static Random RNG = new Random(42);

	private final static List<String> variables = Arrays.asList("a", "b", "c");

	@Test
	public void testCheckEqualityOnEqual() {

		int numRows = 10;
		List<TableRow> expectedRows = createRows(numRows, variables);
		List<TableRow> actualRows = createRows(numRows, variables);

		ResultTableModel expected = new ResultTableModel(expectedRows, variables);
		ResultTableModel actual = new ResultTableModel(actualRows, variables);

		List<ResultTableChecker.Failure> result = ResultTableChecker.checkEquality(expected, actual, false);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testCheckEqualityOnSuperset() {

		int numRows = 10;
		List<TableRow> expectedRows = createRows(numRows, variables);
		List<TableRow> actualRows = createRows(numRows, variables);

		// add additional rows to actual causing equality check to fail
		actualRows.add(createRowValues(17, variables));
		actualRows.add(createRowValues(42, variables));

		ResultTableModel expected = new ResultTableModel(expectedRows, variables);
		ResultTableModel actual = new ResultTableModel(actualRows, variables);

		List<ResultTableChecker.Failure> result = ResultTableChecker.checkEquality(expected, actual, false);
		Assert.assertFalse(result.isEmpty());
		List<ResultTableChecker.Failure> additionalRowsFound = getFailuresOfType(result, ResultTableChecker.FailureType.UNEXPECTED_ROW_FOUND);
		assertEquals(2, additionalRowsFound.size());
		assertTrue(additionalRowsFound.stream()
				.map(f -> f.getDetails().toString())
				.anyMatch(s -> s.contains("\"a@17\"")));
		assertTrue(additionalRowsFound.stream()
				.map(f -> f.getDetails().toString())
				.anyMatch(s -> s.contains("\"a@42\"")));
	}

	private static List<ResultTableChecker.Failure> getFailuresOfType(List<ResultTableChecker.Failure> allFailures, ResultTableChecker.FailureType type) {
		return allFailures.stream().filter(f -> f.getFailureType() == type).collect(Collectors.toList());
	}

	@Test
	public void testCheckEqualityOnSubSet() {

		int numRows = 10;
		List<TableRow> expectedRows = createRows(numRows, variables);
		List<TableRow> actualRows = createRows(numRows, variables);

		// remove rows to actual causing equality check to fail
		TableRow[] removedRows = new TableRow[] {
				actualRows.remove(RNG.nextInt(actualRows.size())),
				actualRows.remove(RNG.nextInt(actualRows.size())) };

		ResultTableModel expected = new ResultTableModel(expectedRows, variables);
		ResultTableModel actual = new ResultTableModel(actualRows, variables);

		List<ResultTableChecker.Failure> result = ResultTableChecker.checkEquality(expected, actual, false);
		Assert.assertFalse(result.isEmpty());
		List<ResultTableChecker.Failure> missingRowFailures = getFailuresOfType(result, ResultTableChecker.FailureType.EXPECTED_ROW_MISSING);
		assertEquals(removedRows.length, missingRowFailures.size());
		List<String> missingRows = missingRowFailures.stream()
				.map(f -> f.getDetails().toString())
				.collect(Collectors.toList());
		for (TableRow removed : removedRows) {
			assertTrue(missingRows.contains(removed.toString()));
		}
	}

	@Test
	public void testCheckEqualityOnDifferentRows() {

		int numRows = 10;
		List<TableRow> expectedRows = createRows(numRows, variables);

		// create n-1 equal rows
		List<TableRow> actualRows = createRows(numRows - 1, variables);
		actualRows.add(createRowValues(42, variables));
		// create a different last row
		Collections.shuffle(actualRows);

		ResultTableModel expected = new ResultTableModel(expectedRows, variables);
		ResultTableModel actual = new ResultTableModel(actualRows, variables);

		List<ResultTableChecker.Failure> result = ResultTableChecker.checkEquality(expected, actual, false);
		Assert.assertEquals(2, result.size());
		List<ResultTableChecker.Failure> missingRowFailures = getFailuresOfType(result, ResultTableChecker.FailureType.EXPECTED_ROW_MISSING);
		Assert.assertEquals(1, missingRowFailures.size());
		ResultTableChecker.Failure missingRowFailure = missingRowFailures.get(0);
		// the last row is different, so we expect it to be detected
		assertTrue(missingRowFailure.getDetails().toString().contains("a@9"));

		List<ResultTableChecker.Failure> additionalRowFailures = getFailuresOfType(result, ResultTableChecker.FailureType.UNEXPECTED_ROW_FOUND);
		assertEquals(1, additionalRowFailures.size());
		assertTrue(additionalRowFailures.get(0).getDetails().toString().contains("\"a@42\""));
	}

	@Test
	public void testCheckEqualityOnDifferentRowsForSameSubject() {

		int numRows = 10;
		List<TableRow> expectedRows = createRows(numRows, variables);

		List<TableRow> actualRows = createRows(numRows, variables);
		actualRows.add(createRowValues(1, variables));
		// create an additional row for existing subject
		Collections.shuffle(actualRows);

		ResultTableModel expected = new ResultTableModel(expectedRows, variables);
		ResultTableModel actual = new ResultTableModel(actualRows, variables);

		List<ResultTableChecker.Failure> result = ResultTableChecker.checkEquality(expected, actual, false);
		Assert.assertEquals(1, result.size());
		List<ResultTableChecker.Failure> mismatchFailures = getFailuresOfType(result, ResultTableChecker.FailureType.ROWS_FOR_SUBJECT_MISMATCH);
		Assert.assertEquals(1, mismatchFailures.size());
		ResultTableChecker.Failure mismatchFailure = mismatchFailures.get(0);
		ResultTableChecker.RowMismatch mismatchDetails = (ResultTableChecker.RowMismatch) mismatchFailure.getDetails();
		assertEquals("a@1", mismatchDetails.getSubject().stringValue());
		assertEquals(mismatchDetails.getExpected(), 1);
		assertEquals(mismatchDetails.getActual(), 2);
	}

	private List<TableRow> createRows(int numRows, List<String> variables) {
		List<TableRow> rows = new ArrayList<>(numRows);
		for (int i = 0; i < numRows; i++) {
			rows.add(createRowValues(i, variables));
		}
		// shuffle rows a bit to make comparision more demanding
		Collections.shuffle(rows, RNG);
		Collections.shuffle(rows, RNG);
		return rows;
	}

	private TableRow createRowValues(int i, List<String> variables) {
		SimpleValueFactory factory = SimpleValueFactory.getInstance();
		Map<String, Value> values = new HashMap<>(variables.size());
		variables.forEach(var -> values.put(var, factory.createLiteral(var + "@" + i)));
		return new TableRow(
				values, variables);
	}
}
