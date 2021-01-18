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

package com.denkbares.strings.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Predicate;

import org.junit.Test;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.strings.PredicateParser;
import com.denkbares.strings.PredicateParser.ParseException;
import com.denkbares.strings.PredicateParser.ParsedPredicate;
import com.denkbares.strings.PredicateParser.ValueBindings;
import com.denkbares.strings.PredicateParser.ValueProvider;
import com.denkbares.utils.Predicates;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 15.02.2020
 */
public class PredicateParserTest {
	@Test
	public void basic() throws ParseException {
		PredicateParser parser = new PredicateParser();
		Predicate<ValueProvider> expensive = parser.parse("price > '2.000,00 €'");
		Predicate<ValueProvider> light = parser.parse("weight <= 2");
		Predicate<ValueProvider> anyUSB = parser.parse("ports ~= '(?i).*USB.*'");
		Predicate<ValueProvider> and = parser.parse("weight >= 1 && weight <= 2 AND processor != i5");
		Predicate<ValueProvider> mixed = parser.parse("processor == i5 OR weight >= 1.5 && weight <= 2 OR ports = audio");
		Predicate<ValueProvider> brackets = parser.parse("((processor == i5 OR processor == i7) AND ((ports == audio)))");
		Predicate<ValueProvider> null1 = parser.parse("processor != null");
		Predicate<ValueProvider> null2 = parser.parse("win_version = null");
		Predicate<ValueProvider> null3 = parser.parse("win_version != 10");
		Predicate<ValueProvider> null4 = parser.parse("win_version == 10");
		Predicate<ValueProvider> portsNotEqual = parser.parse("ports != audio");

		DefaultMultiMap<String, String> values = new DefaultMultiMap<>();
		values.put("processor", "i7");
		values.put("ports", "usb3");
		values.put("ports", "audio");
		values.put("weight", "1.25");
		values.put("price", "2.795,00 €");

		// check the applicable ones
		assertTrue(expensive.test(values::getValues));
		assertTrue(light.test(values::getValues));
		assertTrue(anyUSB.test(values::getValues));
		assertTrue(and.test(values::getValues));
		assertTrue(mixed.test(values::getValues));
		assertTrue(brackets.test(values::getValues));
		assertTrue(null1.test(values::getValues));
		assertTrue(null2.test(values::getValues));
		assertTrue(null3.test(values::getValues));

		// check the non-applicable ones
		assertFalse(null4.test(values::getValues));
		assertFalse(portsNotEqual.test(values::getValues));
	}

	@Test
	public void customStopToken() throws ParseException {
		PredicateParser parser = new PredicateParser().stopToken("{");
		Predicate<ValueProvider> simple = parser.parse("weight <= 2 { Hello World }");
		Predicate<ValueProvider> noSpace = parser.parse("ports ~= '(?i).*USB{1}.*'{ Hello World }");
		Predicate<ValueProvider> brackets = parser.parse("((processor == i5 OR processor == i7) AND ((ports == audio))) {");

		DefaultMultiMap<String, String> values = new DefaultMultiMap<>();

		assertFalse(simple.test(values::getValues));
		assertFalse(noSpace.test(values::getValues));
		assertFalse(brackets.test(values::getValues));

		values.put("processor", "i7");
		values.put("ports", "usb3");
		values.put("ports", "audio");
		values.put("weight", "1.25");

		assertTrue(simple.test(values::getValues));
		assertTrue(noSpace.test(values::getValues));
		assertTrue(brackets.test(values::getValues));

		values.clear();
		values.put("processor", "i3");
		values.put("ports", "lightning");
		values.put("weight", "5");

		assertFalse(simple.test(values::getValues));
		assertFalse(noSpace.test(values::getValues));
		assertFalse(brackets.test(values::getValues));
	}

