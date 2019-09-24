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

package com.denkbares.semanticcore.graphdb;

/**
 * To provide compatibility with a no longer existing rule-set OWL2_RL_REDUCED_OPTIMIZED, we use the adapted horst
 * rule-set. It is GraphDB's build-in owl-horst-optimized profile, extended with property-chains.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class Owl2RlReducedOptimizedConfig extends GraphDBConfig {

	public Owl2RlReducedOptimizedConfig() {
		super("/builtin_Rules-horst-optimized-with-property-chains.pie");
	}

	@Override
	public String getName() {
		return "OWL2_RL_REDUCED_OPTIMIZED";
	}
}
