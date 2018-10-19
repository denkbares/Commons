package com.denkbares.semanticcore;

import org.eclipse.rdf4j.repository.Repository;

import com.denkbares.events.Event;
import com.denkbares.semanticcore.config.RepositoryConfig;

/**
 * Gets fired every time a new {@link Repository} is created in the {@link SemanticCore}.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 11.07.16
 */
public class RepositoryCreatedEvent implements Event {
	private final RepositoryConfig repositoryConfig;
	private final Repository repository;

	public RepositoryCreatedEvent(RepositoryConfig repositoryConfig, Repository repository) {
		this.repositoryConfig = repositoryConfig;
		this.repository = repository;
	}

	public RepositoryConfig getRepositoryConfig() {
		return repositoryConfig;
	}

	public Repository getRepository() {
		return repository;
	}
}
