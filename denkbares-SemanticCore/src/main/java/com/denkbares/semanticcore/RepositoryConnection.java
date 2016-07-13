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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;

import info.aduna.iteration.Iteration;
import org.jetbrains.annotations.NotNull;
import org.openrdf.IsolationLevel;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

import com.denkbares.utils.Log;

/**
 * This is a delegate for the ordinary {@link org.openrdf.repository.RepositoryException}.
 * Tries to close delegate query result when garbage collected. Since we cannot guaranty garbage collection of the
 * object, we still need to use <tt>try(ClosingQueryResult result = getResult(..)) { code }</tt>
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 28.01.16
 */
public class RepositoryConnection implements org.openrdf.repository.RepositoryConnection, AutoCloseable {

	private final org.openrdf.repository.RepositoryConnection connection;

	private static final ThreadLocal<Integer> queryCounter = ThreadLocal.withInitial(() -> 0);

	public RepositoryConnection(org.openrdf.repository.RepositoryConnection connection) {
		this.connection = connection;
	}

	@Override
	public Repository getRepository() {
		return connection.getRepository();
	}

	@Override
	public void setParserConfig(ParserConfig config) {
		connection.setParserConfig(config);
	}

	@Override
	public ParserConfig getParserConfig() {
		return connection.getParserConfig();
	}

	@Override
	public ValueFactory getValueFactory() {
		return connection.getValueFactory();
	}

	@Override
	public boolean isOpen() throws RepositoryException {
		return connection.isOpen();
	}

	@Override
	public void close() throws RepositoryException {
		connection.close();
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException {
		return connection.prepareQuery(ql, query);
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		return connection.prepareQuery(ql, query, baseURI);
	}

	@Override
	public com.denkbares.semanticcore.TupleQuery prepareTupleQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException {
		return new com.denkbares.semanticcore.TupleQuery(new CounterTupleQuery(connection.prepareTupleQuery(ql, query)));
	}

	@Override
	public com.denkbares.semanticcore.TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		return new com.denkbares.semanticcore.TupleQuery(new CounterTupleQuery(connection.prepareTupleQuery(ql, query, baseURI)));
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException {
		return connection.prepareGraphQuery(ql, query);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		return connection.prepareGraphQuery(ql, query, baseURI);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query) throws RepositoryException, MalformedQueryException {
		return connection.prepareBooleanQuery(ql, query);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		return connection.prepareBooleanQuery(ql, query, baseURI);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update) throws RepositoryException, MalformedQueryException {
		return connection.prepareUpdate(ql, update);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI) throws RepositoryException, MalformedQueryException {
		return connection.prepareUpdate(ql, update, baseURI);
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		return connection.getContextIDs();
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
		return connection.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
		return connection.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts) throws RepositoryException {
		return connection.hasStatement(st, includeInferred, contexts);
	}

	@Override
	public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred, RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		connection.exportStatements(subj, pred, obj, includeInferred, handler, contexts);
	}

	@Override
	public void export(RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		connection.export(handler, contexts);
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		return connection.size(contexts);
	}

	@Override
	public boolean isEmpty() throws RepositoryException {
		return connection.isEmpty();
	}

