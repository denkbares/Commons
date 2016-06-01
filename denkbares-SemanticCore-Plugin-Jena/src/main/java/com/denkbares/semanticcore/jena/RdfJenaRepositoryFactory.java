package com.denkbares.semanticcore.jena;

import org.apache.jena.rdf.model.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.openrdf.repository.Repository;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 01.06.16
 */
public class RdfJenaRepositoryFactory extends AbstractJenaRepositoryFactory {

	public static final String TYPE = "jena:RdfRepository";

	public RdfJenaRepositoryFactory() {
		super(TYPE);
	}

	@Override
	protected @NotNull Repository getRepository() {
		return new JenaRepository(ModelFactory.createMemModelMaker().createFreshModel());
	}
}
