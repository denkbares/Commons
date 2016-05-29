package com.denkbares.semanticcore.config;

import java.util.Map;

import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;

/**
 * Interface for repository configurations.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public interface RepositoryConfig {

	/**
	 * Creates a repository configuration for this reasoning.
	 *
	 * @param repositoryId    the id the repository should have
	 * @param repositoryLabel the label the repository should have
	 * @param overrides       optional overrides
	 * @return a RepositoryConfig for this reasoning
	 * @throws RepositoryConfigException
	 */
	org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException;

	/**
	 * The name or label for this reasoning. Can be used in UI e.g. to select reasoning in a drop down.
	 *
	 * @return the name of this reasoning.
	 */
	String getName();

	/**
	 * Allows to perform additional operations on the created repository.
	 *
	 * @param repository the repository created by this reasoning
	 */
	default void postCreationHook(Repository repository) {
		// by default, just do nothing
	}

}
