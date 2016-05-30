package com.denkbares.semanticcore.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailBase;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSail extends SailBase {

	private Model model;

	@Override
	protected void initializeInternal() throws SailException {
		model = ModelFactory.createMemModelMaker().createFreshModel();
	}

	@Override
	protected void shutDownInternal() throws SailException {
		model.close();
	}

	@Override
	protected SailConnection getConnectionInternal() throws SailException {
		return new JenaSailConnection(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	@Override
	public ValueFactory getValueFactory() {
		return new ValueFactoryImpl();
	}

	Model getModel() {
		return model;
	}
}
