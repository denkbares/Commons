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

package com.denkbares.semanticcore.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;

/**
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 11.10.19
 */
public class ResultTableChecker {

	public static final int MAX_DISPLAYED_FAILURES = 20;

	public enum FailureType {
		EXPECTED_ROW_MISSING, UNEXPECTED_ROW_FOUND, ROWS_FOR_SUBJECT_MISMATCH
	}

	public static class RowMismatch {
		private final Value subject;
		private final int expected;
		private final int actual;

		public RowMismatch(Value subject, int expected, int actual) {

			this.subject = subject;
			this.expected = expected;
			this.actual = actual;
		}

		public Value getSubject() {
			return this.subject;
		}

		public int getExpected() {
			return this.expected;
		}

		public int getActual() {
			return this.actual;
		}
	}

	/**
	 * Single finding in the result of the checkEquality method
	 */
	public static class Failure {
		private final FailureType failureType;
		private final Object details;

		Failure(FailureType errorType, Object details) {
			this.failureType = errorType;
			this.details = details;
		}

		public FailureType getFailureType() {
			return this.failureType;
		}

		public Object getDetails() {
			return this.details;
		}
	}

	/**
	 * Originally, to check an empty actual result, we had to define an expected result table with one empty cell, like
	 * <pre>
	 * %%ExpectedSparqlResult
	 * |
	 * %
	 * </pre>
	 * Since we now also check empty cells in the result, this is no longer a "clean" empty table. It is now possible to
	 * just have an %%ExpectedSparqlResult without a table, but to give backwards compatibility, we still handle
	 * expected tables with one empty cell as effectively an empty expected table.
	 */
	private static boolean isOutdatedExpectedTableWithOneEmptyCell(ResultTableModel expectedResultTable, TableRow expectedTableRow) {
		return expectedResultTable.getSize() == 1 && expectedTableRow.getVariables()
				.size() == 1 && expectedTableRow.getValue(expectedTableRow.getVariables().get(0)) == null;
	}

	/**
	 * Compares the two sparql result data tables with each other. Equality is checked if the atLeast-flag is set false.
	 * If the atLeast-flag is set to true, only the subset relation of expected to actual data is checked.
	 * <p/>
	 * CAUTION: result rows with blank nodes are ignored from consideration (graph isomorphism problem)
	 *
	 * @param expectedResultTable the expected data
	 * @param actualResultTable   the actual data
	 * @param atLeast             false: equality of data is required; true: expectedData SUBSET-OF actualData is
	 *                            required
	 * @return if the expected data is equal to (or a subset of) the actual data
	 * @created 20.01.2014
	 */
	public static List<Failure> checkEquality(ResultTableModel expectedResultTable, ResultTableModel actualResultTable, boolean atLeast) {
		final List<Failure> findings = new ArrayList<>();
		if (atLeast) {
			doCheckSubset(expectedResultTable, actualResultTable, findings);
		}
		else {
			doCheckEqual(expectedResultTable, actualResultTable, findings);
		}
		return findings;
	}

	/**
	 * Checks that each row in the expected table is contained in the actual table
	 */
	private static void doCheckSubset(ResultTableModel expectedResultTable, ResultTableModel actualResultTable, List<Failure> findings) {
		final List<TableRow> actualRows = actualResultTable.rows().collect(Collectors.toList());

		expectedResultTable.rows().filter(row -> !containsBlankNode(row)).forEach(expectedRow -> {
			int actualIndex = findRow(expectedRow, actualRows);
			if (actualIndex >= 0) {
				// we have found the row
				// remove it from the list so it won't be found again
				actualRows.remove(actualIndex);
			}
			else {

				if (!isOutdatedExpectedTableWithOneEmptyCell(expectedResultTable, expectedRow)) {
					// we could not find an expected row, so generate an error message
					findings.add(new Failure(FailureType.EXPECTED_ROW_MISSING, expectedRow));
				}
			}
		});
	}

