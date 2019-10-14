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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.jetbrains.annotations.NotNull;

import com.denkbares.semanticcore.TupleQueryResult;

/**
 * ResultTableModel that has keeps an additional index (hash lookup table) that allows to quickly find all rows matching
 * the key variable.
 *
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 11.10.19
 */
public class IndexedResultTableModel extends ResultTableModel {

	private final Map<Value, List<TableRow>> index;
	private final String keyVariable;

	public IndexedResultTableModel(@NotNull List<TableRow> rows, @NotNull List<String> variables, @NotNull String keyVariable) {
		super(rows, variables);
		this.keyVariable = keyVariable;
		this.index = buildIndex(rows, keyVariable);
	}

	private static Map<Value, List<TableRow>> buildIndex(List<TableRow> rows, String keyVariable) {
		final Map<Value, List<TableRow>> rowsByKey = new HashMap<>();
		rows.forEach(row -> {
			Value subject = row.getValue(keyVariable);
			rowsByKey.computeIfAbsent(subject, v -> new ArrayList<>()).add(row);
		});
		return rowsByKey;
	}

	public final List<TableRow> getRowsForKey(Value key) {
		return this.index.getOrDefault(key, Collections.emptyList());
	}

	public static IndexedResultTableModel create(TupleQueryResult result) {
		final List<String> variables = result.getBindingNames();
		final String keyVariable = variables.get(0);
		List<TableRow> rows = result.getBindingSets()
				.stream()
				.map(bs -> createRowFromBindingSet(bs, variables))
				.collect(Collectors.toList());
		return new IndexedResultTableModel(rows, variables, keyVariable);
	}

	public String getKeyVariable() {
		return this.keyVariable;
	}

	@Override
	protected ResultTableModel createResultTableModel(List<TableRow> rows, List<String> variables) {
		return new IndexedResultTableModel(rows, variables, this.keyVariable);
	}
}
