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

package com.denkbares.semanticcore.sparql;

import java.util.Collections;
import java.util.Map;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;

/**
 * Interface to describe an sparql endpoint that is also capable to directly execute sparql
 * queries.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 30.12.2014
 */
public interface SPARQLEndpoint extends AutoCloseable {

	/**
	 * Returns a map of the known prefixes of this SPARQLEndpoint.
	 *
	 * @return a <name, abbreviation> map of prefixes
	 */
	Map<String, String> getPrefixes() throws RepositoryException;

	/**
	 * Prepares a query on a certain graph
	 *
	 * @param queryString SPARQL query to execute
	 * @param graph       Graph to run on
	 * @return Prepared SPARQLQuery object
	 * @throws QueryFailedException If the query was malformed or the method unable to successfully
	 *                              prepare it.
	 */
	SPARQLQuery prepareQuery(String queryString, String graph) throws QueryFailedException;

	/**
	 * Prepares a query to run on the default graph
	 *
	 * @param queryString SPARQL query to execute
	 * @return Prepared SPARQLQuery object
	 * @throws QueryFailedException If the query was malformed or the method unable to successfully
	 *                              prepare it.
	 */
	default SPARQLQuery prepareQuery(String queryString) throws QueryFailedException {
		return prepareQuery(queryString, "");
	}

	/**
	 * Prepares a query on a certain graph
	 *
	 * @param queryString SPARQL query to execute
	 * @param graph       Graph to run on
	 * @return Prepared SPARQLQuery object
	 * @throws QueryFailedException If the query was malformed or the method unable to successfully
	 *                              prepare it.
	 */
	SPARQLBooleanQuery prepareBooleanQuery(String queryString, String graph) throws QueryFailedException;

	/**
	 * Prepares a query to run on the default graph
	 *
	 * @param queryString SPARQL query to execute
	 * @return Prepared SPARQLQuery object
	 * @throws QueryFailedException If the query was malformed or the method unable to successfully
	 *                              prepare it.
	 */
	default SPARQLBooleanQuery prepareBooleanQuery(String queryString) throws QueryFailedException {
		return prepareBooleanQuery(queryString, "");
	}

	/**
	 * Executes a previously prepared SPARQLQuery parametrized with a set of bindings.
	 *
	 * @param query    A prepared SPARQLQuery
	 * @param bindings A Map of Strings to OpenRDF Values to use as bindings
	 * @return The query result
	 * @throws QueryFailedException if the execution was not successful
	 * @see #prepareQuery(String)
	 * @see #prepareQuery(String, String)
	 */
	SPARQLQueryResult execute(SPARQLQuery query, Map<String, Value> bindings) throws QueryFailedException;

	/**
	 * Executes a previously prepared SPARQLQuery without any bindings.
	 *
	 * @param query The prepared query to run
	 * @return The query result
	 * @throws QueryFailedException if the execution was not successful
	 * @see #prepareQuery(String)
	 * @see #prepareQuery(String)
	 */
	default SPARQLQueryResult execute(SPARQLQuery query) throws QueryFailedException {
		return execute(query, Collections.emptyMap());
	}

	/**
	 * Prepares and executes the given query string on the given graph
	 * <p/>
	 * <b>Note:</b> For performance reasons, you should consider preparing the query and executing
	 * it multiple times.
	 *
	 * @param queryString SPARQL query to execute
	 * @param graph       Graph to run on
	 * @return The query result
	 * @throws QueryFailedException if the preparation or execution was not successful
	 */
	default SPARQLQueryResult query(String queryString, String graph) throws QueryFailedException {
		SPARQLQuery query = prepareQuery(queryString, graph);
		return execute(query);
	}

	/**
	 * Prepares and executes the given query string on the default graph
	 * <p/>
	 * <b>Note:</b> For performance reasons, you should consider preparing the query and executing
	 * it multiple times.
	 *
	 * @param queryString SPARQL query to execute
	 * @return The query result
	 * @throws QueryFailedException if the preparation or execution was not successful
	 */
	default SPARQLQueryResult query(String queryString) throws QueryFailedException {
		return query(queryString, "");
	}

	/**
	 * Returns the value factory for the given model. The method returns null if there is no open
	 * connection to a repository.
	 *
	 * @return value factory
	 */
	ValueFactory getValueFactory();

	class QueryFailedException extends RuntimeException {
		public QueryFailedException(String message) {
			super(message);
		}

		public QueryFailedException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@Override
	void close() throws RepositoryException;
}
