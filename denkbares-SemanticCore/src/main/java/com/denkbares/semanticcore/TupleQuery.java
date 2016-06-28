/*
 * Copyright (C) 2016 denkbares GmbH, Germany
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

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 29.01.16
 */
public class TupleQuery implements org.openrdf.query.TupleQuery {

	private final org.openrdf.query.TupleQuery tupleQuery;

	public TupleQuery(org.openrdf.query.TupleQuery tupleQuery) {
		this.tupleQuery = tupleQuery;
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		return new TupleQueryResult(null, tupleQuery.evaluate());
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
		// TODO: do we need to close here somehow?
		tupleQuery.evaluate(handler);
	}

	@Override
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime) {
		tupleQuery.setMaxQueryTime(maxQueryTime);
	}

	@Override
	@Deprecated
	public int getMaxQueryTime() {
		return tupleQuery.getMaxQueryTime();
	}

	@Override
	public void setBinding(String name, Value value) {
		tupleQuery.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		tupleQuery.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		tupleQuery.clearBindings();
	}

	@Override
	public BindingSet getBindings() {
		return tupleQuery.getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		tupleQuery.setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return tupleQuery.getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		tupleQuery.setIncludeInferred(includeInferred);
	}

	@Override
	public boolean getIncludeInferred() {
		return tupleQuery.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		tupleQuery.setMaxExecutionTime(maxExecTime);
	}

	@Override
	public int getMaxExecutionTime() {
		return tupleQuery.getMaxExecutionTime();
	}
}
