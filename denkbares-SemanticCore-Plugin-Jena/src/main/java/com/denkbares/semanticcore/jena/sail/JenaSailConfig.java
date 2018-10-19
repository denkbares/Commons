package com.denkbares.semanticcore.jena.sail;

import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfigBase;

/**
 * Basic SailImplConfig for jena.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 30.05.16
 */
public class JenaSailConfig extends AbstractSailImplConfig {

	public JenaSailConfig() {
		super(JenaSailFactory.SAIL_TYPE);
	}

}
