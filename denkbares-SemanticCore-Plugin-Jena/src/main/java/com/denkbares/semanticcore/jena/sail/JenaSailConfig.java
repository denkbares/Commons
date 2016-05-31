package com.denkbares.semanticcore.jena.sail;

import org.openrdf.sail.config.SailImplConfigBase;

/**
 * Basic SailImplConfig for jena.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 30.05.16
 */
public class JenaSailConfig extends SailImplConfigBase {

	public JenaSailConfig() {
		super(JenaSailFactory.SAIL_TYPE);
	}

}
