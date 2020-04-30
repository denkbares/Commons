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

import java.util.Collection;
import java.util.Objects;

/**
 * This class implements a typed, null-save tuple of a number of other objects.
 *
 * @author volker_belli
 */
public interface Tuple {

	Object get(int index);

	int getSize();

	Collection<?> asList();

	static boolean equals(Tuple t1, Tuple t2) {
		if (t1 == t2) return true;
		if (t1 == null || t2 == null) return false;

		// must have same size
		int size1 = t1.getSize();
		int size2 = t2.getSize();
		if (size1 != size2) return false;

		// nad must have same elements in same order
		for (int i = 0; i < size1; i++) {
			if (!Objects.equals(t1.get(i), t2.get(i))) return false;
		}
		return true;
	}

	static int hashCode(Tuple tuple) {
		if (tuple == null) return 0;
		int hash = 1;
		int size = tuple.getSize();
		for (int i = 0; i < size; i++) {
			Object item = tuple.get(i);
			hash = 31 * hash + (item == null ? 0 : item.hashCode());
		}
		return hash;
	}
}
