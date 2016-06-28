package com.denkbares.semanticcore.jena;

import info.aduna.iteration.CloseableIteration;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultImpl;

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
