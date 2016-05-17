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

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;

import de.d3web.utils.Log;

/**
 * @author Jochen Reutelshöfer
 * @created 30.07.2014
 */
public class SPARQLBooleanQuery {

	private final String queryString;
	private final String graphName;
	private final Query query;

	/**
	 * Creates a new SPARQL query on the default graph
	 *
	 * @param query SPARQL Query String
	 */
	public SPARQLBooleanQuery(String queryString, Query query) {
		this(queryString, "", query);
	}

	/**
	 * Creates a new SPARQL query
	 *
	 * @param query     SPARQL Query String
	 * @param graphName Graph to run the query on
	 */
	public SPARQLBooleanQuery(String queryString, String graphName, Query query) {
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

	public BooleanQuery getBooleanQuery() {
		if (query instanceof BooleanQuery) {
			return (BooleanQuery) query;
		}
		return null;
	}

	public boolean evaluate() {
		if (query instanceof BooleanQuery) {
			try {
				return ((BooleanQuery) query).evaluate();
			}
			catch (QueryEvaluationException e) {
				Log.severe("Exception while evaluation query:\n" + getQueryString(), e);
			}
		}
		return false;
	}
}
