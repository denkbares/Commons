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
