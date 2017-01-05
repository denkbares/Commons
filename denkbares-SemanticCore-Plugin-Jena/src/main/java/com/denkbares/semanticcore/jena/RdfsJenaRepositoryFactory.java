package com.denkbares.semanticcore.jena;

import org.apache.jena.rdf.model.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.eclipse.rdf4j.repository.Repository;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 01.06.16
 */
public class RdfsJenaRepositoryFactory extends AbstractJenaRepositoryFactory {

	public static final String TYPE = "jena:RdfsRepository";

	public RdfsJenaRepositoryFactory() {
		super(TYPE);
	}

	@Override
	protected @NotNull Repository getRepository() {
		return new JenaRepository(ModelFactory.createRDFSModel(ModelFactory.createMemModelMaker().createFreshModel()));
	}
}
