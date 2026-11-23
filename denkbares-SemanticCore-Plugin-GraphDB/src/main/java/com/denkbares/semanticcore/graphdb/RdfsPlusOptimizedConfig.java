/*
 * Copyright (C) 2026 denkbares GmbH, Germany
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

package com.denkbares.semanticcore.graphdb;

/**
 * Repository config with GraphDB's build-in rdfsplus-optimized profile: RDFS with inverse, symmetric and transitive
 * properties, owl:sameAs and equivalent classes and properties, but without the more expensive rules of OWL-Horst.
 *
 * @author Albrecht Striffler (denkbares GmbH) + Claude
 * @created 23.11.2026
 */
public class RdfsPlusOptimizedConfig extends GraphDBConfig {

	public RdfsPlusOptimizedConfig() {
		super("rdfsplus-optimized");
	}
}
