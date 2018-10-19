package com.denkbares.semanticcore.jena;

import java.util.Map;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfigBase;

import com.denkbares.semanticcore.config.RepositoryConfig;

/**
 * Default config for jena repository in sesame!
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class RdfConfig implements RepositoryConfig {

	@Override
	public org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {

		// create a configuration for the repository implementation
		return new org.eclipse.rdf4j.repository.config.RepositoryConfig(repositoryId,
				new RepositoryImplConfigBase(RdfJenaRepositoryFactory.TYPE));
	}

	@Override
	public String getName() {
		return "RDF_JENA";
	}
}
