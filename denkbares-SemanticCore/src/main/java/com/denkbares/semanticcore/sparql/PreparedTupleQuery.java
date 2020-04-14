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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.TupleQueryResult;

/**
 * Utility class for easily create, prepare, and execute prepared sparql queries.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 22.03.2020
 */
public class PreparedTupleQuery {

	private final SPARQLEndpoint endpoint;
	private final String queryString;
	private final Map<String, Class<? extends Value>> queryParameters = new LinkedHashMap<>();

	private TupleQuery query;
	private Reference<TupleQueryResult> recent;

	public PreparedTupleQuery(SPARQLEndpoint endpoint, String queryString) throws RepositoryException, MalformedQueryException {
		this.endpoint = endpoint;
		this.queryString = queryString;
		// prepare to fail early if query is malformed
		prepare();
	}

	/**
	 * Defines the next parameter of the prepared query. The parameter name must not be already defined. The type
	 * specified the type of the value to be used.
	 *
	 * @param parameterName the name of the parameter, as defined in the query String (without leading "?")
	 * @param type          the type of the parameter
	 * @return this instance, to chain method calls
	 */
	public PreparedTupleQuery define(String parameterName, Class<? extends Value> type) {
		if (queryParameters.containsKey(parameterName)) {
			throw new IllegalArgumentException("parameter already defined: " + parameterName);
		}
		queryParameters.put(parameterName, type);
		return this;
	}

	/**
	 * Executes the query with the given parameters. The number and order of the parameters must be the same as
	 * specified using {@link #define(String, Class)}. If the parameter count does not match, or any of the parameter
	 * types are not of the specified type, an {@link IllegalArgumentException} is thrown.
	 * <p>
	 * Note that it is not allowed execute a prepared query until the result is still used, as the bindings sets will
	 * interfere. Therefor this method also checks that the previous query result must be closed, otherwise an {@link
	 * IllegalStateException} is thrown.
	 *
	 * @param queryParameterValues the values for the previously defined parameters
	 * @return the query result
	 */
	public TupleQueryResult evaluate(Value... queryParameterValues) throws RepositoryException, MalformedQueryException, SPARQLEndpoint.QueryFailedException {
		// check parameter count
		if (queryParameterValues.length != queryParameters.size()) {
			throw new IllegalArgumentException("the specified query parameter values does not match the expected parameter count");
		}

		// prepare parameters anc check their type
		Map<String, Value> bindings = new LinkedHashMap<>();
		Iterator<Value> valueIterator = Arrays.asList(queryParameterValues).iterator();
		queryParameters.forEach((name, type) -> {
			Value value = valueIterator.next();
			if (!type.isInstance(value)) {
				throw new IllegalArgumentException("the parameter ?" + name +
						" with type " + value.getClass().getSimpleName() +
						" does not match the expected type of " + type.getSimpleName());
			}
			bindings.put(name, value);
		});

		// check if the previous query result is already closed (or garbage collected)
		if (recent != null && recent.get() != null) {
			throw new IllegalStateException("the previous query result is not closed yet");
		}

		prepare();
		TupleQueryResult result = endpoint.sparqlSelect(query, bindings).onClose(() -> recent = null);
		// no not block garbage collection, as is also invalidates the recent result
		this.recent = new WeakReference<>(result);
		return result;
	}

	/**
	 * Makes this prepared query to re-prepare the query based on the query string.
	 */
	public void reset() {
		close();
	}

	/**
	 * Closes this prepared query. Multiple calls to the method will be ignored.
	 */
	public void close() {
		if (query != null) {
			query.close();
			query = null;
			recent = null; // allow to execute a new query
		}
	}

	/**
	 * Actually prepares the query of this instance.
	 */
	private void prepare() throws RepositoryException, MalformedQueryException {
		if (query == null) {
			query = endpoint.prepareSelect(queryString);
		}
	}

	@Override
	protected void finalize() {
		close();
	}
}
