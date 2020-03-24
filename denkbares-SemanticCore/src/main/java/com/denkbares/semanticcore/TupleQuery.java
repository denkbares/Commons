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

package com.denkbares.semanticcore;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

/**
 * Implements a (prepared) tuple query, that holds the original connection, and closes the connection as the query is
 * closed.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 29.01.16
 */
public class TupleQuery implements org.eclipse.rdf4j.query.TupleQuery, AutoCloseable {

	private final RepositoryConnection connection;
	private final org.eclipse.rdf4j.query.TupleQuery tupleQuery;
	private final String queryString;

	// fore prepared statements, we use a lock to avoid multiple parallel executions
	// we require a semaphore, instead of a lock, because the lock/unlock may be executed by different threads
	// we also store the thread currently holding the semaphore to avoid that inner loops will cause dead-locks
//	private final Semaphore lock = new Semaphore(1, true);
//	private Thread locker = null;

	public TupleQuery(RepositoryConnection connection, org.eclipse.rdf4j.query.TupleQuery tupleQuery, String queryString) {
		this.connection = connection;
		this.tupleQuery = tupleQuery;
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

//	/**
//	 * Locks this query until {@link #unlock()} is called. If a lock is already held by an other thread, the method
//	 * waits until the lock is released.
//	 */
//	private void lock() {
//		// a prepared query is not reentrant, so if this is tried, we will fail
//		if (locker == Thread.currentThread()) {
//			throw new IllegalStateException("tried to lock the query twice.\n" +
//					"This would indicate that the lock is not properly unlocked by a previous (completed) query, " +
//					"or that the query is used inside the loop that currently iterates a query result.");
//		}
//		try {
//			lock.acquire();
//			locker = Thread.currentThread();
//		}
//		catch (InterruptedException e) {
//			throw new QueryEvaluationException("interrupted while waiting to lock query", e);
//		}
//	}
//
//	private void unlock() {
//		locker = null;
//		lock.release();
//	}

	@Override
	public void close() {
		connection.close();
	}

	@Override
	public synchronized TupleQueryResult evaluate() throws QueryEvaluationException {
		return new TupleQueryResult(tupleQuery.evaluate());

//		// we first evaluate, and only if successful acquire lock and unlock on close (otherwise no close will happen)
//		TupleQueryResult result = new TupleQueryResult(tupleQuery.evaluate());
//		lock();
//		return result.onClose(this::unlock);
	}

	/**
	 * Method that evaluates this (prepared) query with a set of predefined variable bindings.
	 *
	 * @param bindings the bindings to be applied before the query is executed
	 * @return the query result
	 */
	public synchronized TupleQueryResult evaluate(Map<String, Value> bindings) throws QueryEvaluationException {
		try {
			bindings.forEach(this::setBinding);
			return evaluate().cachedAndClosed();
		}
		finally {
			bindings.keySet().forEach(this::removeBinding);
		}

//		bindings.forEach(this::setBinding);
//		TupleQueryResult result = null;
//		boolean success = false;
//		try {
//			// we first evaluate, and only if successful acquire lock and unlock on close (otherwise no close will happen)
//			result = new TupleQueryResult(tupleQuery.evaluate());
//			lock();
//			success = true;
//			return result.onClose(() -> {
//				bindings.keySet().forEach(this::removeBinding);
//				unlock();
//			});
//		}
//		finally {
//			// is result creation failed, we have to remove the bindings manually
//			if (!success) {
//				if (result != null) result.close();
//				bindings.keySet().forEach(this::removeBinding);
//			}
//		}
	}

	@Override
	public synchronized void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
//		lock();
//		try {
			tupleQuery.evaluate(handler);
//		}
//		finally {
//			unlock();
//		}
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
