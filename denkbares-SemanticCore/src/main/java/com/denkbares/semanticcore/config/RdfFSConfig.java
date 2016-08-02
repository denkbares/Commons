package com.denkbares.semanticcore.config;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.openrdf.sail.nativerdf.config.NativeStoreConfig;

/**
 * Default repository config class which actually does not do any reasoning. The repository is FileSystem based to be easy on the memory.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RdfFSConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		// create a configuration for the SAIL stack
		SailImplConfig backendConfig = new NativeStoreConfig();

		// create a configuration for the repository implementation
		RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);
		return new org.openrdf.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_FS_SESAME";
	}
}
