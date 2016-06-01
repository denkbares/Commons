package com.denkbares.semanticcore.jena;

import org.openrdf.repository.config.RepositoryImplConfigBase;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 31.05.16
 */
public class JenaRepositoryConfig extends RepositoryImplConfigBase {

	public JenaRepositoryConfig() {
		super(JenaRepositoryFactory.REPOSITORY_TYPE);
	}
}
