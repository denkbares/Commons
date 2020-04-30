/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 30.04.2020
 */
public class NAryTuple implements Tuple {

	private final Object[] items;
	private int hash = -1;

	public NAryTuple(Object... items) {
		this.items = items;
	}

	@Override
	public Object get(int index) {
		return items[index];
	}

	@Override
	public int getSize() {
		return items.length;
	}

	@Override
	public Collection<?> asList() {
		return Arrays.asList(items);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Tuple) && Tuple.equals(this, (Tuple) other);
	}

	@Override
	public int hashCode() {
		if (hash == -1) hash = Tuple.hashCode(this);
		return hash;
	}
}
