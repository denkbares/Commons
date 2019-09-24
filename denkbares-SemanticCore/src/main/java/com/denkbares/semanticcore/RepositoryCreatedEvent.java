/*
 * Copyright (C) 2019 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

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
