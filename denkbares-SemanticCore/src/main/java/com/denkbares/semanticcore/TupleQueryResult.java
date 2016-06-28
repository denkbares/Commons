/*
 * Copyright (C) 2016 denkbares GmbH, Germany
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
import java.util.Iterator;
import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.query.SPARQLQueryBindingSet;

import de.d3web.utils.Log;

/**
 * This is a delegate for the ordinary {@link org.openrdf.query.TupleQueryResult}.
 * Tries to close delegate query result when garbage collected. Since we cannot guaranty garbage collection of the
 * object, we still need to use <tt>try(ClosingQueryResult result = getResult(..)) { code }</tt>
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 26.01.16
 */
public class TupleQueryResult implements ClosableTupleQueryResult, Iterable<BindingSet> {

	private RepositoryConnection connection;
	private org.openrdf.query.TupleQueryResult delegate;
	private CachedTupleQueryResult cache = null;
	private boolean calledNext = false;

	public TupleQueryResult(RepositoryConnection connection, org.openrdf.query.TupleQueryResult delegate) {
		this.connection = connection;
		this.delegate = delegate;
	}

	/**
	 * Create a cached version of this result. This cached version produces a little overhead (object creating, all
	 * results are retrieved beforehand), but is the easier to user (no exceptions, some additional methods...)
	 *
	 * @return a cached version of this result
	 * @throws QueryEvaluationException
	 */
	@Override
	public CachedTupleQueryResult cachedAndClosed() throws QueryEvaluationException {
		if (calledNext) {
			throw new UnsupportedOperationException("After calling next(), cacheAndClose() is no longer usable.");
		}
		if (cache == null) {
			List<String> bindingNames = getBindingNames();
			List<BindingSet> bindingSets = new ArrayList<>();
			try {
				while (!Thread.currentThread().isInterrupted() && hasNext()) {
					// we create a new binding set to make sure it doesn't hold
					// any references to the connection or repository
					bindingSets.add(new SPARQLQueryBindingSet(next()));
				}
				if (Thread.currentThread().isInterrupted()) {
					Log.info("SPARQL query caching interrupted, closing...");
				}
			}
			finally {
				//noinspection ThrowFromFinallyBlock
				close();
			}
			cache = new CachedTupleQueryResult(bindingNames, bindingSets);
		}

		return cache;
	}

	TupleQueryResult() {
		// only to be used by CachedTupleQueryResult
	}

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		return delegate.getBindingNames();
	}

	@Override
	public void close() throws QueryEvaluationException {
		delegate.close();
		try {
			if (connection != null) connection.close();
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException("Unable to close connection", e);
		}
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		return delegate.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		calledNext = true;
		return delegate.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		delegate.remove();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	@Override
	public Iterator<BindingSet> iterator() {
		throw new UnsupportedOperationException("Iterators are only available after calling cacheAndClose()");
	}

	public List<BindingSet> getBindingSets() {
		throw new UnsupportedOperationException("Collections are only available after calling cacheAndClose()");
	}

}