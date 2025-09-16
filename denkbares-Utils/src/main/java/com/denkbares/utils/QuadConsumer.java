package com.denkbares.utils;

/**
 * Represents an operation that accepts four input arguments and returns no result. This is the four-arity
 * specialization of {@link java.util.function.Consumer}.
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {

	void accept(T t, U u, V v, W w);
}
