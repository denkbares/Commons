package com.denkbares.semanticcore.config;

import java.util.Map;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.spin.config.SpinSailConfig;

/**
 * Config to create a sesame SPIN sail with no prior SPARQLs defined and based on RDF reasoning.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 02.06.16
 */
public class SpinRdfConfig implements RepositoryConfig {

	@Override
	public org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		SailImplConfig spinSailConfig = new SpinSailConfig(new MemoryStoreConfig());
		RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(spinSailConfig);
		return new org.eclipse.rdf4j.repository.config.RepositoryConfig(repositoryId, repositoryTypeSpec);
	}

	@Override
	public String getName() {
		return "RDF_SPIN_SESAME";
	}
}
