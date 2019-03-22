/*
 * Copyright (C) 2019 denkbares GmbH. All rights reserved.
 */

package com.denkbares.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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
		Map<T, Boolean> cache = new HashMap<>();
		return value -> cache.computeIfAbsent(value, v -> predicate.test(value));
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
}
