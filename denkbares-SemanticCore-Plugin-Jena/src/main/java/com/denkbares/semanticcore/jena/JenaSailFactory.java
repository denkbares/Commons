package com.denkbares.semanticcore.jena;

import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSailFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "jena:DefaultStore";

	/**
	 * Returns the Sail's type: <tt>jena:DefaultStore</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new JenaSailImplConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		return new JenaSail();
	}
}

