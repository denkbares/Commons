/*
 * Copyright (C) 2016 denkbares GmbH, Germany
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

package com.denkbares.semanticcore.utils.hierarchy;

import java.net.URI;

import com.denkbares.semanticcore.sparql.SPARQLBooleanQuery;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.collections.PartialHierarchy;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 18.02.16.
 */
public class SubClassHierarchy implements PartialHierarchy<URI> {
	private final SPARQLEndpoint core;
	private final String subClassRelation;

	public SubClassHierarchy(SPARQLEndpoint core, String subClassRelation) {
		this.core = core;
		this.subClassRelation = subClassRelation;
	}

	@Override
	public boolean isSuccessorOf(URI node1, URI node2) {
		String query = "ASK { <" + node1 + "> " + subClassRelation + " <" + node2 + "> }";
		SPARQLBooleanQuery sparqlBooleanQuery = core.prepareBooleanQuery(query);
		return sparqlBooleanQuery.evaluate();
	}
}
