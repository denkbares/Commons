/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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

import java.io.IOException;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.denkbares.semanticcore.sparql.SPARQLBooleanQuery;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.sparql.SPARQLQuery;
import com.denkbares.semanticcore.sparql.SPARQLQueryResult;

/**
 * An implementation of a sparql endpoint that will be constructed on demand by some factory method,
 * the first time it is queried or a query is prepared.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 13.01.2015
 */
public class FutureSPARQLEndpoint implements SPARQLEndpoint {

	/**
	 * Functional interface to create the underlying sparql endpoint on demand.
	 */
	@FunctionalInterface
	public interface SPARQLEndpointFactory {
		SPARQLEndpoint createEndpoint() throws IOException;
	}

	private final SPARQLEndpointFactory factory;
	private SPARQLEndpoint delegate = null;

	/**
	 * Creates a new sparql endpoint based on some factory method. The factory method will be used
	 * to create the underlying endpoint that is delegated for each query. The factory method is not
	 * called until the first time a query or preparation is performed.
	 *
	 * @param factory the factory method to be used to create the endpoint on demand
	 */
	public FutureSPARQLEndpoint(SPARQLEndpointFactory factory) {
		this.factory = factory;
	}

	@Override
	public synchronized void close() throws RepositoryException {
		if (delegate != null) {
			delegate.close();
			delegate = null;
		}
	}

	@Override
	public synchronized ValueFactory getValueFactory() {
		if (delegate == null) return null;
		return delegate.getValueFactory();
	}

	@Override
	public Map<String, String> getPrefixes() throws RepositoryException {
		if (delegate == null) return null;
		return delegate.getPrefixes();
	}

	@Override
	public SPARQLQuery prepareQuery(String queryString, String graph) throws QueryFailedException {
		return getQueryEndpoint().prepareQuery(queryString, graph);
	}

	@Override
	public SPARQLBooleanQuery prepareBooleanQuery(String queryString, String graph) throws QueryFailedException {
		return getQueryEndpoint().prepareBooleanQuery(queryString, graph);
	}

	@Override
	public SPARQLQueryResult execute(SPARQLQuery query, Map<String, Value> bindings) throws QueryFailedException {
		return getQueryEndpoint().execute(query, bindings);
	}

	public synchronized SPARQLEndpoint getSparqlEndpoint() throws IOException {
		if (delegate == null) {
			delegate = factory.createEndpoint();
		}
		return delegate;
	}

	private SPARQLEndpoint getQueryEndpoint() throws QueryFailedException {
		try {
			return getSparqlEndpoint();
		}
		catch (IOException e) {
			throw new QueryFailedException("cannot create sparql endpoint", e);
		}
	}
}
