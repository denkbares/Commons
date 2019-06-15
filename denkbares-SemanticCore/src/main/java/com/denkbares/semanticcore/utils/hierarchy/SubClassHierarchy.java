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

import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MultiMap;
import com.denkbares.collections.MultiMaps;
import com.denkbares.collections.PartialHierarchy;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.sparql.SPARQLQueryResult;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 18.02.16.
 */
public class SubClassHierarchy implements PartialHierarchy<URI> {

	private final MultiMap<String, String> subClassCache = new DefaultMultiMap<>(MultiMaps.minimizedFactory(), MultiMaps.minimizedFactory());

	public SubClassHierarchy(SPARQLEndpoint core, String subClassRelation) {
//		String query = "ASK { <" + node1 + "> " + subClassRelation + " <" + node2 + "> }";
		String query = "SELECT ?node1 ?node2 WHERE { ?node1 " + subClassRelation + " ?node2 }";
		try (SPARQLQueryResult queryResult = core.execute(core.prepareQuery(query))) {
			TupleQueryResult result = queryResult.getResult();
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				Binding node1 = bindingSet.getBinding("node1");
				Binding node2 = bindingSet.getBinding("node2");
				subClassCache.put(node1.getValue().stringValue(), node2.getValue().stringValue());
			}
		}
	}

	@Override
	public boolean isSuccessorOf(URI node1, URI node2) {
		return subClassCache.getValues(node1.toString()).contains(node2.toString());
	}
}
