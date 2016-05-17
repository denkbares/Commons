/*
 * Copyright (C) 2013 denkbares GmbH
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openrdf.model.Namespace;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.denkbares.semanticcore.sparql.SPARQLBooleanQuery;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.sparql.SPARQLQuery;
import com.denkbares.semanticcore.sparql.SPARQLQueryResult;

/**
 * Implements a SPARQLEndpoint for sesame repositories.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 31.12.2014
 */
public class SesameEndpoint implements SPARQLEndpoint {

	private RepositoryConnection connection;
	private String sparqlPrefixes = null;

	/**
	 * Creates a new SPARQLEndpoint for the specified SESAME repository connection.
	 *
	 * @param connection the connection to build the queries on
	 */
	public SesameEndpoint(org.openrdf.repository.RepositoryConnection connection) {
		Objects.requireNonNull(connection);
		this.setConnection(connection);
	}

	/**
	 * Creates a new endpoint with no connection, must call #setConnection() before usage.
	 */
	protected SesameEndpoint() {
	}

	protected void setConnection(org.openrdf.repository.RepositoryConnection connection) {
		if (!(connection instanceof RepositoryConnection)) {
			connection = new RepositoryConnection(connection);
		}
		this.connection = (RepositoryConnection) connection;
	}

	@Override
	public synchronized void close() throws RepositoryException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Override
	public Map<String, String> getPrefixes() throws RepositoryException {
		RepositoryResult<Namespace> namespaces = connection.getNamespaces();
		Map<String, String> result = new HashMap<>();
		while (namespaces.hasNext()) {
			Namespace namespace = namespaces.next();
			result.put(namespace.getName(), namespace.getPrefix());
		}
		return result;
	}

	@Override
	public SPARQLQuery prepareQuery(String queryString, String graph) throws QueryFailedException {
		checkConnection();
		try {
			TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, getSPARQLPrefixes() + queryString);
			return new SPARQLQuery(queryString, graph, tupleQuery);
		}
		catch (RepositoryException e) {
			throw new QueryFailedException("Failed to execute query: " + queryString, e);
		}
		catch (MalformedQueryException e) {
			throw new QueryFailedException("Malformed query: " + queryString, e);
		}
	}

	private String getSPARQLPrefixes() throws RepositoryException {

		if (sparqlPrefixes == null) {

			StringBuilder sb = new StringBuilder();

			RepositoryResult<Namespace> namespaces = connection.getNamespaces();
			while (namespaces.hasNext()) {
				Namespace ns = namespaces.next();
				sb.append("PREFIX ").append(ns.getPrefix()).append(": <").append(ns.getName()).append(">\n");
			}

			sb.append("\n");
			sparqlPrefixes = sb.toString();
		}
		return sparqlPrefixes;
	}

	@Override
	public SPARQLBooleanQuery prepareBooleanQuery(String queryString, String graph) throws QueryFailedException {
		checkConnection();
		try {
			BooleanQuery booleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, getSPARQLPrefixes() + queryString);
			return new SPARQLBooleanQuery(queryString, graph, booleanQuery);
		}
		catch (RepositoryException e) {
			throw new QueryFailedException("Failed to execute query: " + queryString, e);
		}
		catch (MalformedQueryException e) {
			throw new QueryFailedException("Malformed query: " + queryString, e);
		}
	}

	@Override
	public SPARQLQueryResult execute(SPARQLQuery query, Map<String, Value> bindings) throws QueryFailedException {
		checkConnection();
		query.setBindings(bindings);
		try {
			return new SPARQLQueryResult(new TupleQueryResult(null, query.getQuery().evaluate()));
		}
		catch (QueryEvaluationException e) {
			throw new QueryFailedException("Failed to execute query: " + query.getQueryString(), e);
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		if (connection == null) return null;
		return connection.getValueFactory();
	}

	private void checkConnection() throws QueryFailedException {
		if (connection == null) {
			throw new QueryFailedException("connection already closed");
		}
	}
}
