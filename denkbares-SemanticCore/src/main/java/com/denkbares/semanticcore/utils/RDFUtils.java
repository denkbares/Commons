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

package com.denkbares.semanticcore.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.jetbrains.annotations.NotNull;

import com.denkbares.semanticcore.RepositoryConnection;
import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.strings.Strings;
import com.denkbares.strings.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 19.01.16.
 */
public class RDFUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(RDFUtils.class);

	public static Text create(String string, Locale language) {
		return new Text(string, language);
	}

	public static Text create(Value value) {
		if (value == null) return null;
		Locale locale = Locale.ROOT;
		if (value instanceof Literal) {
			locale = ((Literal) value).getLanguage().map(Strings::parseLocale).orElse(Locale.ROOT);
		}
		return new Text(value.stringValue(), locale);
	}

	public static boolean isClass(SPARQLEndpoint core, URI resource) {
		String query = "ASK { <" + resource + "> rdf:type rdfs:Class .}";
		return core.sparqlAsk(query);
	}

	public static boolean isProperty(SPARQLEndpoint core, URI resource) {
		String query = "ASK { <" + resource + "> rdf:type rdf:Property .}";
		return core.sparqlAsk(query);
	}

	public static Collection<URI> getClasses(SPARQLEndpoint core, URI instance) {
		String query = "SELECT ?class WHERE { <" + instance + "> rdf:type ?class .}";
		List<URI> resultCollection = new ArrayList<>();
		try (TupleQueryResult result = core.sparqlSelect(query)) {
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				URI aClass = Sparqls.asURI(bindingSet, "class");
				resultCollection.add(aClass);
			}
		}
		catch (QueryEvaluationException e) {
			LOGGER.error("Exception while getting classes.", e);
		}
		return resultCollection;
	}

	/**
	 * Returns all instance of the given classes.
	 *
	 * @param core repository to scan for instances
	 * @param uris classes that instances are detected of
	 * @return all instances of all the given classes
	 */
	public static Collection<IRI> getInstances(RepositoryConnection core, List<URI> uris) {
		String query = createQueryForGetInstances(uris);
		List<IRI> resultCollection = new ArrayList<>();
		TupleQuery tupleQuery = core.prepareTupleQuery(query);
		com.denkbares.semanticcore.TupleQueryResult result = tupleQuery.evaluate();
		while (result.hasNext()) {
			BindingSet row = result.next();
			Value instanceNode = row.getValue("instance");
			IRI uri = (IRI) instanceNode;
			resultCollection.add(uri);
		}
		return resultCollection;
	}

	@NotNull
	public static String createQueryForGetInstances(List<URI> uris) {
		String query = "SELECT ?instance WHERE { " +
				"{?instance rdf:type <" + uris.get(0) + "> .}";
		for (int i = 1; i < uris.size(); i++) {
			query += "UNION ";
			query += "{?instance rdf:type <" + uris.get(i) + "> .}";
		}
		query += "}";
		return query;
	}
}
