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

package com.denkbares.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to provide utilities for {@link Predicate}s and similar.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 28.02.2019
 */
public class Predicates {

	/**
	 * Returns a new predicate instance that applies the key extractor function for each item and accept only those
	 * items that have a key that the predicate has not seen before. The method is useful e.g. to filter {@link
	 * java.util.stream.Stream}s so that for each key only one element occurs.
	 * <p>
	 * Note that even if the same object is tested a second time, it will be rejected.
	 *
	 * @param keyExtractor the key extractor
	 * @return the predicate to accept only one element for each key
	 */
	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	/**
	 * Returns a cached version of the specified predicate, where the specified functions is invoked only once for each
	 * value.
	 * <p>
	 * Note: The specified predicate should not have any side effects, and should return a single defined value for each
	 * input that equals ({@link Object#equals(Object)}) to an other input. If this is violated, the cached predicate
	 * may behave different to the uncached one.
	 *
	 * @param predicate the predicate to cache the results
	 * @return a predicate, identical to the specified function, but caching the values
	 */
	public static <T> Predicate<T> cache(Predicate<T> predicate) {
		Map<T, Boolean> cache = new ConcurrentHashMap<>();
		return value -> cache.computeIfAbsent(value, v -> predicate.test(value));
	}

	/**
	 * Returns a predicate that returns true for the first <code>limit</code> items that are tested. Duplicate items are
	 * only counted once. If the limit is "0", no items are accepted. If the limit is negative, all items are accepted.
	 *
	 * @param limit the number of items to be accepted
	 * @return the predicate that accepts the specified number of tested items
	 */
	public static <T> Predicate<T> limit(int limit) {
		// if no linit is specified, accept all items
		if (limit < 0) return TRUE();
		if (limit == 0) return FALSE();

		// create a set to count the already accepted items
		Set<T> accepted = new HashSet<>();
		return value -> {
			synchronized (accepted) {
				// if we already have enough accepted items in the in the set, only accept the existing items
				if (accepted.size() >= limit) return accepted.contains(value);
				// otherwise add the item and accept
				accepted.add(value);
				return true;
			}
		};
	}

	/**
	 * Returns a predicate that returns true for the first <code>limit</code> items that are accepted by the specified
	 * predicate. Duplicate items are only counted once. If the limit is "0", no items are accepted. If the limit is
	 * negative, all items are accepted.
	 *
	 * @param predicate the original predicate to decorate with a limit
	 * @param limit     the number of items to be accepted
	 * @return the predicate that only accepts the first specified number of items that are already accepted by the
	 * specified predicate
	 */
	public static <T> Predicate<T> limit(Predicate<T> predicate, int limit) {
		// if no limit is specified, use original predicate
		if (limit < 0) return predicate;
		// if limit is "0", accept nothing
		if (limit == 0) return FALSE();
		// otherwise add limit to the accepted items only
		return predicate.and(limit(limit));
	}

	/**
	 * Returns a predicate that is always true.
	 */
	public static <T> Predicate<T> TRUE() {
		return (T t) -> true;
	}

	/**
	 * Returns a predicate that is always false.
	 */
	public static <T> Predicate<T> FALSE() {
		return (T t) -> false;
	}

	/**
	 * Returns a predicate that negates the specified predicate. Even if each predicate has a negate method, this method
	 * may be handy if applying to to method references.
	 */
	@NotNull
	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return predicate.negate();
	}

	/**
	 * Returns a predicate that combines the specified predicates by an logical "AND". If the specified predicates are
	 * empty or null, true is returned.
	 */
	@NotNull
	@SafeVarargs
	public static <T> Predicate<T> and(Predicate<T>... predicates) {
		if (predicates == null) return TRUE();
		if (predicates.length == 0) return TRUE();
		if (predicates.length == 1) return predicates[0];
		return (T t) -> Arrays.stream(predicates).allMatch(p -> p.test(t));
	}

	/**
	 * Returns a predicate that combines the specified predicates by an logical "AND". If the specified predicates are
	 * empty or null, true is returned.
	 */
	@NotNull
	public static <T> Predicate<T> and(@Nullable Collection<Predicate<T>> predicates) {
		if (predicates == null) return TRUE();
		if (predicates.isEmpty()) return TRUE();
		if (predicates.size() == 1) return predicates.iterator().next();
		return (T t) -> predicates.stream().allMatch(p -> p.test(t));
	}

	/**
	 * Returns a predicate that combines the specified predicates by an logical "OR". If the specified predicates are
	 * empty or null, false is returned.
	 */
	@NotNull
	@SafeVarargs
	public static <T> Predicate<T> or(Predicate<T>... predicates) {
		if (predicates == null) return FALSE();
		if (predicates.length == 0) return FALSE();
		if (predicates.length == 1) return predicates[0];
		return (T t) -> Arrays.stream(predicates).anyMatch(p -> p.test(t));
	}

	/**
	 * Returns a predicate that combines the specified predicates by an logical "OR". If the specified predicates are
	 * empty or null, false is returned.
	 */
	@NotNull
	public static <T> Predicate<T> or(@Nullable Collection<Predicate<T>> predicates) {
		if (predicates == null) return FALSE();
		if (predicates.isEmpty()) return FALSE();
		if (predicates.size() == 1) return predicates.iterator().next();
		return (T t) -> predicates.stream().anyMatch(p -> p.test(t));
	}
}
