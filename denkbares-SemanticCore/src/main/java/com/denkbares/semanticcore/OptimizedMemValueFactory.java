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

package com.denkbares.semanticcore;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;

/**
 * This is based on {@link MemValueFactory} and reuses/shares created values. It has a slight concurrency optimization
 * by using atomic long instead of synchronizing to create new b-nodes.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 13.09.16
 */
public class OptimizedMemValueFactory extends MemValueFactory {

	private final AtomicLong bNodeNumber = new AtomicLong(1L);

	@Override
	public BNode createBNode() {
		return super.createBNode("n" + this.bNodeNumber.getAndIncrement());
	}

	public long getBNodeNumber() {
		return this.bNodeNumber.get();
	}

	public void setBNodeNumber(long number) {
		this.bNodeNumber.set(number);
	}
}
