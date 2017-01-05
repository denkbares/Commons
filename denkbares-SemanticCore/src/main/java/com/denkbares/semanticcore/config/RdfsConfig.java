package com.denkbares.semanticcore.config;

import java.util.Map;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;

/**
 * Config sesame rdfs reasoning.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class RdfsConfig implements RepositoryConfig {

	@Override
	public org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		// create a configuration for the SAIL stack
		SailImplConfig backendConfig = new MemoryStoreConfig();

		// create a configuration for the repository implementation
		SailRepositoryConfig repositoryTypeSpec = new SailRepositoryConfig(new ForwardChainingRDFSInferencerConfig(backendConfig));

		return new org.eclipse.rdf4j.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDFS_SESAME";
	}
}
