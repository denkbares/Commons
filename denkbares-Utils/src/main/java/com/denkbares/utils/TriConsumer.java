package com.denkbares.utils;

/**
 * Represents an operation that accepts three input arguments and returns no result. This is the three-arity
 * specialization of {@link java.util.function.Consumer}.
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

	void accept(T t, U u, V v);
}

