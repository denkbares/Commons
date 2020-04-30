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

package com.denkbares.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * This class implements a typed, null-save triple of three other objects.
 *
 * @author volker_belli
 */
public class Triple<T1, T2, T3> implements Tuple {

	private final T1 a;
	private final T2 b;
	private final T3 c;
	private int hash = -1;

	public Triple(T1 a, T2 b, T3 c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public T1 getA() {
		return a;
	}

	public T2 getB() {
		return b;
	}

	public T3 getC() {
		return c;
	}

	@Override
	public Object get(int index) {
		if (index == 0) return a;
		if (index == 1) return b;
		if (index == 2) return c;
		throw new ArrayIndexOutOfBoundsException(String.valueOf(index));
	}

	@Override
	public int getSize() {
		return 3;
	}

	@Override
	public Collection<?> asList() {
		return Arrays.asList(a, b, c);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Triple) {
			Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
			return Objects.equals(a, triple.a) &&
					Objects.equals(b, triple.b) &&
					Objects.equals(c, triple.c);
		}
		else if (o instanceof Tuple) {
			return Tuple.equals(this, (Tuple) o);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		if (hash == -1) hash = Tuple.hashCode(this);
		return hash;
	}

	@Override
	public String toString() {
		return "#Triple[" + getA() + "; " + getB() + "; " + getC() + "]";
	}
}
