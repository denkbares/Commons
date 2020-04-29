/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * This class implements a typed, null-safe pair of two other objects.
 *
 * @author volker_belli
 */
public class Pair<T1, T2> extends Tuple {

	public Pair(T1 a, T2 b) {
		super(a, b);
	}

	@SuppressWarnings("unchecked")
	public T1 getA() {
		return (T1) get(0);
	}

	@SuppressWarnings("unchecked")
	public T2 getB() {
		return (T2) get(1);
	}

	@Override
	public String toString() {
		return "#Pair["
				+ getA() + "; "
				+ getB() + "]";
	}

	/**
	 * Applies the specified (bi-)consumer to the key and value of the pair.
	 *
	 * @param consumer the consumer to be applied
	 */
	public void apply(BiConsumer<T1, T2> consumer) {
		consumer.accept(getA(), getB());
	}

	/**
	 * Applies the specified (bi-)function to the key and value of the pair, returning the returned result.
	 *
	 * @param function the function to be applied
	 */
	public <R> R map(BiFunction<T1, T2, R> function) {
		return function.apply(getA(), getB());
	}

	/**
	 * Iterates the specified Iterable, applying the specified consumer to each pair of the specified container. This
	 * allows a deconstructing apply, similar for {@link java.util.Map#forEach}.
	 *
	 * @param collection the collection to iterate
	 * @param consumer   the bi-function to apply
	 */
	public static <T1, T2> void forEach(Iterable<Pair<T1, T2>> collection, BiConsumer<T1, T2> consumer) {
		for (Pair<T1, T2> pair : collection) {
			pair.apply(consumer);
		}
	}

	/**
	 * Iterates the specified iterator, applying the specified consumer to each pair of the specified iterator. This
	 * allows a deconstructing apply, similar for {@link java.util.Map#forEach}.
	 *
	 * @param iterator the iterator to iterate
	 * @param consumer the bi-function to apply
	 */
	public static <T1, T2> void forEach(Iterator<Pair<T1, T2>> iterator, BiConsumer<T1, T2> consumer) {
		while (iterator.hasNext()) {
			iterator.next().apply(consumer);
		}
	}
}
