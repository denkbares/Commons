package com.denkbares.semanticcore.jena;

import java.util.List;
import java.util.Map;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import org.apache.jena.rdf.model.Model;
import org.jetbrains.annotations.NotNull;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailConnectionBase;

import static java.util.stream.Collectors.toList;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSailConnection extends SailConnectionBase {

	private final JenaSail jenaSail;
	private final Model model;

	public JenaSailConnection(JenaSail jenaSail) {
		super(jenaSail);
		this.jenaSail = jenaSail;
		this.model = jenaSail.getModel();
	}

	@Override
	protected void closeInternal() throws SailException {

	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		return null;
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
		throw getUnsupportedOperationException();
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		return null;
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		return 0;
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		model.begin();
	}

	@Override
	protected void commitInternal() throws SailException {
		model.commit();
	}

	@Override
	protected void rollbackInternal() throws SailException {

	}

	@Override
	protected void addStatementInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.add(toStatement(subj, pred, obj));

	}

	private org.apache.jena.rdf.model.Statement toStatement(Resource subj, URI pred, Value obj) {
		return model.createStatement(
				model.createResource(subj.stringValue()),
				model.createProperty(pred.stringValue()),
				JenaUtils.valueToNode(model, obj));
	}

	@Override
	protected void removeStatementsInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.remove(toStatement(subj, pred, obj));
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.removeAll();
	}

	private void checkContexts(Resource[] contexts) {
		if (contexts.length > 0) {
			throw getUnsupportedOperationException();
		}
	}

	@NotNull
	private UnsupportedOperationException getUnsupportedOperationException() {
		return new UnsupportedOperationException("Contexts are not yet supported");
	}

	@Override
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
		Map<String, String> nsPrefixMap = model.getNsPrefixMap();
		// TODO: this can be done better, e.g. create iterator creating namespaces on call of next(), use caching...
		List<NamespaceImpl> namespaces = nsPrefixMap.entrySet()
				.stream()
				.map(entry -> new NamespaceImpl(entry.getKey(), entry.getValue()))
				.collect(toList());
		return new CloseableIteratorIteration<>(namespaces.iterator());
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		return model.getNsPrefixURI(prefix);
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		model.setNsPrefix(prefix, name);
	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {
		model.removeNsPrefix(prefix);
	}

	@Override
	protected void clearNamespacesInternal() throws SailException {
		model.getNsPrefixMap().keySet().forEach(model::removeNsPrefix);
	}
}
