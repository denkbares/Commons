/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.strings.PredicateParser;
import com.denkbares.strings.PredicateParser.Node;
import com.denkbares.strings.PredicateParser.ParseException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 15.02.2020
 */
public class PredicateParserTest {
	@Test
	public void basic() throws ParseException {

		PredicateParser parser = new PredicateParser();

		Node expensive = parser.parse("price > '2.000,00 €'");
		Node light = parser.parse("weight <= 2");
		Node anyUSB = parser.parse("ports ~= '(?i).*USB.*'");
		Node and = parser.parse("weight >= 1 && weight <= 2 AND processor != i5");
		Node mixed = parser.parse("processor == i5 OR weight >= 1.5 && weight <= 2 OR ports = audio");
		Node brackets = parser.parse("((processor == i5 OR processor == i7) AND ((ports == audio)))");
		Node null1 = parser.parse("processor != null");
		Node null2 = parser.parse("win_version = null");
		Node null3 = parser.parse("win_version != 10");
		Node null4 = parser.parse("win_version == 10");
		Node portsNotEqual = parser.parse("ports != audio");

		DefaultMultiMap<String, String> values = new DefaultMultiMap<>();
		values.put("processor", "i7");
		values.put("ports", "usb3");
		values.put("ports", "audio");
		values.put("weight", "1.25");
		values.put("price", "2.795,00 €");

		// check the applicable ones
		assertTrue(expensive.eval(values::getValues));
		assertTrue(light.eval(values::getValues));
		assertTrue(anyUSB.eval(values::getValues));
		assertTrue(and.eval(values::getValues));
		assertTrue(mixed.eval(values::getValues));
		assertTrue(brackets.eval(values::getValues));
		assertTrue(null1.eval(values::getValues));
		assertTrue(null2.eval(values::getValues));
		assertTrue(null3.eval(values::getValues));

		// check the non-applicable ones
		assertFalse(null4.eval(values::getValues));
		assertFalse(portsNotEqual.eval(values::getValues));
	}

	@Test
	public void customStopToken() throws ParseException {

		PredicateParser parser = new PredicateParser("{");

		Node simple = parser.parse("weight <= 2 { Hello World }");
		Node noSpace = parser.parse("ports ~= '(?i).*USB{1}.*'{ Hello World }");
		Node brackets = parser.parse("((processor == i5 OR processor == i7) AND ((ports == audio))) {");

		DefaultMultiMap<String, String> values = new DefaultMultiMap<>();

		assertFalse(simple.eval(values::getValues));
		assertFalse(noSpace.eval(values::getValues));
		assertFalse(brackets.eval(values::getValues));

		values.put("processor", "i7");
		values.put("ports", "usb3");
		values.put("ports", "audio");
		values.put("weight", "1.25");

		assertTrue(simple.eval(values::getValues));
		assertTrue(noSpace.eval(values::getValues));
		assertTrue(brackets.eval(values::getValues));

		values.clear();
		values.put("processor", "i3");
		values.put("ports", "lightning");
		values.put("weight", "5");

		assertFalse(simple.eval(values::getValues));
		assertFalse(noSpace.eval(values::getValues));
		assertFalse(brackets.eval(values::getValues));
	}
}
