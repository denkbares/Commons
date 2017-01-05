package com.denkbares.semanticcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.Repository;

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
	private Iterator<BindingSet> cachedIterator = null;

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
		return cachedIterator.hasNext();
	}

	private void initIterator() {
		if (cachedIterator == null) {
			cachedIterator = cache.iterator();
		}
	}

	@Override
	public BindingSet next() {
		initIterator();
		return cachedIterator.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public com.denkbares.semanticcore.CachedTupleQueryResult cachedAndClosed() {
		return this;
	}

	@Override
	public Iterator<BindingSet> iterator() {
		return getBindingSets().iterator();
	}

	@Override
	public List<BindingSet> getBindingSets() {
		return Collections.unmodifiableList(cache);
	}
}
