/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg denkbares GmbH
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

/*
 * Created on 22.07.2003
 */
package com.denkbares.progress;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

/**
 * A class can implement this interface, so that other classes can inform it about their progress
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
@FunctionalInterface
public interface ProgressListener {

	/**
	 * Updates this ProgressListener to a new completion value. The percent must be between 0.0 and 1.0.
	 *
	 * @param percent the actual percentage of the progress
	 * @param message a message containing information about the actual state
	 */
	void updateProgress(float percent, String message);

	/**
	 * Returns a (new) progress listener that maps its 0%..100% into the defined span of this progress listener. This is
	 * useful to provide some progress for a partial activity that takes a well-defined part of the total progress.
	 *
	 * @param startPercent the percentage of this progress listener to start with, when the returned progress listener
	 *                     is updated to 0%
	 * @param endPercent   the percentage of this progress listener to end at, when the returned progress listener is
	 *                     updated to 100%
	 * @return the new progress listener that maps into the span
	 */
	@NotNull
	default ProgressListener span(float startPercent, float endPercent) {
		return (percent, message) -> updateProgress(
				percent * (endPercent - startPercent) + startPercent, message);
	}

	/**
	 * When iterating over the returned iterable, the progress is automatically updated fom 0% for the first item to
	 * 100% minus 1 item, when iterating over the last item. As the progress message, the toString verbalization of the
	 * iterated items are used. Usually you use this in the following manner:
	 * <p>
	 * <pre>
	 * for (T item : progress.iterate(arrayOfT)) { ... }
	 * progress.update(1f, "done");
	 * </pre>
	 *
	 * @param items the items to iterate over
	 * @return a iterable over the items that automatically increases the progress
	 */
	@NotNull
	default <T> Iterable<T> iterate(T[] items) {
		return iterate(items, String::valueOf);
	}

	/**
	 * When iterating over the returned iterable, the progress is automatically updated fom 0% for the first item to
	 * 100% minus 1 item, when iterating over the last item. As the progress message, the specified message function is
	 * called for each item. Usually you use this in the following manner:
	 * <p>
	 * <pre>
	 * for (T item : progress.iterate(arrayOfT, t -> ...)) { ... }
	 * progress.update(1f, "done");
	 * </pre>
	 *
	 * @param items the items to iterate over
	 * @return a iterable over the items that automatically increases the progress
	 */
	@NotNull
	default <T> Iterable<T> iterate(T[] items, Function<T, String> message) {
		return () -> iterate(Stream.of(items).iterator(), items.length, message);
	}

	/**
	 * When iterating over the returned iterable, the progress is automatically updated fom 0% for the first item to
	 * 100% minus 1 item, when iterating over the last item. As the progress message, the toString verbalization of the
	 * iterated items are used. Usually you use this in the following manner:
	 * <p>
	 * <pre>
	 * for (T item : progress.iterate(myCollectionOfT)) { ... }
	 * progress.update(1f, "done");
	 * </pre>
	 *
	 * @param items the items to iterate over
	 * @return a iterable over the items that automatically increases the progress
	 */
	@NotNull
	default <T> Iterable<T> iterate(Collection<T> items) {
		return iterate(items, String::valueOf);
	}

	/**
	 * When iterating over the returned iterable, the progress is automatically updated fom 0% for the first item to
	 * 100% minus 1 item, when iterating over the last item. As the progress message, the specified message function is
	 * called for each item. Usually you use this in the following manner:
	 * <p>
	 * <pre>
	 * for (T item : progress.iterate(myCollectionOfT, t -> ...)) { ... }
	 * progress.update(1f, "done");
	 * </pre>
	 *
	 * @param items the items to iterate over
	 * @return a iterable over the items that automatically increases the progress
	 */
	@NotNull
	default <T> Iterable<T> iterate(Collection<T> items, Function<T, String> message) {
		return () -> iterate(items.iterator(), items.size(), message);
	}

	/**
	 * When iterating over the returned iterable, the progress is automatically updated fom 0% for the first item to
	 * 100% minus 1 item, when iterating over the last item. As the progress message, the specified message function is
	 * called for each item. Usually you use this in the following manner:
	 * <p>
	 * <pre>
	 * for (T item : progress.iterate(iteratorOfT, itemCount, t -> ...)) { ... }
	 * progress.update(1f, "done");
	 * </pre>
	 *
	 * @param items the items to iterate over
	 * @return a iterable over the items that automatically increases the progress
	 */
	@NotNull
	default <T> Iterator<T> iterate(Iterator<T> items, int count, Function<T, String> message) {
		return new Iterator<T>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return items.hasNext();
			}

			@Override
			public T next() {
				T next = items.next();
				updateProgress(index++ / (float) count, message.apply(next));
				return next;
			}
		};
	}
}