	@Test
	public void variables() throws ParseException {
		PredicateParser parser = new PredicateParser();
		ParsedPredicate tautology = parser.parse("true");
		ParsedPredicate expensive = parser.parse("price > '2.000,00 €'");
		ParsedPredicate anyUSB = parser.parse("ports ~= '(?i).*USB.*'");
		ParsedPredicate and = parser.parse("weight >= 1 && weight <= 2 AND processor != i5");
		ParsedPredicate mixed = parser.parse("processor == i5 OR weight >= 1.5 && weight <= 2 OR ports = audio");
		ParsedPredicate brackets = parser.parse("((processor == i5 OR processor == i7) AND ((ports == audio)))");

		assertTrue(tautology.getVariables().isEmpty());
		assertEquals(Collections.singletonList("price"), new ArrayList<>(expensive.getVariables()));
		assertEquals(Collections.singletonList("ports"), new ArrayList<>(anyUSB.getVariables()));
		assertEquals(Arrays.asList("weight", "processor"), new ArrayList<>(and.getVariables()));
		assertEquals(Arrays.asList("processor", "weight", "ports"), new ArrayList<>(mixed.getVariables()));
		assertEquals(Arrays.asList("processor", "ports"), new ArrayList<>(brackets.getVariables()));
	}

	@Test(expected = ParseException.class)
	public void forbiddenVariable() throws ParseException {
		PredicateParser parser = new PredicateParser().checkVariables("processor", "ports");
		parser.parse("price > '2.000,00 €'");
	}

	@Test(expected = ParseException.class)
	public void missingBracket() throws ParseException {
		new PredicateParser().parse("((processor == i5 OR processor == i7)");
	}

	@Test(expected = ParseException.class)
	public void missingClause() throws ParseException {
		new PredicateParser().parse("((processor == i5 OR");
	}

	@Test(expected = ParseException.class)
	public void earlyStopToken() throws ParseException {
		new PredicateParser().stopToken("{").parse("(processor == i5 { OR processor == i7)");
	}

	@Test
	public void bindings() throws ParseException {
		Predicate<ValueProvider> predicate = new PredicateParser().parse(
				"(processor == i5 OR weight >= 1.5) && (weight <= 2 OR ports = audio)");

		// try with empty bindings
		assertFalse(predicate.test(new ValueBindings()));

		// try with valid constant bindings
		ValueBindings values = new ValueBindings()
				.constant("processor", "i5")
				.constants("weight", "1")
				.constants("ports", Arrays.asList("audio", "USB"));
		assertTrue(predicate.test(values));

		// try with valid functional bindings
		values
				.value("processor", () -> "i5")
				.valueArray("weight", () -> new String[] { "1", "3" })
				.values("ports", () -> Arrays.asList("audio", "USB"));
		assertTrue(predicate.test(values));
	}

	@Test
	public void singleBounded() throws ParseException {
		Predicate<ValueProvider> predicate = new PredicateParser().parse(
				"(processor == i5 OR weight >= 1.5) && (weight <= 2 OR ports = audio)");

		// try with empty bindings
		HashMap<String, String> values = new HashMap<>();
		assertFalse(predicate.test(ValueProvider.singleBounded(values::get)));

		// try with valid constant bindings
		values.put("processor", "i5");
		values.put("weight", "1");
		values.put("ports", "audio");
		assertTrue(predicate.test(ValueProvider.singleBounded(values::get)));
	}

	@Test
	public void booleanVariables() throws ParseException {
		Predicate<ValueProvider> predicate = new PredicateParser()
				.isBoolean(Predicates.TRUE()).parse("(a OR b) && (c OR d)");

		// try with empty bindings
		ValueBindings values = new ValueBindings();
		assertFalse(predicate.test(values));

		// try with valid false constant bindings
		values.constant("a", "other");
		values.constant("b", "false");
		values.constant("c", "false");
		assertFalse(predicate.test(values));

		// try with valid true constant bindings
		values.constants("d", "false", null, "true");
		assertTrue(predicate.test(values));
	}

	@Test
	public void stringTest() throws ParseException{
		Predicate<ValueProvider> predicate = new PredicateParser()
				.isBoolean(Predicates.TRUE()).parse("serialnumber == 235AE02X");

		ValueBindings values = new ValueBindings();
		values.constant("serialnumber", "235AE02X");
		assertTrue(predicate.test(values));
	}

	@Test(expected = ParseException.class)
	public void failBooleanParsing() throws ParseException {
		new PredicateParser()
				.isBoolean("a", "b", "d") // "c" should not be a valid boolean, so the second "OR" is an invalid token
				.parse("(a OR b) && (c OR d)");
	}
}
