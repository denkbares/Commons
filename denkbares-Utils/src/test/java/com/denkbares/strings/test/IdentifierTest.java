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

import java.util.Arrays;

import org.junit.Test;

import com.denkbares.strings.Identifier;
import com.denkbares.strings.Strings;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * Test for the critical conversion from {@link Identifier} to its external
 * form and back.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.04.2012
 */
public class IdentifierTest {

	@Test
	public void basic() {
		Identifier hi = new Identifier("Hi");
		Identifier extended = new Identifier(hi, "how", "are", "you");
		assertEquals(new Identifier("hi", "how", "are", "you"), extended);

		assertTrue(extended.startsWith(hi));

		assertEquals("you", extended.getLastPathElement());
		assertEquals(new Identifier("how", "are", "you"), extended.rest(hi));

		assertEquals(new Identifier("are", "you"), extended.rest(2));

		assertFalse(hi.isEmpty());

		assertEquals(4, extended.countPathElements());
	}

	@Test
	public void testFromExternalForm() {
		checkPath("");
		checkPath("\"");
		checkPath("\"", "\"");
		checkPath("\\");
		checkPath("termIdentifier");
		checkPath("termIdentifier\\");
		checkPath("\\termIdentifier");
		checkPath("\\termIdentifier\\");
		checkPath("termIdentifier\\\"");
		checkPath("\"\\termIdentifier");
		checkPath("\"\\termIdentifier\\\"");
		checkPath("term#Identifier");
		checkPath("term\\#Identifier");
		checkPath("term#Identifier", "#");
		checkPath("termI\"dentifier");
		checkPath("termI\\dentifier");
		checkPath("termI\"den\\tifier");
		checkPath("termI\\\"den\\tifier");
		checkPath("te\\\\rmI\"den\\\\ti\\fier");
		checkPath("\"termIdenti\"fier\"");
		checkPath("termIdentifier", "\"termIdenti\"fier\"");
		checkPath(" ", "termIdentifier", "\"termIdenti\"fier\"");
		checkPath("", "termIdentifier", "\"termIdenti\"fier\"");
		checkPath("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"");
		checkPath("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", "test");
		checkPath("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", " ");
		checkPath("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", "");
	}

	private void checkPath(String... pathElements) {
		Identifier termIdentifier = new Identifier(pathElements);
		String externalForm = termIdentifier.toExternalForm();
		Identifier fromExternalForm = Identifier.fromExternalForm(externalForm);

		String listOutput = Arrays.asList(termIdentifier.getPathElements()).toString();
		String listOutPutFromExternalForm = Arrays.asList(fromExternalForm.getPathElements()).toString();

		boolean equals = listOutput.equals(listOutPutFromExternalForm);
		System.out.println("equals: " + equals + " " + listOutput + " ==> " + externalForm + " ==> " + listOutPutFromExternalForm);
		assertTrue("Conversion from TermIdentifier to external form and back to TermIdentifier failed:\n"
				+ listOutput + " ==> " + externalForm + " ==> " + listOutPutFromExternalForm, equals);
	}

	@Test
	public void testConcatParse() {
		concatParse("\"", "\"");
		concatParse("term, Identifier", ", ");
		concatParse("term\\, Identifier", ", ");
		concatParse(" ", "termIdentifier", "\"termIdenti\"fier\"");
		concatParse("", "termIdentifier", "\"termIdenti\"fier\"");
		concatParse("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"");
		concatParse("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", "test");
		concatParse("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", " ");
		concatParse("asd\\\\lök", "termIdentifier", "\"termIdenti\"fier\"", "");
		concatParse("1, 1", "2", "3", "4, 4", "5\", \"5");
	}

	private void concatParse(String... pathElements) {
		concatParseDefault(pathElements);
		concatParseOptimized(pathElements);
	}

	private void concatParseDefault(String... pathElements) {
		String concat = Strings.concatParsable(", ", pathElements);
		String[] strings = Strings.parseConcat(", ", concat);

		String listOutput = Arrays.asList(pathElements).toString();
		String listOutPutFromExternalForm = Arrays.asList(strings).toString();

		boolean equals = listOutput.equals(listOutPutFromExternalForm);
		System.out.println("equals: " + equals + " " + listOutput + " ==> " + concat + " ==> " + listOutPutFromExternalForm);
		assertTrue("Concat and parse again failed:\n" + listOutput + " ==> " + concat + " ==> " + listOutPutFromExternalForm, equals);
	}

	private void concatParseOptimized(String... pathElements) {
		String concat = Strings.concatParsable(',', pathElements);
		String[] strings = Strings.parseConcat(",", concat);

		String listOutput = Arrays.asList(pathElements).toString();
		String listOutPutFromExternalForm = Arrays.asList(strings).toString();

		boolean equals = listOutput.equals(listOutPutFromExternalForm);
		System.out.println("equals: " + equals + " " + listOutput + " ==> " + concat + " ==> " + listOutPutFromExternalForm);
		assertTrue("Concat and parse again failed:\n" + listOutput + " ==> " + concat + " ==> " + listOutPutFromExternalForm, equals);
	}

	@Test
	public void testEquals() {
		Identifier caseInsensitiveUpper = new Identifier(false,"term", "Identifier");
		Identifier caseSensitiveUpper = new Identifier(true, "term", "Identifier");
		Identifier caseInsensitiveLower = new Identifier(false,"term", "identifier");
		Identifier caseSensitiveLower = new Identifier(true, "term", "identifier");

		// 1. same written identifiers should be the same
		assertEquals(caseInsensitiveUpper, caseSensitiveUpper);
		assertEquals(caseInsensitiveLower, caseSensitiveLower);

		// 2. check for case sensitivity
		assertEquals(caseInsensitiveUpper, caseInsensitiveLower);
		assertNotEquals(caseSensitiveUpper, caseSensitiveLower);

		Identifier caseInsensitiveUpperFromExternal = Identifier.fromExternalForm(caseInsensitiveUpper.toExternalForm());
		Identifier caseSensitiveUpperFromExternal = Identifier.fromExternalForm(caseSensitiveUpper.toExternalForm());
		Identifier caseInsensitiveLowerFromExternal = Identifier.fromExternalForm(caseInsensitiveLower.toExternalForm());
		Identifier caseSensitiveLowerFromExternal = Identifier.fromExternalForm(caseSensitiveLower.toExternalForm());

		// 3. identifier must be equals with the from external form identifier
		assertEquals(caseInsensitiveUpper, caseInsensitiveUpperFromExternal);
		assertEquals(caseSensitiveUpper, caseSensitiveUpperFromExternal);
		assertEquals(caseInsensitiveLower, caseInsensitiveLowerFromExternal);
		assertEquals(caseSensitiveLower, caseInsensitiveLowerFromExternal);

		// 4. check #1 and #2 again with identifier from external form
		assertEquals(caseInsensitiveUpperFromExternal, caseSensitiveUpperFromExternal);
		assertEquals(caseInsensitiveLowerFromExternal, caseSensitiveLowerFromExternal);
		assertEquals(caseInsensitiveUpperFromExternal, caseInsensitiveLowerFromExternal);
		assertNotEquals(caseSensitiveUpperFromExternal, caseSensitiveLowerFromExternal);
	}

	@Test
	public void testHashcode() {
		Identifier insensitive = new Identifier(false, "THIS", "is", "A", "test");
		Identifier sensitive = new Identifier(true, "THIS", "is", "A", "test");
		assertEquals(insensitive, sensitive);
		assertEquals(insensitive.hashCode(), sensitive.hashCode());

		Identifier insensitive2 = new Identifier(false, "THIS", "is", "A", "test");
		Identifier insensitive3 = new Identifier(false, "this", "is", "a", "test");
		assertEquals(insensitive2, insensitive3);
		assertEquals(insensitive2.hashCode(), insensitive3.hashCode());

		Identifier sensitive2 = new Identifier(true, "THIS", "is", "A", "test");
		Identifier sensitive3 = new Identifier(true, "this", "is", "a", "test");
		assertNotEquals(sensitive2, sensitive3);
		// sensitive2 and sensitive3 will have the same hash code, but that is ok for the hashcode contract
	}
}
