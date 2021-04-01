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
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.jetbrains.annotations.NotNull;

import com.denkbares.semanticcore.BooleanQuery;
import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.TupleQueryResult;

/**
 * Interface to describe an sparql endpoint that is also capable to directly execute sparql queries.
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
	default boolean sparqlAsk(Collection<Namespace> namespaces, String query) throws QueryFailedException {
		try (BooleanQuery ask = prepareAsk(namespaces, query)) {
			return sparqlAsk(ask);
		}
	}

	/**
	 * Executes the given prepared ASK query.
	 *
	 * @param query the ASK query to be executed
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(BooleanQuery query) throws QueryFailedException {
		return query.evaluate();
	}

	/**
	 * Executes the given prepared ASK query. The specified variable bindings are applied to the query before executed,
	 * and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the same
	 * prepared query.
	 *
	 * @param query  the ASK query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(BooleanQuery query, String param1, Value value1) throws QueryFailedException {
		return sparqlAsk(query, Collections.singletonMap(param1, value1));
	}

	/**
	 * Executes the given prepared ASK query. The specified variable bindings are applied to the query before executed,
	 * and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the same
	 * prepared query.
	 *
	 * @param query  the ASK query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @param param2 the second query variable to bind
	 * @param value2 the value of the second query variable
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(BooleanQuery query, String param1, Value value1, String param2, Value value2) throws QueryFailedException {
		return sparqlAsk(query, ImmutableMap.of(param1, value1, param2, value2));
	}

	/**
	 * Executes the given prepared ASK query. The specified variable bindings are applied to the query before executed,
	 * and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the same
	 * prepared query.
	 *
	 * @param query  the ASK query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @param param2 the second query variable to bind
	 * @param value2 the value of the second query variable
	 * @param param3 the third query variable to bind
	 * @param value3 the value of the third query variable
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(BooleanQuery query, String param1, Value value1, String param2, Value value2, String param3, Value value3) throws QueryFailedException {
		return sparqlAsk(query, ImmutableMap.of(param1, value1, param2, value2, param3, value3));
	}

	/**
	 * Executes the given prepared ASK query. The specified variable bindings are applied to the query before executed,
	 * and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the same
	 * prepared query.
	 *
	 * @param query    the ASK query to be executed
	 * @param bindings the variable bindings to be used when executing the perpared query
	 * @return the result of the ASK query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default boolean sparqlAsk(BooleanQuery query, Map<String, Value> bindings) throws QueryFailedException {
		return query.evaluate(bindings);
	}

	/**
	 * Prepares the given sparql ask query. All known namespaces will automatically be prepended as prefixes.
	 *
	 * @param query the sparql ask query to be prepared
	 * @return the prepared query
	 */
	default BooleanQuery prepareAsk(String query) {
		return prepareAsk(getNamespaces(), query);
	}

	/**
	 * Prepares the given sparql ask query. Only the given namespaces will automatically be prepended as prefixes.
	 *
	 * @param query      the sparql ask query to be prepared
	 * @param namespaces the namespaces to prepend as prefixes
	 * @return the prepared query
	 */
	BooleanQuery prepareAsk(Collection<Namespace> namespaces, String query);

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
	default TupleQueryResult sparqlSelect(Collection<Namespace> namespaces, String query) throws QueryFailedException {
		try (TupleQuery select = prepareSelect(namespaces, query)) {
			return sparqlSelect(select);
		}
	}

	/**
	 * Executes the given prepared SELECT query.
	 *
	 * @param query the SELECT query to be executed
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(TupleQuery query) throws QueryFailedException {
		// we lock/unlock after select, to avoid that we have locked, but there is no result to unlock again
		return query.evaluate();
	}

	/**
	 * Executes the given prepared SELECT query. The specified variable bindings are applied to the query before
	 * executed, and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the
	 * same prepared query.
	 *
	 * @param query  the SELECT query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(TupleQuery query, String param1, Value value1) throws QueryFailedException {
		return sparqlSelect(query, Collections.singletonMap(param1, value1));
	}

	/**
	 * Executes the given prepared SELECT query. The specified variable bindings are applied to the query before
	 * executed, and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the
	 * same prepared query.
	 *
	 * @param query  the SELECT query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @param param2 the second query variable to bind
	 * @param value2 the value of the second query variable
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(TupleQuery query, String param1, Value value1, String param2, Value value2) throws QueryFailedException {
		return sparqlSelect(query, ImmutableMap.of(param1, value1, param2, value2));
	}

	/**
	 * Executes the given prepared SELECT query. The specified variable bindings are applied to the query before
	 * executed, and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the
	 * same prepared query.
	 *
	 * @param query  the SELECT query to be executed
	 * @param param1 the first query variable to bind
	 * @param value1 the value of the first query variable
	 * @param param2 the second query variable to bind
	 * @param value2 the value of the second query variable
	 * @param param3 the third query variable to bind
	 * @param value3 the value of the third query variable
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(TupleQuery query, String param1, Value value1, String param2, Value value2, String param3, Value value3) throws QueryFailedException {
		return sparqlSelect(query, ImmutableMap.of(param1, value1, param2, value2, param3, value3));
	}

	/**
	 * Executes the given prepared SELECT query. The specified variable bindings are applied to the query before
	 * executed, and will be removed afterwards. The method is thread-safe, if multiple threads trying to execute the
	 * same prepared query.
	 *
	 * @param query    the SELECT query to be executed
	 * @param bindings the variable bindings to be used when executing the perpared query
	 * @return the result of the SELECT query
	 * @throws QueryFailedException if the execution was not successful
	 */
	default TupleQueryResult sparqlSelect(TupleQuery query, Map<String, Value> bindings) throws QueryFailedException {
		// return a cached instance, because modifying and re-query a prepared query,
		// during iteration, may throw a concurrent modification exception
		return query.evaluate(bindings);
	}

	/**
	 * Prepares the given sparql query. All known namespaces will automatically be prepended as prefixes.
	 *
	 * @param query the sparql query to be prepared
	 * @return the prepared query
	 */
	default TupleQuery prepareSelect(String query) throws RepositoryException, MalformedQueryException {
		return prepareSelect(getNamespaces(), query);
	}

	/**
	 * Prepares the given sparql query. Only the given namespaces will automatically be prepended as prefixes.
	 *
	 * @param query      the sparql query to be prepared
	 * @param namespaces the namespaces to prepend as prefixes
	 * @return the prepared query
	 */
	TupleQuery prepareSelect(Collection<Namespace> namespaces, String query) throws RepositoryException, MalformedQueryException;

	/**
	 * Returns the value factory for the given endpoint.
	 *
	 * @return a value factory
	 */
	ValueFactory getValueFactory();

	/**
	 * De-resolves a specified uri to a short uri name. If there is no matching namespace, the full uri is returned.
	 *
	 * @param uri the uri to be de-resolved
	 * @return the short uri name
	 * @created 13.11.2013
	 */
	@NotNull
	default IRI toShortIRI(java.net.URI uri) {
		return toShortIRI(getValueFactory().createIRI(uri.toString()));
	}

	/**
	 * De-resolves a specified uri to a short uri name. If there is no matching namespace, the full uri is returned.
	 *
	 * @param iri the uri to be de-resolved
	 * @return the short uri name
	 * @created 13.11.2013
	 */
	@NotNull
	IRI toShortIRI(IRI iri);

	/**
	 * Executes the sparql query, and dumps the result to the console, as a human-readable ascii formatted table. The
	 * bound variables are in the title of the table, the column widths are adjusted to the content of each column. URI
	 * references are abbreviated as the namespace is known to this core.
	 *
	 * @param query the sparql query to be executed
	 */
	void dump(String query);

	@Override
	void close() throws RepositoryException;

	class QueryFailedException extends RuntimeException {
		private static final long serialVersionUID = 6858265382899856794L;

		public QueryFailedException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
