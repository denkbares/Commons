package com.denkbares.semanticcore.jena;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;

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
		return null;
	}

	@Override
	public String getName() {
		return "JENA_RDF";
	}
}
