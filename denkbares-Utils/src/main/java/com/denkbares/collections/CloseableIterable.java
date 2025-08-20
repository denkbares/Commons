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
import org.jetbrains.annotations.NotNull;

/**
 * Iterable which is closeable
 *
 * @author Friedrich Fell (Service Mate GmbH)
 * @created 19.08.2025
 */
public class CloseableIterable<E> implements Closeable, Iterable<E> {
	private final Closeable closeable;
	private final Iterable<E> iterable;

	public CloseableIterable(Iterable<E> iterable, Closeable closeable) {
		this.closeable = closeable;
		this.iterable = iterable;
	}

	@Override
	public void close() throws IOException {
		this.closeable.close();
	}

	@Override
	public @NotNull Iterator<E> iterator() {
		return this.iterable.iterator();
	}
}
