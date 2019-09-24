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

import java.util.Set;

/**
 * An Exception that can be thrown if a problem (usually a cycle)
 * within the use of the PartialHierarchy occurs.
 *
 *
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 25.05.16.
 */
public class PartialHierarchyException extends Exception {

	private final Set<?> path;
	private final Object cycleStartObject;

	public PartialHierarchyException(Object cycleStartObject, Set path) {
		super("The following row "+cycleStartObject+" forms a hierarchy cycle during insertion: " + path);
		this.cycleStartObject = cycleStartObject;
		this.path = path;
	}

	/**
	 * Returns the cyclic path (for example for printing an error message)
	 *
	 * @return
	 */
	public Set<?> getPath() {
		return path;
	}

	/**
	 * The object that is the starting point and end point of the cycle.
	 *
	 * @return
	 */
	public Object getCycleStartObject() {
		return cycleStartObject;
	}
}
