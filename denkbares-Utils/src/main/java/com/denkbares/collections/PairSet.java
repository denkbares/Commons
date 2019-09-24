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

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

/**
 * A Set which consists of at most two elements.
 */
public class PairSet<T> extends HashSet<T> {

	private T concept1;
	private T concept2;

	public PairSet(T concept1, T concept2) {
		this.concept1 = concept1;
		this.concept2 = concept2;
		super.add(concept1);
		super.add(concept2);
	}

	public PairSet() {

	}

	public T getOther(T concept) {
		if (concept.equals(concept1)) {
			return concept2;
		}
		if (concept.equals(concept2)) {
			return concept1;
		}
		throw new IllegalArgumentException("Object not contained in PairSet");
	}

	@Override
	public boolean add(T uri) {
		if(this.contains(uri)) {
			return false;
		}
		if(concept1 == null) {
			concept1 = uri;
			super.add(uri);
			return true;
		} else if(concept2 == null) {
			concept2 = uri;
			super.add(uri);
			return true;
		} else {
			throw new IllegalArgumentException("Not allowed for this implementation: Too many objects for PairSet (only 2 allowed)");
		}
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends T> c) {
		boolean changed = false;
		if(c.size() <= 2) {
			for (T t : c) {
				boolean added = add(t);
				if(added) {
					changed = true;
				}
			}
		} else {
			throw new IllegalArgumentException("Not allowed for this implementation");
		}
		return changed;
	}

	@Override
	public void clear() {
		this.concept1 = null;
		this.concept2 = null;
		super.clear();
	}

	@Override
	public boolean remove(Object o) {
		if(this.contains(o)) {
			super.remove(o);
			if(concept1.equals(o)) {
				concept1 = null;
			}
			if(concept2.equals(o)) {
				concept2 = null;
			}
		} else {
			return false;
		}
		return false;
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		boolean removed = super.removeIf(filter);
		if(filter.test(concept1)) {
			concept1 = null;
		}
		if(filter.test(concept2)) {
			concept2 = null;
		}
		return removed;
	}
}
