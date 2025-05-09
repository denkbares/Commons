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

package com.denkbares.collections;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Iterator that decorates an other iterator, but mapping its elements with a specified mapper
 * function.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 30.12.2014
 */
public class MappingIterator<S, E> implements Iterator<E> {

	private String name = null;
	private final Iterator<S> source;
	private final Function<? super S, ? extends E> mapper;

	/**
	 * Creates a new iterator that decorates an other iterator, but mapping its elements with the
	 * specified mapper function.
	 *
	 * @param source the iterator to get the original elements from
	 * @param mapper the mapping function, applied for each element during iteration
	 */
	public MappingIterator(Iterator<S> source, Function<? super S, ? extends E> mapper) {
		this.source = source;
		this.mapper = mapper;
	}

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	@Override
	public E next() {
		return mapper.apply(source.next());
	}

	@Override
	public void remove() {
		source.remove();
	}

	public MappingIterator<S, E> setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return (name == null ? "MappingIterator" : name) + "{" +
			   source +
			   '}';
	}
}
