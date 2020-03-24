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

package com.denkbares.semanticcore;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Implements a (prepared) boolean query, that holds the original connection, and closes the connection as the query is
 * closed.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 21.03.2020
 */
public class BooleanQuery implements org.eclipse.rdf4j.query.BooleanQuery, AutoCloseable {

	private final RepositoryConnection connection;
	private final org.eclipse.rdf4j.query.BooleanQuery delegate;
	private final String queryString;

	public BooleanQuery(RepositoryConnection connection, org.eclipse.rdf4j.query.BooleanQuery delegate, String queryString) {
		this.connection = connection;
		this.delegate = delegate;
		this.queryString = queryString;
	}

	/**
	 * Returns the connection used to create the query.
	 */
	public RepositoryConnection getConnection() {
		return connection;
	}

	/**
	 * Returns the original query string, this query is created from
	 */
	public String getQueryString() {
		return queryString;
	}

	@Override
	public void close() {
		connection.close();
	}

	@Override
	public synchronized boolean evaluate() throws QueryEvaluationException {
		return delegate.evaluate();
	}

	/**
	 * Method that evaluates this (prepared) query with a set of predefined variable bindings.
	 *
	 * @param bindings the bindings to be applied before the query is executed
	 * @return the query result
	 */
	public synchronized boolean evaluate(Map<String, Value> bindings) throws QueryEvaluationException {
		try {
			bindings.forEach(this::setBinding);
			return delegate.evaluate();
		}
		finally {
			bindings.keySet().forEach(this::removeBinding);
		}
	}

	@Override
	public void setMaxQueryTime(int maxQueryTime) {
		//noinspection deprecation
		delegate.setMaxQueryTime(maxQueryTime);
	}

	@Override
	public int getMaxQueryTime() {
		return delegate.getMaxQueryTime();
	}

	@Override
	public void setBinding(String name, Value value) {
		delegate.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		delegate.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		delegate.clearBindings();
	}

	@Override
	public BindingSet getBindings() {
		return delegate.getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		delegate.setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return delegate.getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		delegate.setIncludeInferred(includeInferred);
	}

	@Override
	public boolean getIncludeInferred() {
		return delegate.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		delegate.setMaxExecutionTime(maxExecTime);
	}

	@Override
	public int getMaxExecutionTime() {
		return delegate.getMaxExecutionTime();
	}
}
