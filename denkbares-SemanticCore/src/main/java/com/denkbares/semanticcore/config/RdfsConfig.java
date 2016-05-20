package com.denkbares.semanticcore.config;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;

/**
 * Config sesame rdfs reasoning.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class RdfsConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		// create a configuration for the SAIL stack
		SailImplConfig backendConfig = new MemoryStoreConfig();

		// create a configuration for the repository implementation
		SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(new ForwardChainingRDFSInferencerConfig(backendConfig));

		return new org.openrdf.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDFS_SESAME";
	}
}
