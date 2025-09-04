/*
 * Copyright (C) 2025 denkbares GmbH, Germany
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

package com.denkbares.util.nio;

import java.nio.file.Files;

import org.junit.Test;

import static java.nio.file.Paths.get;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 04.09.2025
 */
public class PathsTest {

	@Test
	public void isInSubTree() {
		var testPath = get("src", "test");
		var javaPath = get("src", "test", "java");
		var missing = javaPath.resolve("missing.txt");
		var existing = get("src", "test", "java", "com", "denkbares", "util", "nio", "PathsTest.java");

		assertTrue(Files.isDirectory(testPath));
		assertTrue(Files.isDirectory(javaPath));
		assertFalse(Files.exists(missing));
		assertTrue(Files.isRegularFile(existing));

		// test existing paths
		assertTrue(Paths.isInSubTree(javaPath, testPath));
		assertFalse(Paths.isInSubTree(testPath, javaPath));
		assertTrue(Paths.isInSubTree(javaPath.toAbsolutePath(), testPath));
		assertFalse(Paths.isInSubTree(testPath.toAbsolutePath(), javaPath));
		assertTrue(Paths.isInSubTree(javaPath, testPath.toAbsolutePath()));
		assertFalse(Paths.isInSubTree(testPath, javaPath.toAbsolutePath()));

		// test equal paths
		var absTestPath = javaPath.toAbsolutePath().getParent();
		assertTrue(Paths.isInSubTree(absTestPath, testPath));
		assertTrue(Paths.isInSubTree(testPath, absTestPath));

		// test non-existing paths
		assertTrue(Paths.isInSubTree(missing, testPath));
		assertFalse(Paths.isInSubTree(testPath, missing));
		assertTrue(Paths.isInSubTree(missing.toAbsolutePath(), testPath));
		assertFalse(Paths.isInSubTree(testPath.toAbsolutePath(), missing));
		assertTrue(Paths.isInSubTree(missing, testPath.toAbsolutePath()));
		assertFalse(Paths.isInSubTree(testPath, missing.toAbsolutePath()));

		// test files
		assertTrue(Paths.isInSubTree(existing, testPath));
		assertFalse(Paths.isInSubTree(testPath, existing));
		assertTrue(Paths.isInSubTree(existing.toAbsolutePath(), testPath));
		assertFalse(Paths.isInSubTree(testPath.toAbsolutePath(), existing));
		assertTrue(Paths.isInSubTree(existing, testPath.toAbsolutePath()));
		assertFalse(Paths.isInSubTree(testPath, existing.toAbsolutePath()));
	}
}