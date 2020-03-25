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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.Repository;
import org.jetbrains.annotations.NotNull;

/**
 * Cached version of the {@link TupleQueryResult}. No exceptions are thrown, no references to any {@link Repository} or
 * {@link RepositoryConnection}.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 19.04.16
 */
public class CachedTupleQueryResult extends TupleQueryResult {

	private final List<BindingSet> cache;
	private final List<String> bindingNames;
	// the CachedTupleQueryResult could be used by different threads, so use thread local iterator
	private final ThreadLocal<Iterator<BindingSet>> cachedIterator = new ThreadLocal<>();

	public CachedTupleQueryResult(List<String> bindingNames, List<BindingSet> bindingSets) {
		this.bindingNames = new ArrayList<>(bindingNames);
		this.cache = new ArrayList<>(bindingSets);
	}

	@Override
	public List<String> getBindingNames() {
		return bindingNames;
	}

	@Override
	public void close() {
		// not necessary, will only be accessible in closed state...
	}

	@Override
	public boolean hasNext() {
		initIterator();
		return cachedIterator.get().hasNext();
	}

	private void initIterator() {
		if (cachedIterator.get() == null) {
			cachedIterator.set(cache.iterator());
		}
	}

	@Override
	public BindingSet next() {
		initIterator();
		return cachedIterator.get().next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public com.denkbares.semanticcore.CachedTupleQueryResult cachedAndClosed() {
		return this;
	}

	@NotNull
	@Override
	public Iterator<BindingSet> iterator() {
		return getBindingSets().iterator();
	}

	/**
	 * Resets this results iterator so {@link #next()} and {@link #hasNext()} behave exactly like the first time
	 * iterating this result.
	 */
	public void resetIterator() {
		cachedIterator.set(null);
	}

	/**
	 * Returns a sequential {@code Stream} with the binding sets of this query result.
	 */
	@Override
	public Stream<BindingSet> stream() {
		return getBindingSets().stream();
	}

	@Override
	public List<BindingSet> getBindingSets() {
		return Collections.unmodifiableList(cache);
	}
}
