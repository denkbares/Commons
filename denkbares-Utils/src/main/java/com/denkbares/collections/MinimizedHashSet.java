/*
 * Copyright (C) 2017 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import com.denkbares.utils.EqualsUtils;

/**
 * HashSet memory optimized for cases where you have a lot of them but most of the time with
 * only one element.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 22.04.2017
 */
public class MinimizedHashSet<T> extends AbstractSet<T> {

	private static final Object EMPTY = new Object();
	private Object element = EMPTY;
	private HashSet<T> backUpSet = null;

	@Override
	public int size() {
		if (backUpSet != null) return backUpSet.size();
		if (element != EMPTY) return 1;
		return 0;
	}

	@Override
	public boolean contains(Object o) {
		if (backUpSet != null) return backUpSet.contains(o);
		return element != EMPTY && EqualsUtils.equals(element, o);
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			private Object current = EMPTY;
			boolean removable = false;
			private final Iterator<T> backupIterator = backUpSet == null ? null : backUpSet.iterator();

			@Override
			public boolean hasNext() {
				return (backupIterator != null && backupIterator.hasNext()) || (element != EMPTY && current != element);
			}

			@SuppressWarnings("unchecked")
			@Override
			public T next() {
				removable = true;
				if (backupIterator != null && backupIterator.hasNext()) {
					current = backupIterator.next();
					return (T) current;
				}
				if (current != element) {
					current = element;
					return (T) current;
				}
				removable = false;
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				if (!removable) throw new IllegalStateException();
				removable = false;
				if (current == element && element != EMPTY) {
					element = EMPTY;
					return;
				}
				if (backupIterator != null) {
					backupIterator.remove();
					if (backUpSet.size() == 1) {
						element = backupIterator.next();
						backUpSet = null;
					}
				}
			}
		};
	}

	@Override
	public boolean add(T t) {
		if (backUpSet != null) return backUpSet.add(t);
		if (element != EMPTY && EqualsUtils.equals(element, t)) {
			return false;
		}
		if (element != EMPTY && !EqualsUtils.equals(element, t)) {
			backUpSet = new HashSet<>(4);
			//noinspection unchecked
			backUpSet.add((T) element);
			backUpSet.add(t);
			element = EMPTY;
			return true;
		}
		element = t;
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (backUpSet != null) {
			boolean remove = backUpSet.remove(o);
			if (backUpSet.size() == 1) {
				element = backUpSet.iterator().next();
				backUpSet = null;
			}
			return remove;
		}
		if (element != EMPTY && EqualsUtils.equals(element, o)) {
			element = EMPTY;
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		backUpSet = null;
		element = EMPTY;
	}
}
