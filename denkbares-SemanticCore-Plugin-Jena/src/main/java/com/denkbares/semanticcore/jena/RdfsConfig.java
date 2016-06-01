package com.denkbares.semanticcore.jena;

import java.util.Map;

import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;

import com.denkbares.semanticcore.config.RepositoryConfig;

/**
 * Rdfs config for jena repository in sesame!
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 20.05.16
 */
public class RdfsConfig implements RepositoryConfig {

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {

		// create a configuration for the repository implementation
		return new org.openrdf.repository.config.RepositoryConfig(repositoryId,
				new RepositoryImplConfigBase(RdfsJenaRepositoryFactory.TYPE));
	}

	@Override
	public String getName() {
		return "RDFS_JENA";
	}
}
