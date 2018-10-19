package com.denkbares.semanticcore.jena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.jetbrains.annotations.NotNull;

import static com.denkbares.semanticcore.jena.JenaUtils.sesame2Jena;
import static com.denkbares.semanticcore.jena.JenaUtils.toStatement;
import static java.util.stream.Collectors.toList;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public class JenaRepositoryConnection extends AbstractRepositoryConnection {

	private final Model model;
	private List<org.apache.jena.rdf.model.Statement> transactionAdd = new ArrayList<>();
	private List<org.apache.jena.rdf.model.Statement> transactionRemove = new ArrayList<>();
	private boolean isActive;

	JenaRepositoryConnection(JenaRepository repository) {
		super(repository);
		this.model = repository.getModel();
	}

	@Override
	public JenaRepository getRepository() {
		return (JenaRepository) super.getRepository();
	}

	@Override
	protected void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		checkContexts(contexts);
		checkIsActive();
		transactionAdd.add(toStatement(model, subject, predicate, object));
	}

	private void checkIsActive() throws RepositoryException {
		if (!isActive) {
			throw new RepositoryException("Repository is not active, call #begin() before adding statements to the repository.");
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		checkIsActive();
		checkContexts(contexts);
		transactionRemove.add(toStatement(model, subject, predicate, object));
	}


	@Override
	public Query prepareQuery(QueryLanguage ql, String queryString, String baseURI) throws RepositoryException, MalformedQueryException {
		checkQueryLanguage(ql);
		org.apache.jena.query.Query query = QueryFactory.create(queryString);
		if (!query.isSelectType()) {
			throw new UnsupportedOperationException();
		}
		return getJenaTupleQuery(query);
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI) throws RepositoryException, MalformedQueryException {
		checkQueryLanguage(ql);
		return getJenaTupleQuery(QueryFactory.create(queryString));
	}

	@NotNull
	private TupleQuery getJenaTupleQuery(org.apache.jena.query.Query query) {
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		return new JenaTupleQuery(getRepository(), queryExecution);
	}

	private void checkQueryLanguage(QueryLanguage ql) {
		if (!ql.equals(QueryLanguage.SPARQL)) {
			throw new UnsupportedOperationException("Query language " + ql.getName() + " is not supported by this repository");
		}
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI) throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI) throws RepositoryException, MalformedQueryException {
		throw new UnsupportedOperationException();
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		throw getContextsNotSupportedException();
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws RepositoryException {
		checkContexts(contexts);
		checkIncludeInferred(includeInferred);
		StmtIterator stmtIterator = model.listStatements(sesame2Jena(model, subj), sesame2Jena(model, pred), sesame2Jena(model, obj));
		return new RepositoryResult<>(new CloseableIteratorIteration<>(new Iterator<Statement>() {
			@Override
			public boolean hasNext() {
				return stmtIterator.hasNext();
			}

			@Override
			public Statement next() {
				return JenaUtils.jena2Sesame(getRepository().getValueFactory(), stmtIterator.next());
			}
		}));
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
		RepositoryResult<Statement> statements = getStatements(subj, pred, obj, includeInferred, contexts);
		handler.startRDF();
		while (statements.hasNext()) {
			handler.handleStatement(statements.next());
		}
		handler.endRDF();
	}

	private void checkIncludeInferred(boolean includeInferred) {
		if (!includeInferred) {
			throw new UnsupportedOperationException("This repository does not support exclusion of inferred statements");
		}
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		return model.size();
	}

	private void checkContexts(Resource[] contexts) {
		if (Arrays.stream(contexts).anyMatch(Objects::nonNull)) {
			throw getContextsNotSupportedException();
		}
	}

	@NotNull
	private UnsupportedOperationException getContextsNotSupportedException() {
		return new UnsupportedOperationException("Contexts are not yet supported");
	}

	@Override
	public boolean isActive() throws RepositoryException {
		return isActive;
	}

	@Override
	public void begin() throws RepositoryException {
		isActive = true;
		if (model.supportsTransactions()) {
			model.begin();
		}
	}

	@Override
	public void commit() throws RepositoryException {
		model.remove(transactionRemove);
		model.add(transactionAdd);
		if (model.supportsTransactions()) {
			model.commit();
		}
		resetTransaction();
	}

	private void resetTransaction() {
		transactionAdd = new ArrayList<>();
		transactionRemove = new ArrayList<>();
		isActive = false;
	}

	@Override
	public void rollback() throws RepositoryException {
		if (model.supportsTransactions()) {
			model.abort();
		}
		resetTransaction();
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		Map<String, String> nsPrefixMap = model.getNsPrefixMap();
		// TODO: this can be done better, e.g. create iterator creating namespaces on call of next(), use caching...
		List<Namespace> namespaces = nsPrefixMap.entrySet()
				.stream()
				.map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
				.collect(toList());
		return new RepositoryResult<>(new CloseableIteratorIteration<>(namespaces.iterator()));
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		return model.getNsPrefixURI(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		model.setNsPrefix(prefix, name);
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		model.removeNsPrefix(prefix);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		model.getNsPrefixMap().keySet().forEach(model::removeNsPrefix);
	}
}
