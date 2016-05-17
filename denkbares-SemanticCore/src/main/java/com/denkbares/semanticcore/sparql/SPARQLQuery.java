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

import java.util.Map;

import org.openrdf.model.Value;
import org.openrdf.query.TupleQuery;

import com.denkbares.semanticcore.SemanticCore;

/**
 * A SPARQLQuery contains a query made to the {@link SemanticCore}.
 *
 * @author Alex Legler
 */
public class SPARQLQuery {
	private final String queryString;
	private final String graphName;
	private final TupleQuery query;

	/**
	 * Creates a new SPARQL query on the default graph
	 *
	 * @param query SPARQL Query String
	 */
	public SPARQLQuery(String queryString, TupleQuery query) {
		this(queryString, "", query);
	}

	/**
	 * Creates a new SPARQL query
	 *
	 * @param query     SPARQL Query String
	 * @param graphName Graph to run the query on
	 */
	public SPARQLQuery(String queryString, String graphName, TupleQuery query) {
		this.queryString = queryString;
		this.graphName = graphName;
		this.query = query;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getGraphName() {
		return graphName;
	}

	public TupleQuery getQuery() {
		return query;
	}

	public void setBindings(Map<String, Value> bindings) {
		if (bindings == null || bindings.isEmpty()) return;
		query.clearBindings();
		for (Map.Entry<String, Value> entry : bindings.entrySet()) {
			query.setBinding(entry.getKey(), entry.getValue());
		}
	}
}
