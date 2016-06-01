package com.denkbares.semanticcore.jena;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryImplConfigBase;

import com.denkbares.semanticcore.config.RepositoryConfig;

/**
 * Default config for jena repository in sesame!
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class RdfConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {

		// create a configuration for the repository implementation
		RepositoryImplConfig repositoryTypeSpec = new RepositoryImplConfigBase(JenaRepositoryFactory.REPOSITORY_TYPE);
		return new org.openrdf.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_JENA";
	}
}
