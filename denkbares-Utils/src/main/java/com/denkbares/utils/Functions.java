/*
 * Copyright (C) 2019 denkbares GmbH. All rights reserved.
 */

package com.denkbares.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

	@NotNull
	private static <T> T wrap(@Nullable T nullable) {
		//noinspection unchecked
		return (nullable == null) ? (T) NULL : nullable;
	}

	@Nullable
	private static <T> T unwrap(@NotNull T notNull) {
		return (notNull == NULL) ? null : notNull;
	}

	/**
	 * Returns a "constant" Function, that always returns the specified value, for each parameter to be specified.
	 *
	 * @param value the value to be returned by the returned function
	 * @return the constant function with the specified return value
	 */
	public static <T, R> Function<T, R> constant(R value) {
		return (T t) -> value;
	}
}
