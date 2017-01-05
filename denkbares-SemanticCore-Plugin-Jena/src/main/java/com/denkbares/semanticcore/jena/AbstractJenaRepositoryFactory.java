package com.denkbares.semanticcore.jena;

import org.apache.jena.shared.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfigBase;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public abstract class AbstractJenaRepositoryFactory implements RepositoryFactory {

	private final String repositoryType;

	public AbstractJenaRepositoryFactory(String type) {
		repositoryType = type;
	}

	@Override
	public String getRepositoryType() {
		return repositoryType;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new RepositoryImplConfigBase(repositoryType);
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (!repositoryType.equals(config.getType())) {
			throw new ConfigException("Invalid repository type: " + config.getType());
		}
		return getRepository();
	}

	@NotNull
	protected abstract Repository getRepository();

}
