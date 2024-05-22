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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.jetbrains.annotations.NotNull;

import com.denkbares.collections.SubSpanIterator;
import com.denkbares.semanticcore.TupleQueryResult;

public class ResultTableModel implements Iterable<TableRow> {

	public static class Builder {

		private final List<String> variables;
		private final List<TableRow> rows;

		public Builder(List<String> variables) {
			this.variables = variables;
			this.rows = new ArrayList<>();
		}

		public TableRow addRow(List<Value> columns) {
			TableRow row = createRowFromValues(columns, this.variables);
			this.rows.add(row);
			return row;
		}

		public ResultTableModel build() {
			return new ResultTableModel(this.rows, this.variables);
		}

		public TableRow addRow(Value... columns) {
			return addRow(Arrays.asList(columns));
		}

		public TableRow addRow(Map<String, Value> values) {
			TableRow row = createRowFromMap(values, this.variables);
			this.rows.add(row);
			return row;
		}
	}

	private final List<TableRow> rows;
	private final List<String> variables;

	public int getSize() {
		return this.rows.size();
	}

	public static Builder builder(List<String> variables) {
		return new Builder(variables);
	}

	public static ResultTableModel create(TupleQueryResult result) {
		final List<String> variables = result.getBindingNames();

		List<TableRow> rows = result.getBindingSets()
				.stream()
				.map(bs -> createRowFromBindingSet(bs, variables))
				.collect(Collectors.toList());
		return new ResultTableModel(rows, variables);
	}

	static TableRow createRowFromBindingSet(BindingSet bindings, List<String> variables) {
		return createRowFromBindings(bindings::getValue, variables);
	}

	static TableRow createRowFromBindings(Function<String, Value> bindings, List<String> variables) {
		Map<String, Value> values = new HashMap<>();
		variables.forEach(var -> {
			Value val = bindings.apply(var);
			if (val != null) {
				values.put(var, val);
			}
		});
		return new TableRow(values, variables);
	}

	static TableRow createRowFromMap(Map<String, Value> bindings, List<String> variables) {
		return createRowFromBindings(bindings::get, variables);
	}

	static TableRow createRowFromValues(List<Value> values, List<String> variables) {
		Map<String, Value> valueMap = new HashMap<>(values.size());
		Iterator<Value> valueIter = values.iterator();
		Iterator<String> varIter = variables.iterator();
		while (valueIter.hasNext()) {
			valueMap.put(varIter.next(), valueIter.next());
		}
		return new TableRow(valueMap, variables);
	}

	@Deprecated
	public ResultTableModel(@NotNull TupleQueryResult result) {
		this.variables = result.getBindingNames();
		this.rows = result.getBindingSets()
				.stream()
				.map(bs -> createRowFromBindingSet(bs, this.variables))
				.collect(Collectors.toList());
	}

	public ResultTableModel(List<TableRow> rows, List<String> variables) {
		this.variables = variables;
		this.rows = rows;
	}

	@NotNull
	@Override
	public Iterator<TableRow> iterator() {
		return this.rows.iterator();
	}

	public Stream<TableRow> rows() {
		return this.rows.stream();
	}

	/**
	 * Returns a sorted copy of the ResultTableModel, using the given comparator
	 *
	 * @param comparator the comparator to sort the model by
	 * @return a new sorted copy of the model
	 */
	@NotNull
	public final ResultTableModel sort(Comparator<TableRow> comparator) {
		List<TableRow> sortedRows = new ArrayList<>(this.rows);
		sortedRows.sort(comparator);
		return createResultTableModel(sortedRows, this.variables);
	}

