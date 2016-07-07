/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package de.d3web.collections;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Set implementation that adds a single element to an existing (decorated) set without modifying or
 * copying the set. If the added key is already in the decorated set, the value of the decorated set
 * seems to be overwritten by the added instance.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 20.05.2016
 */
public class ExtendedSet<E> extends AbstractSet<E> {

	private final E element;
	private final Set<E> decorated;

	public ExtendedSet(Set<E> decorated, E element) {
		this.decorated = decorated;
		this.element = element;
	}

	@NotNull
	@Override
	public Iterator<E> iterator() {
		// if the added element replaces an already existing one
		// replace the decorated one by the new one (even if both are equal, the are not identical)
		if (decorated.contains(element)) {
			return new MappingIterator<>(decorated.iterator(),
					e -> Objects.equals(e, element) ? element : e);
		}
		// otherwise, if the added element is new, extend the decorated set by one item
		//noinspection unchecked
		return ConcatenateIterator.concat(
				Collections.singleton(element).iterator(),
				decorated.iterator());
	}

	@Override
	public int size() {
		return decorated.contains(element) ? decorated.size() : decorated.size() + 1;
	}

	@Override
	public boolean contains(Object o) {
		return Objects.equals(o, element) || decorated.contains(o);
	}
}
