/*
 * Copyright (C) 2023 denkbares GmbH, Germany
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

package com.denkbares.parser;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.strings.PredicateParser;
import com.denkbares.utils.Predicates;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 16.04.2021
 */
public class NormalizerCNFTest {

	/**
	 * not(a) --> (!a)
	 */
	@Test
	public void normalizeNot() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("NOT(a)");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Predicate<PredicateParser.ValueProvider> predicate = cnf.iterator().next().iterator().next();
		assertEquals(PredicateParser.BoolPredicate.class, predicate.getClass());
		Assert.assertTrue(((PredicateParser.BoolPredicate) predicate).isNegated());
		checkVariable(predicate, "a");
	}

	/**
	 * not(a || b) --> not(a) && not(b) --> (!a) && (!b)
	 */
	@Test
	public void normalizeNotOr() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("NOT(a || b)");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();
		final Predicate<PredicateParser.ValueProvider> notA = first.iterator().next();
		assertEquals(notA.getClass(), PredicateParser.BoolPredicate.class);
		assertTrue(((PredicateParser.BoolPredicate) notA).isNegated());
		checkVariable(notA, "a");

		final Set<Predicate<PredicateParser.ValueProvider>> second = iterator.next();
		final Predicate<PredicateParser.ValueProvider> notB = second.iterator().next();
		assertEquals(notB.getClass(), PredicateParser.BoolPredicate.class);
		assertTrue(((PredicateParser.BoolPredicate) notB).isNegated());
		checkVariable(notB, "b");
	}

	/**
	 * not(a && b) --> not(a) || not(b) --> (!a || !b)
	 */
	@Test
	public void normalizeNotAnd() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("NOT(a && b)");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();
		final Iterator<Predicate<PredicateParser.ValueProvider>> innterIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> notA = innterIterator.next();
		assertEquals(notA.getClass(), PredicateParser.BoolPredicate.class);
		assertTrue(((PredicateParser.BoolPredicate) notA).isNegated());
		checkVariable(notA, "a");
		final Predicate<PredicateParser.ValueProvider> notB = innterIterator.next();
		assertEquals(notB.getClass(), PredicateParser.BoolPredicate.class);
		assertTrue(((PredicateParser.BoolPredicate) notB).isNegated());
		checkVariable(notB, "b");
	}

	/**
	 * not(a && not(a || b)) --> not(a) || not(not(a)) || not(not(b))) --> (!a || a || b)
	 */
	@Test
	public void normalizeNotInner() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("NOT(a && NOT(a || b))");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();
		final Iterator<Predicate<PredicateParser.ValueProvider>> innterIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> notA = innterIterator.next();
		assertEquals(notA.getClass(), PredicateParser.BoolPredicate.class);
		assertTrue(((PredicateParser.BoolPredicate) notA).isNegated());
		checkVariable(notA, "a");
		final Predicate<PredicateParser.ValueProvider> a = innterIterator.next();
		assertEquals(a.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");
		final Predicate<PredicateParser.ValueProvider> b = innterIterator.next();
		assertEquals(b.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a && b) || (c && d) --> (a||c) && (b||c) && (a||d) && (b||d)
	 */
	@Test
	public void normalizeDistributed() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a && b) || (c && d)");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(a.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");
		final Predicate<PredicateParser.ValueProvider> c = firstIterator.next();
		assertEquals(c.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) c).isNegated());
		checkVariable(c, "c");

		final Iterator<Predicate<PredicateParser.ValueProvider>> thirdIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> a3 = thirdIterator.next();
		assertEquals(a3.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) a3).isNegated());
		checkVariable(a3, "a");
		final Predicate<PredicateParser.ValueProvider> d = thirdIterator.next();
		assertEquals(d.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) d).isNegated());
		checkVariable(d, "d");

		final Iterator<Predicate<PredicateParser.ValueProvider>> secondIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> b = secondIterator.next();
		assertEquals(b.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
		final Predicate<PredicateParser.ValueProvider> c2 = secondIterator.next();
		assertEquals(c2.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) c2).isNegated());
		checkVariable(c2, "c");

		final Iterator<Predicate<PredicateParser.ValueProvider>> fourthIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> b4 = fourthIterator.next();
		assertEquals(b4.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) b4).isNegated());
		checkVariable(b4, "b");
		final Predicate<PredicateParser.ValueProvider> d4 = fourthIterator.next();
		assertEquals(d4.getClass(), PredicateParser.BoolPredicate.class);
		assertFalse(((PredicateParser.BoolPredicate) d4).isNegated());
		checkVariable(d, "d");
	}

	/**
	 * (a || b) && true --> (a || b)
	 */
	@Test
	public void normalizeAndTrue() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a || b) && true");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, a.getClass());
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");

		final Predicate<PredicateParser.ValueProvider> b = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a && b) || true --> (true)
	 */
	@Test
	public void normalizeOrTrue() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a && b) || true");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.TruePredicate.class, a.getClass());
	}

	/**
	 * (a && true) || b --> (a || b)
	 */
	@Test
	public void normalizeInnerAndTrue() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a && true) || b");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, a.getClass());
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");

		final Predicate<PredicateParser.ValueProvider> b = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a || true) && b --> (b)
	 */
	@Test
	public void normalizeInnerOrTrue() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a || true) && b");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> b = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a || b) && false --> (false)
	 */
	@Test
	public void normalizeAndFalse() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a || b) && false");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.FalsePredicate.class, a.getClass());
	}

	/**
	 * (a && b) || false --> ((a) && (b))
	 */
	@Test
	public void normalizeOrFalse() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a && b) || false");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, a.getClass());
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");

		final Iterator<Predicate<PredicateParser.ValueProvider>> secondIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> b = secondIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a || false) && b --> ((a) && (b))
	 */
	@Test
	public void normalizeInnerOrFalse() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a || false) && b");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();
		final Set<Predicate<PredicateParser.ValueProvider>> first = iterator.next();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = first.iterator();
		final Predicate<PredicateParser.ValueProvider> a = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, a.getClass());
		assertFalse(((PredicateParser.BoolPredicate) a).isNegated());
		checkVariable(a, "a");

		final Iterator<Predicate<PredicateParser.ValueProvider>> secondIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> b = secondIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	/**
	 * (a && false) || b --> (b)
	 */
	@Test
	public void normalizeInnerAndFalse() throws PredicateParser.ParseException {
		final PredicateParser.ParsedPredicate parse = new PredicateParser().isBoolean(Predicates.TRUE())
				.parse("(a && false) || b");
		final Set<Set<Predicate<PredicateParser.ValueProvider>>> cnf = parse.toCNF();
		final Iterator<Set<Predicate<PredicateParser.ValueProvider>>> iterator = cnf.iterator();

		final Iterator<Predicate<PredicateParser.ValueProvider>> firstIterator = iterator.next().iterator();
		final Predicate<PredicateParser.ValueProvider> b = firstIterator.next();
		assertEquals(PredicateParser.BoolPredicate.class, b.getClass());
		assertFalse(((PredicateParser.BoolPredicate) b).isNegated());
		checkVariable(b, "b");
	}

	private static void checkVariable(Predicate<PredicateParser.ValueProvider> predicate, String expectedVariable) {
		predicate.test(variable -> {
			assertEquals(expectedVariable, variable);
			return Collections.emptyList();
		});
	}
}
