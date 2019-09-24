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

package com.denkbares.semanticcore.sparql;

import java.util.Collection;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.denkbares.semanticcore.TupleQueryResult;

/**
 * Interface to describe an sparql endpoint that is also capable to directly execute sparql
 * queries.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 30.12.2014
 */
public interface SPARQLEndpoint extends AutoCloseable {

	/**
	 * Provides a collection of the known prefixes of this endpoint.
	 *
	 * @return a collection with the namespaces known to this endpoint
	 */
	Collection<Namespace> getNamespaces() throws RepositoryException;

	/**
	 * Executes the given ASK query. All known namespaces will automatically be prepended as prefixes.
	 *
	 * @param query the ASK query without any namespace prefixes
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(String query) throws QueryFailedException {
		return sparqlAsk(getNamespaces(), query);
	}

	/**
	 * Executes the given ASK query. Only the given namespaces will automatically be prepended as prefixes.
	 *
	 * @param query      the ASK query to be executed
	 * @param namespaces the namespaces to prepend as prefixes
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	boolean sparqlAsk(Collection<Namespace> namespaces, String query) throws QueryFailedException;

	/**
	 * Executes the given SELECT query. All known namespaces will automatically be prepended as prefixes.
	 *
	 * @param query the SELECT query without any namespace prefixes
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(String query) throws QueryFailedException {
		return sparqlSelect(getNamespaces(), query);
	}

	/**
	 * Executes the given SELECT query. Only the given namespaces will automatically be prepended as prefixes.
	 *
	 * @param query      the SELECT query to be executed
	 * @param namespaces the namespaces to prepend as prefixes
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	TupleQueryResult sparqlSelect(Collection<Namespace> namespaces, String query) throws QueryFailedException;

	/**
	 * Returns the value factory for the given endpoint.
	 *
	 * @return a value factory
	 */
	ValueFactory getValueFactory();

	@Override
	void close() throws RepositoryException;

	class QueryFailedException extends RuntimeException {

		public QueryFailedException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
