package com.denkbares.semanticcore.jena;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;

import com.denkbares.semanticcore.config.RepositoryConfig;

/**
 * Default config for jena repository in sesame!
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class JenaConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		// create a configuration for the SAIL stack
		SailImplConfig backendConfig = new MemoryStoreConfig();

		// create a configuration for the repository implementation
		RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
		return new org.openrdf.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_JENA";
	}
}
