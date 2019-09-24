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

package com.denkbares.semanticcore.jena;

import org.apache.jena.shared.ConfigException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.jetbrains.annotations.NotNull;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public abstract class AbstractJenaRepositoryFactory implements RepositoryFactory {

	private final String repositoryType;

	public AbstractJenaRepositoryFactory(String type) {
		repositoryType = type;
	}

	@Override
	public String getRepositoryType() {
		return repositoryType;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new AbstractRepositoryImplConfig(repositoryType);
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (!repositoryType.equals(config.getType())) {
			throw new ConfigException("Invalid repository type: " + config.getType());
		}
		return getRepository();
	}

	@NotNull
	protected abstract Repository getRepository();

}
