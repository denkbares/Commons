package com.denkbares.semanticcore.jena.sail;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.SailBase;

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
		return ValueFactoryImpl.getInstance();
	}

	Model getModel() {
		return model;
	}
}
