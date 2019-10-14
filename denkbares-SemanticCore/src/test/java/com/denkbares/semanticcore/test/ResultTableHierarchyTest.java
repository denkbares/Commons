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
import java.util.List;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Test;

import com.denkbares.semanticcore.utils.ResultTableHierarchy;
import com.denkbares.semanticcore.utils.ResultTableModel;
import com.denkbares.semanticcore.utils.TableRow;

import static junit.framework.TestCase.*;

/**
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 11.10.19
 */
public class ResultTableHierarchyTest {

	final static List<String> VARIABLES = Arrays.asList("rowid", "parent", "text");

	private TableRow addRow(ResultTableModel.Builder model, String rowId, String parent) {
		return addRow(model, rowId, parent, rowId.toUpperCase());
	}

	private TableRow addRow(ResultTableModel.Builder model, String rowId, String parent, String text) {
		Value rowIdValue = SimpleValueFactory.getInstance().createLiteral(rowId);
		Value parentValue = (parent != null) ?
				SimpleValueFactory.getInstance().createLiteral(parent) : null;
		Value textValue = SimpleValueFactory.getInstance().createLiteral(text);

		return model.addRow(rowIdValue, parentValue, textValue);
	}

	@Test
	public void test() {

		ResultTableModel.Builder tableModelBuilder = ResultTableModel.builder(VARIABLES);

		List<String> roots = Arrays.asList("a", "b");
		roots.forEach(root -> createNodes(tableModelBuilder, root, 2));
		ResultTableModel tableModel = tableModelBuilder.build();

		ResultTableHierarchy hierarchy = new ResultTableHierarchy(tableModel);

		TableRow aRoot = findRoot(hierarchy, "a");
		assertNotNull(aRoot);
		assertEquals("a", aRoot.getValue("rowid").stringValue());
		assertNull(aRoot.getValue("parent"));
		assertEquals("A", aRoot.getValue("text").stringValue());
		assertEquals(2, hierarchy.getChildren(aRoot).size());
		validateRow(hierarchy.getChildren(aRoot).get(0), "a0", "A0");
		validateRow(hierarchy.getChildren(aRoot).get(1), "a1", "A1");
	}

	@Test
	public void testChildInMultipleParents() {
		ResultTableModel.Builder tableModelBuilder = ResultTableModel.builder(VARIABLES);

		TableRow root0 = addRow(tableModelBuilder, "root", null, "Root0");
		TableRow root1 = addRow(tableModelBuilder, "root", null, "Root1");
		TableRow child = addRow(tableModelBuilder, "child", "root");

		ResultTableModel tableModel = tableModelBuilder.build();
		ResultTableHierarchy hierarchy = new ResultTableHierarchy(tableModel);
		List<TableRow> roots = hierarchy.getRoots();
		assertEquals(2, roots.size());
		assertSame(root0, roots.get(0));
		assertSame(root1, roots.get(1));
		assertSame(child, hierarchy.getChildren(root0).get(0));
		assertSame(child, hierarchy.getChildren(root1).get(0));
	}

	private void validateRow(TableRow row, String expectedRowId, String expectedText) {
		assertNotNull(row);
		assertEquals(expectedRowId, row.getValue("rowid").stringValue());
		assertEquals(expectedText, row.getValue("text").stringValue());
	}

	private TableRow findRoot(ResultTableHierarchy hierarchy, String rowId) {
		List<TableRow> roots = hierarchy.getRoots();
		return roots
				.stream()
				.filter(row -> {
					Value rowIdValue = row.getValue("rowid");
					return rowIdValue.stringValue().equals(rowId);
				})
				.findFirst()
				.orElse(null);
	}

	private void createNodes(ResultTableModel.Builder tableModel, String root, int numChildren) {
		for (int i = 0; i < numChildren; i++) {
			addRow(tableModel, root + i, root);
		}
		addRow(tableModel, root, null);
	}
}
