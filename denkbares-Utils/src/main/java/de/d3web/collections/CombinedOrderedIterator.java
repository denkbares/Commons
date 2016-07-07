/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package de.d3web.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Implements an iterator that iterates over a set of other iterators with sorted elements. The
 * particular elements are mixed in a way that the resulting iteration is also sorted again.
 * <p>
 * <pre>
 *     CombinedIterator( [1,3,5] , [2,8,9], [4,6,7] ) --> [1,2,3,4,5,6,7,8,9]
 * </pre>
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 07.07.2016
 */
public class CombinedOrderedIterator<E> implements Iterator<E> {

	/**
	 * List of all iterators, without any empty iterator.
	 */
	private final List<PeekableIterator<E>> iterators = new LinkedList<>();
	private final Comparator<E> order;

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to their natural order, otherwise the iteration order
	 * is unpredictable. The order of the items is the natural order of the items. If &lt;E&gt; does
	 * not implement &lt;E&gt; an ClassCastException may be thrown during iteration.
	 *
	 * @param iterators the iterators to be combined
	 */
	public CombinedOrderedIterator(Collection<? extends Iterator<? extends E>> iterators) {
		//noinspection unchecked
		this(iterators, (Comparator<E>) Comparator.naturalOrder());
	}

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to the specified order, otherwise the iteration order
	 * is unpredictable.
	 *
	 * @param iterators the iterators to be combined
	 * @param order the order of the items
	 */
	public CombinedOrderedIterator(Collection<? extends Iterator<? extends E>> iterators, Comparator<E> order) {
		this(iterators.iterator(), order);
	}

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to their natural order, otherwise the iteration order
	 * is unpredictable. The order of the items is the natural order of the items. If &lt;E&gt; does
	 * not implement &lt;E&gt; an ClassCastException may be thrown during iteration.
	 *
	 * @param iterators the iterators to be combined
	 */
	public CombinedOrderedIterator(Stream<? extends Iterator<? extends E>> iterators) {
		//noinspection unchecked
		this(iterators, (Comparator<E>) Comparator.naturalOrder());
	}

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to the specified order, otherwise the iteration order
	 * is unpredictable.
	 *
	 * @param iterators the iterators to be combined
	 * @param order the order of the items
	 */
	public CombinedOrderedIterator(Stream<? extends Iterator<? extends E>> iterators, Comparator<E> order) {
		this(iterators.iterator(), order);
	}

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to their natural order, otherwise the iteration order
	 * is unpredictable. The order of the items is the natural order of the items. If &lt;E&gt; does
	 * not implement &lt;E&gt; an ClassCastException may be thrown during iteration.
	 *
	 * @param iterators the iterators to be combined
	 */
	public CombinedOrderedIterator(Iterator<? extends Iterator<? extends E>> iterators) {
		//noinspection unchecked
		this(iterators, (Comparator<E>) Comparator.naturalOrder());
	}

	/**
	 * Creates a new iterator that combines all the specified iterators into one. The iterators
	 * itself must already be sorted according to the specified order, otherwise the iteration order
	 * is unpredictable.
	 *
	 * @param iterators the iterators to be combined
	 * @param order the order of the items
	 */
	public CombinedOrderedIterator(Iterator<? extends Iterator<? extends E>> iterators, Comparator<E> order) {
		iterators.forEachRemaining(subset -> {
			if (subset.hasNext()) {
				this.iterators.add(new PeekableIterator<>(subset));
			}
		});
		this.order = order;
	}

	@Override
	public boolean hasNext() {
		// because of having no empty iterators in out list, we can simply check if we have any left
		return !iterators.isEmpty();
	}

	@Override
	public E next() {
		if (iterators.isEmpty()) throw new NoSuchElementException();

		// find iterator with best rating
		PeekableIterator<E> bestIterator = null;
		for (PeekableIterator<E> iterator : iterators) {
			if (bestIterator == null || order.compare(bestIterator.peek(), iterator.peek()) > 0) {
				bestIterator = iterator;
			}
		}

		// and use next method of that iterator
		// and also remove iterator if it will become empty
		assert bestIterator != null;
		E next = bestIterator.next();
		if (!bestIterator.hasNext()) iterators.remove(bestIterator);
		return next;
	}
}
