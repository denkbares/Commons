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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.denkbares.strings.PredicateParser;

/**
 * Utility class to convert a Condition into conjunctive normal form.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 11.04.2021
 */
public class Normalizer {
	// ------------------------------------------------------------------------
	// DNF - disjunctive normal form
	// ------------------------------------------------------------------------

	/**
	 * Return the disjunctive normal form of the condition, in clause form, as set of clause sets, where the outer sets
	 * are combined with "OR" and each inner set of atomic clauses are combined with "AND".
	 * <p>
	 * Example:
	 * <br><code>toDNF( a ) --> {{a}}</code>
	 * <br><code>toDNF( (a AND b AND c) OR (!d AND e) OR f ) --> {{a b c} {!d e} {f}}</code>
	 * <br><code>toDNF( !(a AND !b) ) --> {{!a} {b}}</code>
	 * <br><code>toDNF( !(a OR !b) ) --> {{!a b}}</code>
	 * <p>
	 * <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">See disjunctive normal form on wikipedia</a>.
	 *
	 * @param condition the condition to be converted
	 * @return the clause for of the disjunctive normal form of the condition
	 */
	public static Set<Set<Predicate<PredicateParser.ValueProvider>>> toDNF(Predicate<PredicateParser.ValueProvider> condition) throws PredicateParser.ParseException {
		// for constants, migrate to the actual value
		// but only if they are not current date conditions or evaluateDates holds
//		if (condition.isConstant() && (!(condition instanceof CurrentDateCondition) || evaluateDates)) {
//			Bool3 bool = condition.evaluate(EmptyContext.getInstance());
//			if (bool.isTrue()) return DNF_TRUE;
//			if (bool.isFalse()) return DNF_FALSE;
//		}

		// if condition is negated, push the negation to the atoms, and proceed with pushed one
		if (condition instanceof PredicateParser.NotPredicate) {
			condition = pushNegationsToAtoms(((PredicateParser.NotPredicate) condition).getNodes().get(0), true);
		}

		if (condition instanceof PredicateParser.OrPredicate) {
			// for an OR, create the DNF of the sub-conditions, and join the DNF sets (with optimization)
			SetBuilder dnf = new SetBuilder();
			for (Predicate<PredicateParser.ValueProvider> subCondition :
					((PredicateParser.OrPredicate) condition).getNodes()) {
				for (Set<Predicate<PredicateParser.ValueProvider>> disjunction : toDNF(subCondition)) {
					appendDNFConjunction(dnf, disjunction);
				}
			}
			return dnf.toSet();
		}

		if (condition instanceof PredicateParser.AndPredicate) {
			// Distribute ANDs inwards
			// create the DNF for each sub-condition -->
			// for each pair in the conjunctions sets, merge the disjunctions
			Iterator<Predicate<PredicateParser.ValueProvider>> iterator =
					((PredicateParser.AndPredicate) condition).getNodes()
							.iterator();
			// if empty return "TRUE" in clause form
			if (!iterator.hasNext()) {
				return Collections.singleton(Collections.singleton(new PredicateParser.TruePredicate()));
			}

			// start with the first operand
			Set<Set<Predicate<PredicateParser.ValueProvider>>> prev = toDNF(iterator.next());

			// and join each operand individually the the prev clause form
			while (iterator.hasNext()) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> next = toDNF(iterator.next());
				SetBuilder dnf = new SetBuilder();
				// merge each possible pair of prev and next
				for (Set<Predicate<PredicateParser.ValueProvider>> con1 : prev) {
					for (Set<Predicate<PredicateParser.ValueProvider>> con2 : next) {
						// create the paired disjunction, and add each pair to the dnf
						appendDNFConjunction(dnf, mergeAndCleanDNFConjunctions(con1, con2));
					}
				}
				// finally replace the prev with the merged dnf
				prev = dnf.toSet();
			}

			// prev is the original pref-cnf, or the merged cnf if there is more than one operand
			return prev;
		}

