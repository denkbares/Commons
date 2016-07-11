package com.denkbares.semanticcore.graphdb;

/**
 * Repository config with GraphDB's build-in owl-horst-optimized profile, extended with property-chains.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class OwlHorstOptimizedPropertyChainsConfig extends GraphDBConfig {

	public OwlHorstOptimizedPropertyChainsConfig() {
		super("/builtin_Rules-horst-optimized-with-property-chains.pie");
	}

	@Override
	public String getName() {
		return "OWL_HORST_OPTIMIZED_WITH_PROPERTY_CHAINS";
	}
}
