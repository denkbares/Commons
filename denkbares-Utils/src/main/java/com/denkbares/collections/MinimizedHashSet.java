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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * HashSet memory optimized for cases where you have a lot of them but most of the time with only one (or none) element.
 * As most sets will do (but not all), this implementation also supports to explicitly store a null values into the
 * set.
 * <p>
 * The implementation has a backup set, that is either an empty set or the item itself (not a singleton set, to save
 * memory), or a singleton set (if the item itself was a set), or a HashSet of the items.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 22.04.2017
 */
public class MinimizedHashSet<T> extends AbstractSet<T> {

	private static final Object EMPTY = new Object();
	private Object data = EMPTY;

	@Override
	public int size() {
		return (data == EMPTY) ? 0 : isPotentialWrappingSet(data) ? ((Set) data).size() : 1;
	}

	@Override
	public boolean contains(Object o) {
		return isPotentialWrappingSet(data) ? ((Set) data).contains(o) : Objects.equals(data, o);
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		//noinspection unchecked
		return new Iterator<T>() {

			private final Iterator<T> delegateIterator =
					(data == EMPTY) ? Collections.emptyIterator()
							: isPotentialWrappingSet(data) ? ((Set) data).iterator()
							: (Iterator<T>) Collections.singleton(data).iterator();
			private Object current = EMPTY;

			@Override
			public boolean hasNext() {
				return delegateIterator.hasNext();
			}

			@Override
			public T next() {
				// prepare if next() fails
				current = EMPTY;
				// get the next one
				current = delegateIterator.next();
				//noinspection unchecked
				return (T) current;
			}

			@Override
			public void remove() {
				if (current == EMPTY) throw new IllegalStateException();
				if (isPotentialWrappingSet(data)) {
					// if it is still a set, then remove by the iterator and unwrap is possible
					// because direct removal will lead to ConcurrentModificationException
					delegateIterator.remove();
					unwrapDelegate();
				}
				else {
					// but otherwise remove the item directly
					MinimizedHashSet.this.remove(current);
				}
				current = EMPTY;
			}
		};
	}

	@Override
	public boolean add(T item) {
		// if empty set, create singleton for the item
		if (data == EMPTY) {
			// we require to wrap a set around, if the item itself is a set, to not confuse this implementation
			if (isPotentialWrappingSet(item)) {
				wrapDelegate(item, 2);
				return true;
			}
			data = item;
			return true;
		}
		// if we have a single element, test the element (and stop if equal)
		// but otherwise create a set for the element and continue
		if (!isPotentialWrappingSet(data)) {
			// stop if same item added twice
			if (Objects.equals(data, item)) return false;
			// wrap the current item into a new set and continue to add the new item to the set
			wrapDelegate(data, 4);
		}
		// when we are here, we have at least one item,
		// and the items are wrapped in a modifiable hash set in the field delegate
		//noinspection unchecked
		return ((Set) data).add(item);
	}

	@Override
	public boolean remove(Object o) {
		// if set is empty, do nothing
		if (data == EMPTY) return false;
		// if we have one element, remove the element if equal
		if (!isPotentialWrappingSet(data)) {
			if (!Objects.equals(data, o)) return false;
			data = EMPTY;
			return true;
		}
		// otherwise we have a set
		// so remove the item and potentially fallback to single element or empty
		boolean removed = ((Set) data).remove(o);
		unwrapDelegate();
		return removed;
	}

	private void wrapDelegate(Object item, int capacity) {
		Set wrapper = createWrappingSet(capacity);
		//noinspection unchecked
		wrapper.add(item);
		data = wrapper;
	}

	/**
	 * Method to create a set of the specified initial capacity, to add the elements of this set
	 *
	 * @param capacity the initial capacity
	 * @return the created storage set to fill in the items of this set
	 */
	@NotNull
	protected Set<T> createWrappingSet(int capacity) {
		return new HashSet<>(capacity);
	}

	/**
	 * Returns true if the specified object is potentially the created wrapping set. Implementors should return true, if
	 * the object has the class of a created wrapping set.
	 *
	 * @param object the object to be tested
	 * @return true, if the object is potentially the wrapping set
	 */
	protected boolean isPotentialWrappingSet(Object object) {
		return (object != null) && (object.getClass() == HashSet.class);
	}

	private void unwrapDelegate() {
		Set set = (Set) data;
		if (set.isEmpty()) {
			data = EMPTY;
		}
		else if (set.size() == 1) {
			Object item = set.iterator().next();
			// fallback if the item is not a set itself, to not confuse this implementation
			if (!isPotentialWrappingSet(item)) {
				data = item;
			}
		}
	}

	@Override
	public void clear() {
		data = EMPTY;
	}
}