		// atom conditions can directly be represented in clause form
		return Collections.singleton(Collections.singleton(condition));
	}

	/**
	 * Merges a disjunction into a cnf, optimizing the resulting cnf. The specified cnf may be modified (!)
	 */
	private static void appendDNFConjunction(SetBuilder result,
											 Set<Predicate<PredicateParser.ValueProvider>> conjunction) {
		// false terms can be ignored
		if (conjunction.stream().allMatch(pred -> pred instanceof PredicateParser.FalsePredicate)) return;

		// if the conjunction is a superset of an existing one (in the result), the disjunction can be omitted
		// ((a && b) || (a && b && c)) --> ((a && b))
//		if (result.hasSubSetOf(conjunction)) return;

		// if the conjunction is a subset existing ones (in the result), the existing ones can be replaced
		// ((a && b && c) || (a && b && d) || (a && b)) --> ((a && b))
//		result.removeSuperSetsOf(conjunction);
		result.add(conjunction);
	}

	/**
	 * Merges two set of conjunctions (to be combined with an AND operator). Additionally optimizes the resulting set.
	 */
	private static Set<Predicate<PredicateParser.ValueProvider>> mergeAndCleanDNFConjunctions(Set<Predicate<PredicateParser.ValueProvider>> conjunction1, Set<Predicate<PredicateParser.ValueProvider>> conjunction2) {
		Set<Predicate<PredicateParser.ValueProvider>> con =
				new LinkedHashSet<>((conjunction1.size() + conjunction2.size()) * 2);
		con.addAll(conjunction1);
		con.addAll(conjunction2);
		// if TRUE is contained, all other atoms can be removed/skipped
		for (Predicate<PredicateParser.ValueProvider> pred : con) {
			if (pred instanceof PredicateParser.FalsePredicate) {
				return Collections.singleton(new PredicateParser.FalsePredicate());
			}
		}
		// clean true from the merged conjunction
		con.removeIf(pred -> pred instanceof PredicateParser.TruePredicate);
		return con;
	}

	// ------------------------------------------------------------------------
	// CNF - conjunctive normal form
	// ------------------------------------------------------------------------

	/**
	 * Return the conjunctive normal form of the condition, in clause form, as set of clause sets, where the outer sets
	 * are combined with "AND" and each inner set of atomic clauses are combined with "OR".
	 * <p>
	 * Example:
	 * <br><code>toCNF( a ) --> {{a}}</code>
	 * <br><code>toCNF( (a OR b OR c) AND (!d OR e) AND f ) --> {{a b c} {!d e} {f}}</code>
	 * <br><code>toCNF( !(a AND !b) ) --> {{!a b}}</code>
	 * <br><code>toCNF( !(a OR !b) ) --> {{!a} {b}}</code>
	 * <p>
	 * <a href="https://en.wikipedia.org/wiki/Conjunctive_normal_form">See conjunctive normal form on wikipedia</a>.
	 *
	 * @param condition the condition to be converted
	 * @return the clause for of the conjunctive normal form of the condition
	 */
	public static Set<Set<Predicate<PredicateParser.ValueProvider>>> toCNF(Predicate<PredicateParser.ValueProvider> condition) throws PredicateParser.ParseException {
		// for constants, migrate to the actual value
		// but only if they are not current date conditions or evaluateDates holds
//		if (condition.isConstant() && (!(condition instanceof CurrentDateCondition) || evaluateDates)) {
//			Bool3 bool = condition.evaluate(EmptyContext.getInstance());
//			if (bool.isTrue()) return CNF_TRUE;
//			if (bool.isFalse()) return CNF_FALSE;
//		}

		// if condition is negated, push the negation to the atoms, and proceed with pushed one
		if (condition instanceof PredicateParser.NotPredicate) {
			condition = pushNegationsToAtoms(((PredicateParser.NotPredicate) condition).getNodes().get(0), true);
		}

		if (condition instanceof PredicateParser.AndPredicate) {
			// for an AND, create the CNF of the sub-conditions, and join the conjunctions (with optimization)
			SetBuilder cnf = new SetBuilder();
			for (Predicate<PredicateParser.ValueProvider> subCondition :
					((PredicateParser.AndPredicate) condition).getNodes()) {
				for (Set<Predicate<PredicateParser.ValueProvider>> disjunction : toCNF(subCondition)) {
					appendCNFDisjunction(cnf, disjunction);
				}
			}
			return cnf.toSet();
		}

		if (condition instanceof PredicateParser.OrPredicate) {
			// Distribute ORs inwards
			// create the CNF for each sub-condition -->
			// for each pair in the disjunctions lists, merge the conjunctions
			Iterator<Predicate<PredicateParser.ValueProvider>> iterator =
					((PredicateParser.OrPredicate) condition).getNodes()
							.iterator();
			// if empty return "FALSE" in clause form
			if (!iterator.hasNext()) {
				return Collections.singleton(Collections.singleton(new PredicateParser.FalsePredicate()));
			}

			// start with the first operand
			Set<Set<Predicate<PredicateParser.ValueProvider>>> prev = toCNF(iterator.next());

			// and join each operand individually the the prev clause form
			while (iterator.hasNext()) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> next = toCNF(iterator.next());
				SetBuilder cnf = new SetBuilder();
				// merge each possible pair of prev and next
				for (Set<Predicate<PredicateParser.ValueProvider>> dis1 : prev) {
					for (Set<Predicate<PredicateParser.ValueProvider>> dis2 : next) {
						// create the paired disjunction, and add each pair to the cnf
						appendCNFDisjunction(cnf, mergeAndCleanCNFDisjunctions(dis1, dis2));
					}
				}
				// finally replace the prev with the merged cnf
				prev = cnf.toSet();
			}

			// prev is the original pref-cnf, or the merged cnf if there is more than one operand
			return prev;
		}

		// atom conditions can directly be represented in clause form
		return Collections.singleton(Collections.singleton(condition));
	}

	/**
	 * Merges a disjunction into a cnf, optimizing the resulting cnf. The specified cnf may be modified (!)
	 */
	private static void appendCNFDisjunction(SetBuilder result,
											 Set<Predicate<PredicateParser.ValueProvider>> disjunction) {
		// true terms can be ignored
		final boolean onlyTruePredicates = disjunction.stream()
				.allMatch(pred -> pred instanceof PredicateParser.TruePredicate);
		if (onlyTruePredicates) return;

		// if the disjunction is a superset of an existing one (in the result), the disjunction can be omitted
		// ((a||b) && (a||b||c)) --> ((a||b))
//		if (result.hasSubSetOf(disjunction)) return;

		// if the disjunction is a subset existing ones (in the result), the existing ones can be replaced
		// ((a||b||c) ((a||b||d)) && (a||b)) --> ((a||b))
//		result.removeSuperSetsOf(disjunction);
		result.add(disjunction);
	}

	/**
	 * Merges two set of disjunctions (to be combined with an OR operator). Additionally optimizes the resulting set.
	 */
	private static Set<Predicate<PredicateParser.ValueProvider>> mergeAndCleanCNFDisjunctions(Set<Predicate<PredicateParser.ValueProvider>> disjunction1, Set<Predicate<PredicateParser.ValueProvider>> disjunction2) {
		Set<Predicate<PredicateParser.ValueProvider>> dis =
				new LinkedHashSet<>((disjunction1.size() + disjunction2.size()) * 2);
		dis.addAll(disjunction1);
		dis.addAll(disjunction2);
		// if TRUE is contained, all other atoms can be removed/skipped
		for (Predicate<PredicateParser.ValueProvider> pred : dis) {
			if (pred instanceof PredicateParser.TruePredicate) {
				return Collections.singleton(new PredicateParser.TruePredicate());
			}
		}
		// clean false from the paired disjunction
		dis.removeIf(pred -> pred instanceof PredicateParser.FalsePredicate);
		return dis;
	}

	// ------------------------------------------------------------------------
	// internal utility methods
	// ------------------------------------------------------------------------

	private static Predicate<PredicateParser.ValueProvider> pushNegationsToAtoms(Predicate<PredicateParser.ValueProvider> condition, boolean negated) throws PredicateParser.ParseException {
		if (condition instanceof PredicateParser.NotPredicate) {
			return pushNegationsToAtoms(((PredicateParser.NotPredicate) condition).getNodes().get(0), !negated);
		}

//		if (condition instanceof ValueExistsCondition) {
//			// there is no negated atom-condition of exists, so negate the atom
//			return negated ? condition.negate() : condition;
//		}

		if (condition instanceof PredicateParser.CompareNode com) {
			// if no negation is required, return the original compare, as it is atomic
			if (!negated) return condition;
			// otherwise we negate by inverting the operator
			return new PredicateParser.CompareNode(com.getVariable(), com.getToken(), !com.isAcceptLess(),
					!com.isAcceptEqual(), !com.isAcceptGreater());
		}

//		if (condition instanceof CurrentDateCondition) {
//			// there is no negated atom-condition of exists, so negate the atom
//			return negated ? condition.negate() : condition;
//		}

//		if (condition instanceof Values.BoolOption) {
//			if (!negated) return condition;
//			return ((Values.BoolOption) condition).isTrue() ? new PredicateParser.FalsePredicate() : new
//			PredicateParser.TruePredicate();
//		}

		if (condition instanceof PredicateParser.AndPredicate) {
			// push negations further into sub-conditions
			List<Predicate<PredicateParser.ValueProvider>> operands =
					pushNegationsToAtoms(((PredicateParser.AndPredicate) condition).getNodes(), negated);
			// create new And/Or, reversed if currently negated
			return negated ? new PredicateParser.OrPredicate(operands) : new PredicateParser.AndPredicate(operands);
		}

		if (condition instanceof PredicateParser.OrPredicate) {
			// push negations further into sub-conditions
			List<Predicate<PredicateParser.ValueProvider>> operands =
					pushNegationsToAtoms(((PredicateParser.OrPredicate) condition).getNodes(), negated);
			// create new And/Or, reversed if currently negated
			return negated ? new PredicateParser.AndPredicate(operands) : new PredicateParser.OrPredicate(operands);
		}

		// and unknown condition --> fail
		throw new PredicateParser.ParseException(condition.toString(), 0);
	}

	private static List<Predicate<PredicateParser.ValueProvider>> pushNegationsToAtoms(List<Predicate<PredicateParser.ValueProvider>> conditions, boolean negated) throws PredicateParser.ParseException {
		List<Predicate<PredicateParser.ValueProvider>> pushed = new ArrayList<>(conditions.size());
		for (Predicate<PredicateParser.ValueProvider> condition : conditions) {
			pushed.add(pushNegationsToAtoms(condition, negated));
		}
		return pushed;
	}

	private static class SetBuilder {
		@SuppressWarnings("unchecked")
		private Set<Set<Predicate<PredicateParser.ValueProvider>>>[] containers = new Set[0];

		public Set<Set<Predicate<PredicateParser.ValueProvider>>> toSet() {
			Set<Set<Predicate<PredicateParser.ValueProvider>>> result = new LinkedHashSet<>(size() * 3 / 2);
			Arrays.stream(containers).filter(Objects::nonNull).forEach(result::addAll);
			return result;
		}

		public int size() {
			return Arrays.stream(containers).filter(Objects::nonNull).mapToInt(Set::size).sum();
		}

		public void add(Set<Predicate<PredicateParser.ValueProvider>> set) {
			getContainer(set).add(set);
		}

		public void remove(Set<Predicate<PredicateParser.ValueProvider>> set) {
			getContainer(set).remove(set);
		}

		private Set<Set<Predicate<PredicateParser.ValueProvider>>> getContainer(Set<Predicate<PredicateParser.ValueProvider>> set) {
			int index = set.size();
			if (index >= containers.length) {
				containers = Arrays.copyOf(containers, index + 1);
			}
			if (containers[index] == null) {
				containers[index] = new LinkedHashSet<>();
			}
			return containers[index];
		}

		public boolean hasSubSetOf(Set<Predicate<PredicateParser.ValueProvider>> superSet) {
			int to = Math.min(superSet.size(), containers.length);
			for (int index = 0; index < to; index++) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> container = containers[index];
				if (container == null) continue;
				for (Set<Predicate<PredicateParser.ValueProvider>> set : container) {
					if (superSet.containsAll(set)) return true;
				}
			}
			return false;
		}

		public void removeSubSetsOf(Set<Predicate<PredicateParser.ValueProvider>> superSet) {
			int to = Math.min(superSet.size(), containers.length);
			for (int index = 0; index < to; index++) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> container = containers[index];
				if (container == null) continue;
				container.removeIf(superSet::containsAll);
			}
		}

		public boolean hasSuperSetOf(Set<Predicate<PredicateParser.ValueProvider>> subSet) {
			int from = subSet.size() + 1;
			for (int index = from; index < containers.length; index++) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> container = containers[index];
				if (container == null) continue;
				for (Set<Predicate<PredicateParser.ValueProvider>> set : container) {
					if (set.containsAll(subSet)) return true;
				}
			}
			return false;
		}

		public void removeSuperSetsOf(Set<Predicate<PredicateParser.ValueProvider>> subSet) {
			int from = subSet.size() + 1;
			for (int index = from; index < containers.length; index++) {
				Set<Set<Predicate<PredicateParser.ValueProvider>>> container = containers[index];
				if (container == null) continue;
				container.removeIf(set -> set.containsAll(subSet));
			}
		}
	}
}
