package com.denkbares.semanticcore.jena.sail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.jetbrains.annotations.NotNull;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailConnectionBase;

import com.denkbares.semanticcore.jena.JenaUtils;

import static com.denkbares.semanticcore.jena.JenaUtils.sesame2Jena;
import static java.util.stream.Collectors.toList;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSailConnection extends SailConnectionBase {

	private final JenaSail jenaSail;
	private final Model model;
	private final ValueFactory valueFactory;

	public JenaSailConnection(JenaSail jenaSail) {
		super(jenaSail);
		this.jenaSail = jenaSail;
		valueFactory = jenaSail.getValueFactory();
		this.model = jenaSail.getModel();
	}

	@Override
	protected void closeInternal() throws SailException {
		// nothing to do
	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		return new CloseableIteratorIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
		throw getUnsupportedOperationException();
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		StmtIterator stmtIterator = model.listStatements(sesame2Jena(model, subj), sesame2Jena(model, pred), sesame2Jena(model, obj));
		return new CloseableIteratorIteration<>(new Iterator<Statement>() {
			@Override
			public boolean hasNext() {
				return stmtIterator.hasNext();
			}

			@Override
			public Statement next() {
				return JenaUtils.jena2Sesame(valueFactory, stmtIterator.next());
			}
		});
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		return model.size();
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		if (model.supportsTransactions()) {
			model.begin();
		}
	}

	@Override
	protected void commitInternal() throws SailException {
		if (model.supportsTransactions()) {
			model.commit();
		}
	}

	@Override
	protected void rollbackInternal() throws SailException {
		if (model.supportsTransactions()) {
			model.abort();
		}
	}

	@Override
	protected void addStatementInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.add(JenaUtils.toStatement(model, subj, pred, obj));

	}


	@Override
	protected void removeStatementsInternal(Resource subj, URI pred, Value obj, Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.remove(JenaUtils.toStatement(model, subj, pred, obj));
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		checkContexts(contexts);
		model.removeAll();
	}

	private void checkContexts(Resource[] contexts) {
		if (Arrays.stream(contexts).filter(context -> context != null).count() > 0) {
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
