package com.denkbares.semanticcore.graphdb;

/**
 * Repository config with GraphDB's build-in rdfs profile.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RdfsConfig extends GraphDBConfig {

	public RdfsConfig() {
		super("rdfs");
	}

	@Override
	public String getName() {
		return "RDFS_SESAME";
	}
}
