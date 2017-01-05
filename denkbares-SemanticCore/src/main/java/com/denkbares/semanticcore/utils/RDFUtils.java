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

package com.denkbares.semanticcore.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.denkbares.semanticcore.sparql.SPARQLBooleanQuery;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.sparql.SPARQLQueryResult;
import com.denkbares.utils.Log;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 19.01.16.
 */
public class RDFUtils {

	public static boolean isClass(SPARQLEndpoint core, URI resource) {
		String query = "ASK { <" + resource + "> rdf:type rdfs:Class .}";
		SPARQLBooleanQuery sparqlBooleanQuery = core.prepareBooleanQuery(query);
		return sparqlBooleanQuery.evaluate();
	}

	public static boolean isProperty(SPARQLEndpoint core, URI resource) {
		String query = "ASK { <" + resource + "> rdf:type rdf:Property .}";
		SPARQLBooleanQuery sparqlBooleanQuery = core.prepareBooleanQuery(query);
		return sparqlBooleanQuery.evaluate();
	}

	public static Collection<URI> getClasses(SPARQLEndpoint core, URI instance) {
		String query = "SELECT ?class WHERE { <" + instance + "> rdf:type ?class .}";
		List<URI> resultCollection = new ArrayList<>();
		try (SPARQLQueryResult queryResult = core.execute(core.prepareQuery(query))) {
			TupleQueryResult result = queryResult.getResult();
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				URI aClass = Sparqls.asURI(bindingSet, "class");
				resultCollection.add(aClass);
			}
		}
		catch (QueryEvaluationException e) {
			Log.severe("Exception while getting classes.", e);
		}
		return resultCollection;
	}
}
