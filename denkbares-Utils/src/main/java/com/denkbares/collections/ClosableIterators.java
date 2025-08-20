/*
 * Copyright (C) 2025 denkbares GmbH, Germany
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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Helper class for Closeable Iterators
 *
 * @author Friedrich Fell (Service Mate GmbH)
 * @created 19.08.2025
 */
public class ClosableIterators {
	private ClosableIterators() {
		// prevent initiation of this class
	}

	public static <T> CloseableIterator<T> emptyIterator() {
		return (CloseableIterator<T>) EmptyIterator.EMPTY_ITERATOR;
	}

	private static class EmptyIterator<E> implements CloseableIterator<E> {
		static final CloseableIterator<Object> EMPTY_ITERATOR = new EmptyIterator<>();

		public boolean hasNext() {
			return false;
		}

		public E next() {
			throw new NoSuchElementException();
		}

		public void remove() {
			throw new IllegalStateException();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
		}

		@Override
		public void close() {

		}
	}
}
