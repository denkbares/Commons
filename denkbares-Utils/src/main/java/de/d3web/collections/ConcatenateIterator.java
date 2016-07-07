/*
 * Copyright (C) 2013 denkbares GmbH, Germany
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
package de.d3web.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Concatenates the items of multiple {@link Iterator}s into one Iterator. Please use the factory
 * methods {@link #flatMap(Iterator)}, {@link #flatMap(Iterator, Function)} and {@link
 * #concat(Iterator[])} instead of the constructor.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 14.01.2013
 */
public class ConcatenateIterator<T> implements Iterator<T> {

	private final Iterator<? extends Iterator<? extends T>> iterators;
	private Iterator<? extends T> current;

	/**
	 * Constructor is private, because of generic type conflicts otherwise.
	 */
	private ConcatenateIterator(Iterator<? extends Iterator<? extends T>> iterators) {
		this.iterators = iterators;
		this.current = this.iterators.hasNext() ? this.iterators.next() : null;
		// call hasNext to proceed to first item value of the preceding iterators are empty
		hasNext();
	}

	/**
	 * Concatenates the specified iterators into one iterator.
	 *
	 * @param iterators the iterators to be concatenated
	 * @param <T> the element type
	 * @return the concatenated iterator
	 */
	@SafeVarargs
	public static <T> Iterator<T> concat(Iterator<? extends T>... iterators) {
		return new ConcatenateIterator<T>(Arrays.asList(iterators).iterator());
	}

	/**
	 * Concatenates the specified iterators into one iterator.
	 *
	 * @param iterables the iterables to be concatenated
	 * @param <T> the element type
	 * @return the concatenated iterator
	 */
	@SafeVarargs
	public static <T> Iterator<T> concat(Iterable<? extends T>... iterables) {
		return new ConcatenateIterator<T>(Arrays.asList(iterables)
				.stream().map(Iterable::iterator).iterator());
	}

	/**
	 * Concatenates flattens the specified iterator of iterators into an plaint iterator. The first
	 * iterator will be completely processed, before the next iterator is consumed.
	 *
	 * @param iterators the iterators to be flatten
	 * @param <T> the element type
	 * @return the flat iterator for all elements
	 */
	public static <T> Iterator<T> flatMap(Iterator<? extends Iterator<? extends T>> iterators) {
		return new ConcatenateIterator<T>(iterators);
	}

	/**
	 * Concatenates flattens the specified iterator of iterators into an plaint iterator. The first
	 * iterator will be completely processed, before the next iterator is consumed.
	 *
	 * @param sources the iterator over the source objects
	 * @param mapper the mapping function, creating the to-be-flatten-iterators from the source
	 * items
	 * @param <T> the element type
	 * @return the flat iterator for all elements
	 */
	public static <T, S> Iterator<T> flatMap(Iterator<S> sources, Function<S, ? extends Iterator<T>> mapper) {
		return flatMap(new MappingIterator<S, Iterator<T>>(sources, mapper));
	}

	@Override
	public boolean hasNext() {
		// proceed to next iterator that has an element
		while (current != null && !current.hasNext()) {
			current = iterators.hasNext() ? iterators.next() : null;
		}
		return current != null;
	}

	@Override
	public T next() {
		if (!hasNext()) throw new NoSuchElementException();
		return current.next();
	}

	@Override
	public void remove() {
		current.remove();
	}
}
