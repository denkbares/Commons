package com.denkbares.semanticcore.config;

import java.util.Map;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;

/**
 * Default repository config class which actually does not do any reasoning. The repository is FileSystem based to be easy on the memory.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RdfFSConfig implements RepositoryConfig {

	@Override
	public org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		// create a configuration for the SAIL stack
		SailImplConfig backendConfig = new NativeStoreConfig();

		// create a configuration for the repository implementation
		RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
		return new org.eclipse.rdf4j.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_FS_SESAME";
	}
}