	private static void doCheckEqual(ResultTableModel expectedResultTable, ResultTableModel actualResultTable, List<Failure> findings) {
		doCheckSubset(expectedResultTable, actualResultTable, findings);
		Map<Value, List<TableRow>> expectedData = createIndexBySubject(expectedResultTable);
		Map<Value, List<TableRow>> actualData = createIndexBySubject(actualResultTable);
		for (Map.Entry<Value, List<TableRow>> actual : actualData.entrySet()) {
			Value actualSubject = actual.getKey();
			List<TableRow> expectedRowsForSubject = expectedData.get(actualSubject);
			if (expectedRowsForSubject == null) {
				actual.getValue()
						.forEach(actualRow -> findings.add(new Failure(FailureType.UNEXPECTED_ROW_FOUND, actualRow)));
			}

			else {
				List<TableRow> actualRowsForSubject = actual.getValue();
				if (expectedRowsForSubject.size() != actualRowsForSubject.size()) {
					findings.add(new Failure(FailureType.ROWS_FOR_SUBJECT_MISMATCH, new RowMismatch(actualSubject, expectedRowsForSubject
							.size(), actualRowsForSubject.size())));
				}
			}
		}
	}

	private static boolean containsBlankNode(TableRow row) {
		return row.getVariables().stream().map(row::getValue).anyMatch(BNode.class::isInstance);
	}

	/**
	 * Create an index of the table rows  in the model by the subject (= first) variable)
	 */
	private static Map<Value, List<TableRow>> createIndexBySubject(ResultTableModel tableModel) {
		Map<Value, List<TableRow>> index = new HashMap<>();
		String subjectVariable = tableModel.getVariables().get(0);
		for (TableRow row : tableModel) {
			Value subject = row.getValue(subjectVariable);
			if (subject != null && !(subject instanceof BNode)) {
				index.computeIfAbsent(subject, v -> new ArrayList<>()).add(row);
			}
		}
		return index;
	}

	private static int findRow(TableRow search, List<TableRow> rows) {
		int i = 0;
		for (TableRow current : rows) {
			if (current.deepEquals(search)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static String generateErrorsText(List<Failure> failures) {
		return generateErrorsText(failures, true);
	}

	public static String generateErrorsText(List<Failure> failures, boolean full) {
		StringBuilder buffy = new StringBuilder();
		buffy.append("The following test failures occurred:\n");

		Map<FailureType, List<Failure>> failuresByType = new HashMap<>();
		failures.forEach(f -> failuresByType.computeIfAbsent(f.getFailureType(), v -> new ArrayList<>()).add(f));

		for (Map.Entry<FailureType, List<Failure>> failuresEntry : failuresByType.entrySet()) {
			List<Failure> failuresOfType = failuresEntry.getValue();
			String failureTypeName = getFailureTypeName(failuresEntry.getKey());

			buffy.append(failuresOfType.size()).append(" ").append(failureTypeName).append(":\n");
			int i = 0;
			for (Failure failure : failuresOfType) {
				if (!full && ++i > MAX_DISPLAYED_FAILURES) {
					buffy.append("* ... see expected and actual result linked below\n");
					break;
				}
				String message = getFailureDetails(failure);
				buffy.append("* ").append(message).append("\n");
			}
		}
		return buffy.toString();
	}

	private static String getFailureDetails(Failure failure) {
		FailureType type = failure.failureType;
		Object details = failure.details;
		switch (type) {
			case EXPECTED_ROW_MISSING:
				TableRow missingRow = (TableRow) details;
				return missingRow.toString();
			case UNEXPECTED_ROW_FOUND:
				TableRow additionalRow = (TableRow) details;
				return additionalRow.toString();
			case ROWS_FOR_SUBJECT_MISMATCH:
				RowMismatch mismatch = (RowMismatch) details;
				return mismatch.getSubject().stringValue();
			default:
				throw new IllegalArgumentException("Unexpected failure type " + type);
		}
	}

	private static String getFailureTypeName(FailureType type) {
		switch (type) {
			case EXPECTED_ROW_MISSING:
				return "expected rows missing";
			case UNEXPECTED_ROW_FOUND:
				return "unexpected rows found";
			case ROWS_FOR_SUBJECT_MISMATCH:
				return "cases where number of result columns does not match";
			default:
				throw new IllegalArgumentException("Unexpected failure type " + type);
		}
	}
}
