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

import java.util.Comparator;

import com.denkbares.collections.PartialHierarchyTree.Node;

class PartialHierarchyNodeComparator<T> implements Comparator<PartialHierarchyTree.Node<T>> {

	private final Comparator<T> comp;

	public PartialHierarchyNodeComparator(Comparator<T> c) {
		this.comp = c;
	}

	@Override
	public int compare(Node<T> arg0, Node<T> arg1) {
		return comp.compare(arg0.data, arg1.data);
	}

}
