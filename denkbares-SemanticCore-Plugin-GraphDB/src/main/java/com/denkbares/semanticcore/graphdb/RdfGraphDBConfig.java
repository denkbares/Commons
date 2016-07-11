package com.denkbares.semanticcore.graphdb;

/**
 * Repository config with GraphDB's build-in rdf or empty profile.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RdfGraphDBConfig extends GraphDBConfig {

	public RdfGraphDBConfig() {
		super("empty");
	}

	@Override
	public String getName() {
		return "RDF_GRAPHDB";
	}
}
