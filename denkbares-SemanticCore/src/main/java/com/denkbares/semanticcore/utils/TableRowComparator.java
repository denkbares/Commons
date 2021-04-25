/*
 * Copyright (C) 2021 denkbares GmbH, Germany
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

import java.util.Comparator;
import java.util.List;

import org.eclipse.rdf4j.model.Value;

import com.denkbares.utils.Pair;

/**
 * Comparator for {@link TableRow}s of a {@link ResultTableModel}
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 24.04.21
 */
public class TableRowComparator implements Comparator<TableRow> {

	private final List<Pair<String, Comparator<Value>>> columnComparators;

	public TableRowComparator(List<Pair<String, Comparator<Value>>> columnComparators) {
		this.columnComparators = columnComparators;
	}

	@Override
	public int compare(TableRow o1, TableRow o2) {
		for (Pair<String, Comparator<Value>> columnComparator : columnComparators) {
			String variable = columnComparator.getA();
			Value v1 = o1.getValue(variable);
			Value v2 = o2.getValue(variable);
			int result = columnComparator.getB().compare(v1, v2);
			if (result != 0) return result;
		}
		return 0;
	}
}
