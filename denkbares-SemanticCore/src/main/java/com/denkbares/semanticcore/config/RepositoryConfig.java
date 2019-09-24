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

package com.denkbares.semanticcore.config;

import java.util.Map;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

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
	 */
	org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException;

	/**
	 * The name or label for this reasoning. Can be used in UI e.g. to select reasoning in a drop down.
	 *
	 * @return the name of this reasoning.
	 */
	String getName();

}
