package com.denkbares.semanticcore.jena;

import java.io.File;

import org.apache.jena.rdf.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryBase;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public class JenaRepository extends RepositoryBase {

	final Model model;
	private File dataDir;

	public JenaRepository(Model model) {
		this.model = model;
	}

	@Override
	protected void initializeInternal() throws RepositoryException {
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		model.close();
	}

	@Override
	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return true;
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return new JenaRepositoryConnection(this);
	}

	@Override
	public ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	Model getModel() {
		return model;
	}
}
