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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

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
		Map<T, R> cache = new ConcurrentHashMap<>();
		return value -> unwrap(cache.computeIfAbsent(value, v -> wrap(function.apply(value))));
	}

	/**
	 * Returns a cached version of the specified supplier, where the specified supplier is invoked only once for each
	 * value.
	 * <p>
	 * Note: The specified supplier should not have any side effects, and should return an equal instance for each
	 * input. If this is violated, the cached supplier may behave different to the uncached one.
	 *
	 * @param supplier the supplier to be cached
	 * @return a supplier, identical to the specified supplier, but caching the value
	 */
	public static <R> Supplier<R> cache(Supplier<R> supplier) {
		AtomicReference<R> cache = new AtomicReference<>();
		return () -> unwrap(cache.updateAndGet(prev -> (prev == null) ? wrap(supplier.get()) : prev));
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