	@SuppressWarnings("deprecation")
	@Override
	@Deprecated
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		connection.setAutoCommit(autoCommit);
	}

	@SuppressWarnings("deprecation")
	@Override
	@Deprecated
	public boolean isAutoCommit() throws RepositoryException {
		return connection.isAutoCommit();
	}

	@Override
	public boolean isActive() throws RepositoryException {
		return connection.isActive();
	}

	@Override
	public void setIsolationLevel(IsolationLevel isolationLevel) throws IllegalStateException {
		connection.setIsolationLevel(isolationLevel);
	}

	@Override
	public IsolationLevel getIsolationLevel() {
		return connection.getIsolationLevel();
	}

	@Override
	public void begin() throws RepositoryException {
		connection.begin();
	}

	@Override
	public void begin(IsolationLevel isolationLevel) throws RepositoryException {
		connection.begin(isolationLevel);
	}

	@Override
	public void commit() throws RepositoryException {
		connection.commit();
	}

	@Override
	public void rollback() throws RepositoryException {
		connection.rollback();
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
		increaseQueryCounter();
		connection.add(in, baseURI, dataFormat, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
		increaseQueryCounter();
		connection.add(reader, baseURI, dataFormat, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
		increaseQueryCounter();
		connection.add(url, baseURI, dataFormat, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts) throws IOException, RDFParseException, RepositoryException {
		increaseQueryCounter();
		connection.add(file, baseURI, dataFormat, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.add(subject, predicate, object, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.add(st, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.add(statements, contexts);
		decreaseQueryCounter();
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts) throws RepositoryException, E {
		increaseQueryCounter();
		connection.add(statements, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void remove(Resource subject, URI predicate, Value object, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.remove(subject, predicate, object, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.remove(st, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts) throws RepositoryException {
		increaseQueryCounter();
		connection.remove(statements, contexts);
		decreaseQueryCounter();
	}

	@Override
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statements, Resource... contexts) throws RepositoryException, E {
		increaseQueryCounter();
		connection.remove(statements, contexts);
		decreaseQueryCounter();
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		connection.clear(contexts);
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		return connection.getNamespaces();
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		return connection.getNamespace(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		connection.setNamespace(prefix, name);
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		connection.removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		connection.clearNamespaces();
	}

	public static void closeQuietly(@NotNull org.openrdf.repository.RepositoryConnection connection) {
		try {
			connection.close();
		}
		catch (Exception e) {
			Log.severe("Exception during close()", e);
		}
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	private void increaseQueryCounter() {
		int value = queryCounter.get() + 1;
		if (value > 1) {
//			System.out.println("++#################################################++");
//			System.out.println("Multiple simultaneous queries in same thread (" + Thread.currentThread()
//					.getName() + ")!!! Counter: " + value);
//			StackTraceElement[] stackTrace = new Exception().getStackTrace();
//			System.out.println(Strings.concat("\n\t", stackTrace));
//			System.out.println("--#################################################--");
		}
		queryCounter.set(value);
	}

	private void decreaseQueryCounter() {
		int value = queryCounter.get() - 1;
		if (value > 0) {
//			System.out.println("Multiple simultaneous queries in same thread (" + Thread.currentThread()
//					.getName() + ") decreased: " + value);
		}
		queryCounter.set(value);
	}

	private class CounterTupleQuery implements TupleQuery {

		private final TupleQuery query;

		public CounterTupleQuery(TupleQuery query) {
			this.query = query;
		}

		@Override
		public org.openrdf.query.TupleQueryResult evaluate() throws QueryEvaluationException {
			increaseQueryCounter();
			return new CountingTupleQueryResult(query.evaluate());
		}

		@Override
		public void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
			increaseQueryCounter();
			query.evaluate(new CountingTupleQueryResultHandler(handler));
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setMaxQueryTime(int maxQueryTime) {
			query.setMaxQueryTime(maxQueryTime);
		}

		@SuppressWarnings("deprecation")
		@Override
		public int getMaxQueryTime() {
			return query.getMaxQueryTime();
		}

		@Override
		public void setBinding(String name, Value value) {
			query.setBinding(name, value);
		}

		@Override
		public void removeBinding(String name) {
			query.removeBinding(name);
		}

		@Override
		public void clearBindings() {
			query.clearBindings();
		}

		@Override
		public BindingSet getBindings() {
			return query.getBindings();
		}

		@Override
		public void setDataset(Dataset dataset) {
			query.setDataset(dataset);
		}

		@Override
		public Dataset getDataset() {
			return query.getDataset();
		}

		@Override
		public void setIncludeInferred(boolean includeInferred) {
			query.setIncludeInferred(includeInferred);
		}

		@Override
		public boolean getIncludeInferred() {
			return query.getIncludeInferred();
		}

		@Override
		public void setMaxExecutionTime(int maxExecTime) {
			query.setMaxExecutionTime(maxExecTime);
		}

		@Override
		public int getMaxExecutionTime() {
			return query.getMaxExecutionTime();
		}
	}

	private class CountingTupleQueryResult implements TupleQueryResult {

		private final TupleQueryResult tupleQueryResult;

		public CountingTupleQueryResult(TupleQueryResult tupleQueryResult) {
			this.tupleQueryResult = tupleQueryResult;
		}

		@Override
		public List<String> getBindingNames() throws QueryEvaluationException {
			return tupleQueryResult.getBindingNames();
		}

		@Override
		public void close() throws QueryEvaluationException {
			decreaseQueryCounter();
			tupleQueryResult.close();
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return tupleQueryResult.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			return tupleQueryResult.next();
		}

		@Override
		public void remove() throws QueryEvaluationException {
			tupleQueryResult.remove();
		}
	}

	private class CountingTupleQueryResultHandler implements TupleQueryResultHandler {

		private final TupleQueryResultHandler tupleQueryResultHandler;

		public CountingTupleQueryResultHandler(TupleQueryResultHandler tupleQueryResultHandler) {
			this.tupleQueryResultHandler = tupleQueryResultHandler;
		}

		@Override
		public void handleBoolean(boolean value) throws QueryResultHandlerException {
			tupleQueryResultHandler.handleBoolean(value);
		}

		@Override
		public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
			tupleQueryResultHandler.handleLinks(linkUrls);
		}

		@Override
		public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
			tupleQueryResultHandler.startQueryResult(bindingNames);
		}

		@Override
		public void endQueryResult() throws TupleQueryResultHandlerException {
			tupleQueryResultHandler.endQueryResult();
			decreaseQueryCounter();
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
			tupleQueryResultHandler.handleSolution(bindingSet);
		}
	}
}
