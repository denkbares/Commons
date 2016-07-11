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
