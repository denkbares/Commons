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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Test;

import com.denkbares.semanticcore.utils.ResultTableModel;
import com.denkbares.semanticcore.utils.TableRow;

import static org.junit.Assert.*;

/**
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 11.10.19
 */
public class ResultTableModelTest {
	private final static String[] VARIABLES = new String[] { "col1", "col2" };

	private TableRow addRow(ResultTableModel.Builder tableModel, String... columns) {

		List<Value> columnValues = Arrays.stream(columns)
				.map(v -> SimpleValueFactory.getInstance().createLiteral(v))
				.collect(Collectors
						.toList());
		return tableModel.addRow(columnValues);
	}

	@Test
	public void testMultipleEqualRows() {
		ResultTableModel.Builder tableModelBuilder = ResultTableModel.builder(Arrays.asList(VARIABLES));
		// add two equivalent rows
		TableRow rowA = addRow(tableModelBuilder, "a0", "a1");
		TableRow rowAStar = addRow(tableModelBuilder, "a0", "a1");
		// add another row
		TableRow rowB = addRow(tableModelBuilder, "b0", "b1");

		ResultTableModel tableModel = tableModelBuilder.build();
		//we expect different rows for equivalent content, i.e. same number of rows in model as calls to addRow
		assertEquals(3, tableModel.getSize());

		// verify that the rows in model are the same and in order as inserted
		Iterator<TableRow> actualIter = tableModel.iterator();
		Iterator<TableRow> expectedIter = Iterators.forArray(rowA, rowAStar, rowB);
		while (expectedIter.hasNext()) {
			assertSame(expectedIter.next(), actualIter.next());
		}
		assertFalse(actualIter.hasNext());
	}
}
