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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MultiMap;

public class ResultTableHierarchy {

	private final ResultTableModel data;
	private final List<TableRow> roots = new LinkedList<>();
	private final MultiMap<TableRow, TableRow> children = new DefaultMultiMap<>();
	private final Comparator<TableRow> comparator;
	public static final String SORT_VALUE = "sortValue";

	public ResultTableHierarchy(ResultTableModel data) {
		this.data = data;
		this.comparator = getComparator(data);
		init();
	}

	public List<TableRow> getRoots() {
		return this.roots.stream()
				.sorted(this.comparator)
				.collect(Collectors.toList());
	}

	public List<TableRow> getChildren(TableRow row) {
		return this.children.getValues(row).stream()
				.sorted(this.comparator)
				.collect(Collectors.toList());
	}

	private void init() {
		// the first column is supposed to be the row identifier = subject
		// build an index of all rows by subject
		// note that the subject may not be unique
		final Map<Value, List<TableRow>> rowsBySubject = new HashMap<>();
		final String subjectColumn = this.data.getVariables().get(0);
		for (TableRow row : this.data) {
			Value subject = row.getValue(subjectColumn);
			rowsBySubject.computeIfAbsent(subject, v -> new ArrayList<>()).add(row);
		}

		final String parentColumn = this.data.getVariables().get(1);
		for (TableRow tableRow : this.data) {
			Value parentId = tableRow.getValue(parentColumn);
			List<TableRow> parents = rowsBySubject.getOrDefault(parentId, Collections.emptyList());
			if (parents.isEmpty()) {
				this.roots.add(tableRow);
			}
			else {
				parents.forEach(parent -> this.children.put(parent, tableRow));
			}
		}
	}

	private static Comparator<TableRow> getComparator(final ResultTableModel result) {
		String sortColumn = result.getVariables()
				.stream()
				.filter(v -> v.equals(SORT_VALUE))
				.findAny()
				.orElse(result.getVariables().get(0));

		return (o1, o2) -> {
			// we sort by the column 'sortValue' if existing
			// otherwise we sort by URI
			Value sortValue1 = o1.getValue(sortColumn);
			Value sortValue2 = o2.getValue(sortColumn);

			// TODO : is there a better way to sort integer literals?
			final String sortString1 = sortValue1.toString();
			final String sortString2 = sortValue2.toString();
			final String xmlInt = "<http://www.w3.org/2001/XMLSchema#integer>";
			final String numRegex = "\"(\\d+)\".*$";
			if (sortString1.endsWith(xmlInt) && sortString2.endsWith(xmlInt)) {
				Pattern p = Pattern.compile(numRegex);
				final Matcher matcher = p.matcher(sortString1);
				matcher.find();
				final String intValueString1 = matcher.group(1);
				final Matcher matcher2 = p.matcher(sortString2);
				matcher2.find();
				final String intValueString2 = matcher2.group(1);
				return Integer.valueOf(intValueString1).compareTo(Integer.valueOf(intValueString2));
			}
			return sortString1.compareTo(sortString2);
		};
	}
}

