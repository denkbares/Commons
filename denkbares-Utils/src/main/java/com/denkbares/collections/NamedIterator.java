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

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

/**
 * Class allowing to name your iterator for better readability during debugging.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 24.02.2025
 */
public class NamedIterator<E> implements Iterator<E> {

	private final String name;
	private final Iterator<E> delegate;

	public NamedIterator(@NotNull String name, @NotNull Iterator<E> delegate) {
		this.name = name;
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public E next() {
		return delegate.next();
	}

	@Override
	public String toString() {
		return name + "{" + delegate + '}';
	}
}
