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



/**
 * Defines a generic hierarchy.
 * 
 * @author Jochen Reutelshöfer
 * @created 25.11.2013
 * @param <T>
 */
public interface PartialHierarchy<T> {

	/**
	 * Returns true if node1 is a (transitive!) successor of node2, false
	 * otherwise.
	 * 
	 * @created 25.11.2013
	 */
	boolean isSuccessorOf(T node1, T node2) throws PartialHierarchyException;
}