	/**
	 * Filters the table model. The returned table model will only contain rows where all given patterns for all given
	 * columns are matching. The map keys are the column names, the values are the patterns that have to match for that
	 * column.
	 *
	 * @param filter a filter map with a set of Patterns to match for each column
	 * @return a filtered tabled
	 */
	public ResultTableModel filter(@NotNull Map<String, Set<Pattern>> filter) {
		if (filter.isEmpty()) return this; // shortcut

		List<TableRow> filteredRows = new ArrayList<>();
		rows:
		for (TableRow row : this.rows) {
			for (String variable : row.getVariables()) {
				Set<Pattern> patterns = filter.getOrDefault(variable, Collections.emptySet());
				if (patterns.isEmpty()) continue;
				Value value = row.getValue(variable);
				String stringValue = value == null ? "" : value.stringValue();
				if (patterns.stream().noneMatch(p -> p.matcher(stringValue).matches())) {
					continue rows;
				}
			}
			filteredRows.add(row);
		}
		return createResultTableModel(filteredRows, this.variables);
	}

	/**
	 * Filters the table model. The returned table model will contain all rows, but only the columns/cells that are not hidden.
	 *
	 * @param hiddenColumns a set
	 * @return a filtered tabled
	 */
	public ResultTableModel hideColumns(Set<String> hiddenColumns) {
		if (hiddenColumns.isEmpty()) return this; // shortcut

		List<TableRow> filteredRows = new ArrayList<>();
		ArrayList<String> variables = new ArrayList<>(this.getVariables());
		variables.removeIf(hiddenColumns::contains);
		for (TableRow row : this.rows) {
			LinkedHashMap<String, Value> values = new LinkedHashMap<>();
			for (String variable : variables) {
				values.put(variable, row.getValue(variable));
			}
			filteredRows.add(new TableRow(values, variables));
		}
		return createResultTableModel(filteredRows, variables);
	}

	protected ResultTableModel createResultTableModel(List<TableRow> rows, List<String> variables) {
		return new ResultTableModel(rows, variables);
	}

	/**
	 * Returns an iterator for a subset of the rows, starting from row 'start' inclusively (where 0
	 * is the first row) and end before row "end" (exclusively). If "start" is below 0, it will be
	 * assumed as 0. If "end" is above the current number of rows or end is below 0, it will be
	 * assumed to be the number of rows.
	 *
	 * @param start the first row to iterate
	 * @param end   the row to stop iteration before
	 * @return an iterator for the sub-span of rows
	 */
	public Iterator<TableRow> iterator(int start, int end) {
		return new SubSpanIterator<>(iterator(), start, end);
	}

	@Override
	public String toString() {
		StringBuilder buffy = new StringBuilder();
		buffy.append("Variables: ").append(this.variables).append("\n");
		for (TableRow tableRow : this.rows) {
			buffy.append(tableRow).append("\n");
		}
		return buffy.toString();
	}

	public List<String> getVariables() {
		return this.variables;
	}

	public String toCSV() throws IOException {
		StringWriter out = new StringWriter();
		CSVPrinter printer = CSVFormat.DEFAULT.withHeader(this.variables.toArray(new String[0]))
				.print(out);
		for (TableRow row : this.rows) {
			List<Object> values = new ArrayList<>(this.variables.size());
			for (String variable : this.variables) {
				Value value = row.getValue(variable);
				values.add(value == null ? null : value.stringValue());
			}
			printer.printRecord(values);
		}
		return out.toString();
	}

	public static ResultTableModel fromCSV(String csv) throws IOException {
		try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new StringReader(csv))) {

			// read the header
			Map<String, Integer> headerMap = parser.getHeaderMap();
			final List<String> variables = headerMap.entrySet().stream()
					.sorted(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey).collect(Collectors.toList());

			// read the rows
			List<TableRow> rows = new LinkedList<>();
			SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
			for (final CSVRecord record : parser) {
				Map<String, Value> rowValues = new HashMap<>(variables.size());
				for (String variable : variables) {
					String cell = record.get(variable);
					Value value;
					if (cell != null) {
						rowValues.put(variable, valueFactory.createLiteral(cell));
					}
				}
				rows.add(new TableRow(rowValues, variables));
			}

			//  and return the parsed table
			return new ResultTableModel(rows, variables);
		}
	}
}

