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

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.jetbrains.annotations.NotNull;

import com.denkbares.utils.Log;

/**
 * This is a delegate for the ordinary {@link org.eclipse.rdf4j.query.TupleQueryResult}. Tries to close delegate query
 * result when garbage collected. Since we cannot guaranty garbage collection of the object, we still need to use
 * <tt>try(ClosingQueryResult result = getResult(..)) { code }</tt>
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 26.01.16
 */
public class TupleQueryResult implements ClosableTupleQueryResult, Iterable<BindingSet> {

	private RepositoryConnection connection;
	private org.eclipse.rdf4j.query.TupleQueryResult delegate;
	private CachedTupleQueryResult cache = null;
	private boolean calledNext = false;

	private List<Namespace> namespaces = null;

	public TupleQueryResult(RepositoryConnection connection, org.eclipse.rdf4j.query.TupleQueryResult delegate) {
		this.connection = connection;
		this.delegate = delegate;
	}

	@Override
	public CachedTupleQueryResult cachedAndClosed() throws QueryEvaluationException {
		return cachedAndClosed(false);
	}

	@Override
	public CachedTupleQueryResult cachedAndClosed(boolean preserveNamespaces) throws QueryEvaluationException {
		if (calledNext) {
			throw new UnsupportedOperationException("After calling next(), cacheAndClose() is no longer usable.");
		}
		if (preserveNamespaces) initNamespaces();
		if (cache == null) {
			List<String> bindingNames = getBindingNames();
			List<BindingSet> bindingSets = new ArrayList<>();
			try {
				while (!Thread.currentThread().isInterrupted() && hasNext()) {
					// we create a new binding set to make sure it doesn't hold
					// any references to the connection or repository
					bindingSets.add(new CachedBindingSet(next()));
				}
				if (Thread.currentThread().isInterrupted()) {
					Log.info("SPARQL query caching interrupted, closing...");
				}
			}
			finally {
				close();
			}
			cache = new CachedTupleQueryResult(bindingNames, bindingSets, namespaces);
		}

		return cache;
	}

	TupleQueryResult(List<Namespace> namespaces) {
		// only to be used by CachedTupleQueryResult
		this.namespaces = namespaces;
	}

	private void initNamespaces() {
		if (namespaces == null && connection != null) {
			this.namespaces = Iterations.asList(connection.getNamespaces());
		}
	}

	public IRI toShortIRI(IRI iri) {
		initNamespaces();
		if (namespaces == null) return iri;

		String uriText = iri.toString();
		int length = 0;
		IRI shortURI = iri;
		for (Namespace namespace : namespaces) {
			String partURI = namespace.getName();
			int partLength = partURI.length();
			if (partLength > length && uriText.length() > partLength && uriText.startsWith(partURI)) {
				String shortText = namespace.getPrefix() + ":" + uriText.substring(partLength);
				shortURI = new SimpleIRI(shortText) {
					private static final long serialVersionUID = 8831976782866898688L;
				};
				length = partLength;
			}
		}
		return shortURI;
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

	@NotNull
	@Override
	public Iterator<BindingSet> iterator() {
		if (cache != null) {
			return cache.iterator();
		}
		//noinspection IteratorNextCanNotThrowNoSuchElementException
		return new Iterator<BindingSet>() {

			@Override
			public boolean hasNext() {
				return TupleQueryResult.this.hasNext();
			}

			@Override
			public BindingSet next() {
				return TupleQueryResult.this.next();
			}
		};
	}

	public List<BindingSet> getBindingSets() {
		return cachedAndClosed().getBindingSets();
	}
}
