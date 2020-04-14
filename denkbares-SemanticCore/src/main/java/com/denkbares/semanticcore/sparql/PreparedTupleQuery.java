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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;

import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.TupleQueryResult;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 22.03.2020
 */
public class PreparedTupleQuery {

	private final SPARQLEndpoint endpoint;
	private final String queryString;
	private final Map<String, Class<? extends Value>> queryParameters = new LinkedHashMap<>();

	private TupleQuery query;

	public PreparedTupleQuery(SPARQLEndpoint endpoint, String queryString) {
		this.endpoint = endpoint;
		this.queryString = queryString;
	}

	public TupleQueryResult evaluate(Value... queryParameterValues) {
		if (queryParameterValues.length != queryParameters.size()) {
			throw new IllegalArgumentException("the specified qury parameter values does not match the expected count");
		}
		Map<String, Value> bindings = new LinkedHashMap<>();
		Iterator<Value> valueIterator = Arrays.asList(queryParameterValues).iterator();
		queryParameters.forEach((name, type) -> {
			Value value = valueIterator.next();
			if (!type.isInstance(value)) {
				throw new IllegalArgumentException("the specified qury parameter values does not match the expected count");
			}
			bindings.put(name, value);
		});
		return endpoint.sparqlSelect(query, bindings);
	}

	public void reset() {
		if (query != null) {
			query.close();
			query = null;
		}
	}

	public void prepare() {
		if (query == null) {
			query = endpoint.prepareSelect(queryString);
		}
	}
}
