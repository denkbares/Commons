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
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.denkbares.collections.PartialHierarchyTree;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.utils.Sparqls;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 30.10.15.
 */
public class HierarchyUtils {

	/**
	 * Generates the tree of all classes that the concept belongs to. The tree contains all class
	 * the the concept is 'rdf:type' of. Further, if for two class in the tree 'A rdfs:subClassOf B'
	 * holds, then A is a successor of B in the generated tree.
	 *
	 * @param core    the repository to work with
	 * @param concept the concept for which the class tree should be generated
	 * @return the tree of all classes that the concept belongs to
	 */
	public static PartialHierarchyTree<URI> getClassHierarchy(SPARQLEndpoint core, URI concept) {
		return getClassHierarchy(core, concept, "rdfs:subClassOf", "rdf:type");
	}

	/**
	 * @param core             the repository to work with
	 * @param concept          the concept for which the class tree should be generated
	 * @param subClassRelation the relation that defines the subclass hierarchy (usually
	 *                         rdfs:subClassOf)
	 * @param typeRelation     the property defining an instanceof relation (usually rdf:type)
	 * @return the tree of all classes that the concept belongs to
	 */
	public static PartialHierarchyTree<URI> getClassHierarchy(SPARQLEndpoint core, URI concept, String subClassRelation, String typeRelation) {
		final SubClassHierarchy subClassHierarchy = new SubClassHierarchy(core, subClassRelation);
		PartialHierarchyTree<URI> tree = new PartialHierarchyTree<>(subClassHierarchy);

        /*
		build up tree of classes
         */
		String classQuery = "SELECT ?c WHERE { <" + concept + "> " + typeRelation + " ?c }";

		try (TupleQueryResult tupleQueryResult = core.sparqlSelect(classQuery)) {
			while (tupleQueryResult.hasNext()) {
				BindingSet row = tupleQueryResult.next();

				Value value = row.getValue("c");

				if (value.toString().startsWith("_:")) {
					// is BlankNode
					continue;
				}
				URI uri = Sparqls.asURI(row, "c");
				tree.insertNode(uri);
			}
			return tree;
		}
	}

	/**
	 * Returns the most specific class of the concept where '<concept> rdf:type <class>' holds. For
	 * most specific one is considered to be the leaf class which has the longest path (highest
	 * depth) in the tree of all classes of the concept. If there are multiple deepest classes with
	 * same depth, the result is one of those (randomly).
	 */
	public static URI findMostSpecificClass(SPARQLEndpoint core, URI concept) {
		return findMostSpecificClass(getClassHierarchy(core, concept));
	}

	/**
	 * Returns the most specific class the given hierarchy of classes. For most specific one is
	 * considered to be the leaf class which has the longest path (highest depth) in given
	 * hierarchy. If there are multiple deepest classes with same depth, the result is one of those
	 * (randomly).
	 */
	public static URI findMostSpecificClass(PartialHierarchyTree<URI> classHierarchy) {
		final Set<PartialHierarchyTree.Node<URI>> nodes = classHierarchy.getNodes();
		int maxDepth = 0;
		PartialHierarchyTree.Node<URI> deepestLeaf = classHierarchy.getRoot();
		for (PartialHierarchyTree.Node<URI> node : nodes) {
			int depth = classHierarchy.getMaxDepthLevel(node);
			if (depth >= maxDepth) {
				maxDepth = depth;
				deepestLeaf = node;
			}
		}
		return deepestLeaf.getData();
	}
}
