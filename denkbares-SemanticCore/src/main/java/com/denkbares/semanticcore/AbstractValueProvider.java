/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import de.d3web.utils.Log;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @since 23.10.2014
 */
public abstract class AbstractValueProvider implements ValueProvider {

	private final TupleQueryResult queryResult;
	private BindingSet currentBindings;

	public AbstractValueProvider(RepositoryConnection connection, String queryFile, String language, Class<?> c) throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		com.denkbares.semanticcore.TupleQuery query = loadQuery(connection, queryFile, language, c);
		queryResult = query.evaluate().cachedAndClosed();
	}

	private com.denkbares.semanticcore.TupleQuery loadQuery(RepositoryConnection connection, String fileName, String langID, Class<?> c)
			throws RepositoryException, MalformedQueryException {

		String query = SPARQLLoader.load(fileName, c);
		query = query.replaceAll("###LANG###", langID);
		return connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
	}

	@Override
	public boolean hasNext() {
		try {
			return queryResult.hasNext();
		}
		catch (QueryEvaluationException e) {
			Log.warning("Couldn't check if there are more results!", e);
		}
		return false;
	}

	@Override
	public BindingSet next() {
		try {
			do {
				currentBindings = queryResult.next();
			} while (!accept(currentBindings) && hasNext());
			return currentBindings;
		}
		catch (QueryEvaluationException e) {
			Log.warning("Couldn't retrieve next BindingSet!", e);
		}
		return null;
	}

	protected abstract boolean accept(BindingSet currentBindings);

	@Override
	public Value value(String variable) {
		return currentBindings.getValue(variable);
	}

	@Override
	public String stringValue(String variable) {
		return value(variable).stringValue();
	}

	@Override
	public Map<String, Value> values() {
		final Map<String, Value> values = new HashMap<>();
		currentBindings.forEach(t -> values.put(t.getName(), t.getValue()));
		return values;
	}

}
