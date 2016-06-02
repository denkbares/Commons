package com.denkbares.semanticcore.config;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.openrdf.sail.spin.config.SpinSailConfig;

/**
 * Config to create a sesame SPIN sail with no prior SPARQLs defined and based on RDF reasoning.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 02.06.16
 */
public class SpinRdfConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		SailImplConfig spinSailConfig = new SpinSailConfig(new MemoryStoreConfig());
		RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(spinSailConfig);
		return new org.openrdf.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_SPIN_SESAME";
	}
}
