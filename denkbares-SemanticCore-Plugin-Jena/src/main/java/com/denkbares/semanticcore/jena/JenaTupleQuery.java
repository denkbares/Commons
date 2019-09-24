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

package com.denkbares.semanticcore.jena;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultImpl;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public class JenaTupleQuery implements TupleQuery {

	private final JenaRepository repository;
	private final QueryExecution query;

	public JenaTupleQuery(JenaRepository repository, QueryExecution query) {
		this.repository = repository;
		this.query = query;
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		ResultSet results = query.execSelect();
		return new TupleQueryResultImpl(results.getResultVars(), new CloseableIteration<BindingSet, QueryEvaluationException>() {

			@Override
			public boolean hasNext() throws QueryEvaluationException {
				return results.hasNext();
			}

			@Override
			public BindingSet next() throws QueryEvaluationException {
				return JenaUtils.jena2Sesame(repository, results.next());
			}

			@Override
			public void remove() throws QueryEvaluationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() throws QueryEvaluationException {
				query.close();
			}
		});
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMaxQueryTime(int maxQueryTime) {
		setMaxExecutionTime(maxQueryTime);
	}

	@Override
	public int getMaxQueryTime() {
		return getMaxExecutionTime();
	}

	@Override
	public void setBinding(String name, Value value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeBinding(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearBindings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BindingSet getBindings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDataset(Dataset dataset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dataset getDataset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getIncludeInferred() {
		return true;
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		if (maxExecTime == 0) maxExecTime--; // compatibility to sesame, where 0 also already equals no time out
		query.setTimeout(maxExecTime * 1000); // sesame specifies timeout in seconds, jena milliseconds
	}

	@Override
	public int getMaxExecutionTime() {
		return (int) query.getTimeout2() * 1000; // sesame specifies timeout in seconds, jena milliseconds
	}
}
