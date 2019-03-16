/*
 * Copyright (C) 2019 denkbares GmbH. All rights reserved.
 */

package com.denkbares.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to provide utilities for {@link java.util.function.Function}, {@link java.util.function.Predicate} and
 * similar.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 28.02.2019
 */
public class Functions {

	private static final Object NULL = new Object();

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
	 * Returns a cached version of the specified function, where the specified functions is invoked only once for each
	 * value.
	 * <p>
	 * Note: The specified function should not have any side effects, and should return a single defined value for each
	 * input that equals ({@link Object#equals(Object)}) to an other input. If this is violated, the cached function may
	 * behave different to the uncached one.
	 *
	 * @param function the function to cache the results
	 * @return a function, identical to the specified function, but caching the values
	 */
	public static <T, R> Function<T, R> cache(Function<T, R> function) {
		Map<T, R> cache = new HashMap<>();
		return value -> unwrap(cache.computeIfAbsent(value, v -> wrap(function.apply(value))));
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

	@NotNull
	private static <T> T wrap(@Nullable T nullable) {
		//noinspection unchecked
		return (nullable == null) ? (T) NULL : nullable;
	}

	@Nullable
	private static <T> T unwrap(@NotNull T notNull) {
		return (notNull == NULL) ? null : notNull;
	}
}
