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

import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a minimized hash set, that retains the order of the stored elements. It is very memory efficient
 * for empty sets of sets of size 1.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 16.10.2019
 */
public class MinimizedLinkedHashSet<T> extends MinimizedHashSet<T> {
	@Override
	@NotNull
	protected Set<T> createWrappingSet(int capacity) {
		return new LinkedHashSet<>(capacity);
	}
}
