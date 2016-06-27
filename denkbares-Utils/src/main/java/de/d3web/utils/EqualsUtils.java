/*
 * Copyright (C) 2010 denkbares GmbH, Germany
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
package de.d3web.utils;

import java.util.Objects;

/**
 * Collected methods which allow easy implementation of <code>equals</code>.
 * 
 * @author volker_belli
 * @created 19.10.2010
 */
public final class EqualsUtils {

	/**
	 * Compares two objects. It returns true if the objects are identical, both
	 * objects are null or (if both objects are non-null), if a equals to b.
	 * otherwise it returns false.
	 * <p>
	 * If the two objects are arrays, all their elements will be compared using
	 * this method.
	 * <p>
	 * The method is null-safe, so each of the arguments may be null.
	 * 
	 * @created 19.10.2010
	 */
	public static boolean equals(Object a, Object b) {
		return Objects.deepEquals(a, b);
	}

	/**
	 * Checks if two objects are both null or if they equal with the equals
	 * method
	 * 
	 * @created 15.07.2011
	 * @param obj1 first {@link Object}
	 * @param obj2 second {@link Object}
	 * @return true if both objects are null or obj1.equals(obj2), false
	 *         otherwise
	 */
	public static boolean isSame(Object obj1, Object obj2) {
		if (obj1 == null && obj2 == null) {
			return true;
		}
		if (obj1 != null && obj2 != null) {
			return obj1.equals(obj2);
		}
		return false;
	}

}
