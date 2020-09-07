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

package com.denkbares.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class with hany methods to dealt with iterators.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 07.09.2020
 */
public class Iterators {

	/**
	 * Creates a java 8+ styled stream of the specified iterator. The stream is a non-parallel stream of ordered
	 * elements with unknown stream size. Note that the specified iterator is consumed through the stream and should be
	 * used any longer, after calling this method.
	 *
	 * @param source the iterator to create the stream from
	 * @return the stream of the iterator elements
	 */
	public static <E> Stream<E> stream(Iterator<? extends E> source) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(source, Spliterator.ORDERED), false);
	}

	/**
	 * Iterates the specified iterator and collects all elements into a list. The order of the list elements is the
	 * order of the elements, as they occur in the iterator. The returned list is mutable and can be modified by the
	 * caller afterwards. The specified iterator is completely consumed before this method returns.
	 *
	 * @param source the iterator to collect the elements from
	 * @return the list created from the elements
	 */
	public static <E> List<E> asList(Iterator<? extends E> source) {
		return asCollection(source, ArrayList::new);
	}

	/**
	 * Iterates the specified iterator and collects all elements into a set. Duplicate elements of the iterator are
	 * de-duplicated. The returned set is mutable and can be modified by the caller afterwards. The specified iterator
	 * is completely consumed before this method returns.
	 *
	 * @param source the iterator to collect the elements from
	 * @return the set created from the elements
	 */
	public static <E> Set<E> asSet(Iterator<? extends E> source) {
		return asCollection(source, HashSet::new);
	}

	/**
	 * Iterates the specified iterator and collects all elements into an ordered set. Duplicate elements of the iterator
	 * are de-duplicated. The order of the set elements is the order of the elements, as they occur the first time in
	 * the iterator. The returned set is mutable and can be modified by the caller afterwards. The specified iterator is
	 * completely consumed before this method returns.
	 *
	 * @param source the iterator to collect the elements from
	 * @return the ordered set created from the elements
	 */
	public static <E> Set<E> asOrderedSet(Iterator<? extends E> source) {
		return asCollection(source, LinkedHashSet::new);
	}

	/**
	 * Iterates the specified iterator and collects all elements into a collection. The collection is created through
	 * the specified supplier function. The specified iterator is completely consumed before this method returns.
	 *
	 * @param source the iterator to collect the elements from
	 * @return the set created from the elements
	 */
	public static <E, C extends Collection<E>> C asCollection(Iterator<? extends E> source, Supplier<C> factory) {
		C result = factory.get();
		source.forEachRemaining(result::add);
		return result;
	}
}
