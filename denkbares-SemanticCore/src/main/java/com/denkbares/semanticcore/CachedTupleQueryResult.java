package com.denkbares.semanticcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Namespace;
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
	private Iterator<BindingSet> cachedIterator = null;

	public CachedTupleQueryResult(List<String> bindingNames, List<BindingSet> bindingSets) {
		this(bindingNames, bindingSets, null);
	}

	public CachedTupleQueryResult(List<String> bindingNames, List<BindingSet> bindingSets, List<Namespace> namespaces) {
		super(namespaces);
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
		cachedIterator = null;
	}

	/**
	 * Returns a sequential {@code Stream} with the binding sets of this query result.
	 */
	public Stream<BindingSet> stream() {
		return getBindingSets().stream();
	}

	@Override
	public List<BindingSet> getBindingSets() {
		return Collections.unmodifiableList(cache);
	}
}
