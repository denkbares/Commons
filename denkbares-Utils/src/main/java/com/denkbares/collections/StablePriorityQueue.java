/*
 * Copyright (C) 2017 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.jetbrains.annotations.NotNull;

/**
 * Same as normal {@link PriorityQueue}, but preserves insertion order in case comparator classifies elements as equal.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 15.03.17
 */
public class StablePriorityQueue<T> extends AbstractQueue<T> {

	private final PriorityQueue<ElementWrapper<T>> priorityQueue;
	private long counter = 0; // potential overflow?

	public StablePriorityQueue() {
		//noinspection unchecked
		this((Comparator<T>) Comparator.<Comparable>naturalOrder());
	}

	public StablePriorityQueue(Comparator<T> comparator) {
		priorityQueue  = new PriorityQueue<>(new ItemCompare<>(comparator));
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return new MappingIterator<>(priorityQueue.iterator(), elementWrapper -> elementWrapper.element);
	}

	@Override
	public int size() {
		return priorityQueue.size();
	}

	@Override
	public boolean offer(T t) {
		return priorityQueue.offer(new ElementWrapper<>(t, counter++));
	}

	@Override
	public T poll() {
		ElementWrapper<T> poll = priorityQueue.poll();
		return poll == null ? null : poll.element;
	}

	@Override
	public T peek() {
		ElementWrapper<T> peek = priorityQueue.peek();
		return peek == null ? null : peek.element;
	}

	private static class ElementWrapper<T> {
		private final T element;
		private final long counter;

		private ElementWrapper(T element, long counter) {
			this.element = element;
			this.counter = counter;
		}
	}

	private static class ItemCompare<T> implements Comparator<ElementWrapper<T>> {

		private final Comparator<T> comparator;

		public ItemCompare(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(ElementWrapper<T> o1, ElementWrapper<T> o2) {
			int compare = comparator.compare(o1.element, o2.element);
			if (compare != 0) return compare;
			return Long.compare(o1.counter, o2.counter);
		}
	}
}
