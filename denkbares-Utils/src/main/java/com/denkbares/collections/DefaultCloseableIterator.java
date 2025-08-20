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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import org.jetbrains.annotations.Nullable;

/**
 * Autocloseable Iterator, which wraps another iterator and closes a corresponding closeable Object.
 * When the delegated iterator is also an ClosableIterator, this iterator will be closed.
 *
 * @author Friedrich Fell (Service Mate GmbH)
 * @created 18.08.2025
 */
public class DefaultCloseableIterator<E> implements CloseableIterator<E> {
	private final Iterator<E> delegate;
	private final Closeable closeable;

	public DefaultCloseableIterator(DefaultCloseableIterator<E> delegate) {
		this(delegate, delegate);
	}

	/**
	 * With a normal iterator nothing will be closed.
	 *
	 * @param delegate to delegate iterator functions
	 */
	public DefaultCloseableIterator(Iterator<E> delegate) {
		this(delegate, null);
	}

	/**
	 * Uses an iterator to delegate all methods and a {@link Closeable} which should be closed when iteration is
	 * finished (auto closed).
	 * Closeable can be null, if nothing has to be closed, but a CloseableIterator is needed.
	 *
	 * @param delegate  to delegate elements
	 * @param closeable to close when iterator is auto closed.
	 */
	public DefaultCloseableIterator(Iterator<E> delegate, @Nullable Closeable closeable) {
		this.delegate = delegate;
		this.closeable = closeable;
	}

	@Override
	public void close() throws IOException {
		if (closeable != null) {
			this.closeable.close();
		}
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public E next() {
		return delegate.next();
	}
}
