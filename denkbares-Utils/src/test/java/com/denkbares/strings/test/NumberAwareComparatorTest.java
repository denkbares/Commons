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

package com.denkbares.strings.test;

import org.junit.Test;

import static com.denkbares.strings.NumberAwareComparator.CASE_INSENSITIVE;
import static com.denkbares.strings.NumberAwareComparator.CASE_SENSITIVE;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 30.05.2016
 */
public class NumberAwareComparatorTest {
	@Test
	public void insensitive() {
		assertTrue(CASE_INSENSITIVE.compare("bla", "foo") < 0);
		assertTrue(CASE_INSENSITIVE.compare("Bla", "foo") < 0);
		assertTrue(CASE_INSENSITIVE.compare("bla", "Foo") < 0);
		assertTrue(CASE_INSENSITIVE.compare("Bla", "Foo") < 0);

		assertTrue(CASE_INSENSITIVE.compare("bla", "bla") == 0);
		assertTrue(CASE_INSENSITIVE.compare("Bla", "bla") == 0);
		assertTrue(CASE_INSENSITIVE.compare("bla", "Bla") == 0);
		assertTrue(CASE_INSENSITIVE.compare("Bla", "Bla") == 0);
	}

	@Test
	public void sensitive() {
		assertTrue(CASE_SENSITIVE.compare("bla", "foo") < 0);
		assertTrue(CASE_SENSITIVE.compare("Bla", "foo") < 0);
		assertTrue(CASE_SENSITIVE.compare("bla", "Foo") > 0);
		assertTrue(CASE_SENSITIVE.compare("Bla", "Foo") < 0);

		assertTrue(CASE_SENSITIVE.compare("bla", "bla") == 0);
		assertTrue(CASE_SENSITIVE.compare("Bla", "bla") != 0);
		assertTrue(CASE_SENSITIVE.compare("bla", "Bla") != 0);
		assertTrue(CASE_SENSITIVE.compare("Bla", "Bla") == 0);
	}

	@Test
	public void chapters() {
		assertTrue(CASE_INSENSITIVE.compare("3.1 Test", "4.1 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("4.1 Test", "3.1 Test") > 0);

		assertTrue(CASE_SENSITIVE.compare("3.1 Test", "4.1 test") < 0);
		assertTrue(CASE_SENSITIVE.compare("4.1 Test", "3.1 test") > 0);

		// hierarchical numbering
		assertTrue(CASE_INSENSITIVE.compare("3 Test", "3.1.1 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3. Test", "3.1.1 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3.1 Test", "3.1.1 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3.1.1 Test", "3.1.2 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3.3 Test", "3.45 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3.4 Test", "3.45 Test") < 0);
		assertTrue(CASE_INSENSITIVE.compare("3.5 Test", "3.45 Test") < 0);
	}

	@Test
	public void versionNumbers() throws Exception {
		assertTrue(CASE_INSENSITIVE.compare("1.0.1", "1.0.1") == 0);
		assertTrue(CASE_INSENSITIVE.compare("1.0.2", "1.0.1") > 0);
		assertTrue(CASE_INSENSITIVE.compare("1.0.1", "1.0.2") < 0);

		assertTrue(CASE_INSENSITIVE.compare("1.1.1", "1.0.1") > 0);
		assertTrue(CASE_INSENSITIVE.compare("1.0.1", "1.1.1") < 0);

		assertTrue(CASE_INSENSITIVE.compare("1.10.1", "1.2.1") > 0);
		assertTrue(CASE_INSENSITIVE.compare("1.2.1", "1.10.1") < 0);

		assertTrue(CASE_INSENSITIVE.compare("2.10", "2.10.1") < 0);
		assertTrue(CASE_INSENSITIVE.compare("2.10.1", "2.10") > 0);

		assertTrue(CASE_SENSITIVE.compare("1.0.1", "1.0.1") == 0);
		assertTrue(CASE_SENSITIVE.compare("1.0.2", "1.0.1") > 0);
		assertTrue(CASE_SENSITIVE.compare("1.0.1", "1.0.2") < 0);

		assertTrue(CASE_SENSITIVE.compare("1.1.1", "1.0.1") > 0);
		assertTrue(CASE_SENSITIVE.compare("1.0.1", "1.1.1") < 0);

		assertTrue(CASE_SENSITIVE.compare("1.10.1", "1.2.1") > 0);
		assertTrue(CASE_SENSITIVE.compare("1.2.1", "1.10.1") < 0);

		assertTrue(CASE_SENSITIVE.compare("2.10", "2.10.1") < 0);
		assertTrue(CASE_SENSITIVE.compare("2.10.1", "2.10") > 0);
	}

	@Test
	public void mixed() {
		assertTrue(CASE_INSENSITIVE.compare("file10", "file2") > 0);
		assertTrue(CASE_INSENSITIVE.compare("file10", "file2x") > 0);
		assertTrue(CASE_INSENSITIVE.compare("file10", "file20") < 0);

		assertTrue(CASE_INSENSITIVE.compare("file10", "file10x") < 0);
		assertTrue(CASE_INSENSITIVE.compare("file010", "file10x") < 0);
		assertTrue(CASE_INSENSITIVE.compare("file10", "file010x") < 0);
	}
}
