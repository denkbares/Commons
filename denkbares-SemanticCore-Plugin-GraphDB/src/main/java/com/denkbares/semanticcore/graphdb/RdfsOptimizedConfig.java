package com.denkbares.semanticcore.graphdb;

/**
 * Repository config with GraphDB's build-in rdfs-optimized profile.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RdfsOptimizedConfig extends GraphDBConfig {

	public RdfsOptimizedConfig() {
		super("rdfs-optimized");
	}
}
