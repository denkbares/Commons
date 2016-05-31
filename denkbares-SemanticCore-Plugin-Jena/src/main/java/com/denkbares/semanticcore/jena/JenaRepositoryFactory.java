package com.denkbares.semanticcore.jena;

import org.apache.jena.shared.ConfigException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.sail.config.SailFactory;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public class JenaRepositoryFactory implements RepositoryFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String REPOSITORY_TYPE = "jena:DefaultRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new JenaRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (!REPOSITORY_TYPE.equals(config.getType())) {
			throw new ConfigException("Invalid repository type: " + config.getType());
		}
		return new JenaRepository();
	}

}
