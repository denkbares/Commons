package com.denkbares.semanticcore.jena;

import java.io.File;

import org.apache.jena.rdf.model.Model;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryBase;

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
