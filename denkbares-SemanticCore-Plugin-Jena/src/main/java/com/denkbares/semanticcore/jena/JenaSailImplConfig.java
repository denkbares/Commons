package com.denkbares.semanticcore.jena;

import org.openrdf.sail.config.SailImplConfigBase;

/**
 * Basic SailImplConfig for jena.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 30.05.16
 */
public class JenaSailImplConfig extends SailImplConfigBase {

	public JenaSailImplConfig() {
		super(JenaSailFactory.SAIL_TYPE);
	}

}
