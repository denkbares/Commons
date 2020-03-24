/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

package com.denkbares.semanticcore.sparql;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.jetbrains.annotations.NotNull;

import com.denkbares.semanticcore.BooleanQuery;
import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.TupleQueryResult;

/**
 * Abstract base class that delegates all sparql endpoint methods. This class can be used to implement a delegation
 * class, that selectively modifies particular calls.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 21.03.2020
 */
public abstract class AbstractDelegateEndpoint implements SPARQLEndpoint {

	/**
	 * Returns the endpoint to delegate all method calls to.
	 */
	@NotNull
	protected abstract SPARQLEndpoint getDelegate();

	@Override
	public Collection<Namespace> getNamespaces() throws RepositoryException {
		return getDelegate().getNamespaces();
	}

	@Override
	public boolean sparqlAsk(String query) throws QueryFailedException {
		return getDelegate().sparqlAsk(query);
	}

	@Override
	public boolean sparqlAsk(Collection<Namespace> namespaces, String query) throws QueryFailedException {
		return getDelegate().sparqlAsk(namespaces, query);
	}

	@Override
	public boolean sparqlAsk(BooleanQuery query) throws QueryFailedException {
		return getDelegate().sparqlAsk(query);
	}

	@Override
	public boolean sparqlAsk(BooleanQuery query, Map<String, Value> bindings) throws QueryFailedException {
		return getDelegate().sparqlAsk(query, bindings);
	}

	@Override
	public BooleanQuery prepareAsk(String query) {
		return getDelegate().prepareAsk(query);
	}

	@Override
	public BooleanQuery prepareAsk(Collection<Namespace> namespaces, String query) {
		return getDelegate().prepareAsk(namespaces, query);
	}

	@Override
	public TupleQueryResult sparqlSelect(String query) throws QueryFailedException {
		return getDelegate().sparqlSelect(query);
	}

	@Override
	public TupleQueryResult sparqlSelect(Collection<Namespace> namespaces, String query) throws QueryFailedException {
		return getDelegate().sparqlSelect(namespaces, query);
	}

	@Override
	public TupleQueryResult sparqlSelect(TupleQuery query) throws QueryFailedException {
		return getDelegate().sparqlSelect(query);
	}

	@Override
	public TupleQueryResult sparqlSelect(TupleQuery query, Map<String, Value> bindings) throws QueryFailedException {
		return getDelegate().sparqlSelect(query, bindings);
	}

	@Override
	public TupleQuery prepareSelect(String query) {
		return getDelegate().prepareSelect(query);
	}

	@Override
	public TupleQuery prepareSelect(Collection<Namespace> namespaces, String query) {
		return getDelegate().prepareSelect(namespaces, query);
	}

	@Override
	public ValueFactory getValueFactory() {
		return getDelegate().getValueFactory();
	}

	@Override
	public void dump(String query) {
		getDelegate().dump(query);
	}

	@Override
	public void close() throws RepositoryException {
		getDelegate().close();
	}
}
