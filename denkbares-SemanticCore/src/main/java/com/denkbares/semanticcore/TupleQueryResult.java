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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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

	private final org.eclipse.rdf4j.query.TupleQueryResult delegate;
	private final List<Consumer<TupleQueryResult>> closeHandlers = new ArrayList<>(0);
	private final Date creationDate;

	private CachedTupleQueryResult cache = null;
	private boolean calledNext = false;
	private boolean closed = false;

	public TupleQueryResult(org.eclipse.rdf4j.query.TupleQueryResult delegate) {
		this.delegate = delegate;
		this.creationDate = new Date();
	}

	TupleQueryResult() {
		// only to be used by CachedTupleQueryResult
		this.delegate = null;
		this.creationDate = new Date();
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public TupleQueryResult onClose(Runnable closeHandler) {
		return onClose(self -> closeHandler.run());
	}

	public TupleQueryResult onClose(Consumer<TupleQueryResult> closeHandler) {
		this.closeHandlers.add(closeHandler);
		return this;
	}

	@Override
	public CachedTupleQueryResult cachedAndClosed() throws QueryEvaluationException {
		if (calledNext) {
			throw new UnsupportedOperationException("After calling next(), this method is no longer usable.");
		}
		if (closed) {
			throw new UnsupportedOperationException("After calling close(), this method is no longer usable.");
		}
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
			cache = new CachedTupleQueryResult(bindingNames, bindingSets, getCreationDate());
		}

		return cache;
	}

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		if (cache != null) return cache.getBindingNames();
		assert delegate != null;
		return delegate.getBindingNames();
	}

	@Override
	public void close() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		try {
			closeHandlers.forEach(handler -> handler.accept(this));
		}
		catch (RepositoryException e) {
			throw new QueryEvaluationException("Unable to close connection", e);
		}
		closed = true;
		assert delegate != null;
		delegate.close();
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		if (closed) {
			throw new UnsupportedOperationException("After calling close(), this method is no longer usable.");
		}
		assert delegate != null;
		return delegate.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		if (closed) {
			throw new UnsupportedOperationException("After calling close(), this method is no longer usable.");
		}
		calledNext = true;
		assert delegate != null;
		return delegate.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		if (cache != null) {
			throw new UnsupportedOperationException("After calling cacheAndClose(), this method is no longer usable.");
		}
		if (closed) {
			throw new UnsupportedOperationException("After calling close(), this method is no longer usable.");
		}
		assert delegate != null;
		delegate.remove();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		// we only close if not closed yet, to avoid useless exception creation
		if (!closed) close();
	}

	@NotNull
	@Override
	public Iterator<BindingSet> iterator() {
		if (cache != null) {
			return cache.iterator();
		}
		return new Iterator<>() {

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

	@Override
	public Stream<BindingSet> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	public List<BindingSet> getBindingSets() {
		return cachedAndClosed().getBindingSets();
	}
}
