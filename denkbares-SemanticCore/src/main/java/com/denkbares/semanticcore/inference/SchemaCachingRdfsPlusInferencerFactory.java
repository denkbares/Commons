/*
 * Copyright (C) 2025 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.semanticcore.inference;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 26.02.2025
 */
public class SchemaCachingRdfsPlusInferencerFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "des:SchemaCachingRdfsPlusInferencer";

	/**
	 * Returns the Sail's type: <var>des:SchemaCachingRdfsPlusInferencer</var>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new SchemaCachingRDFSInferencerConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		return new SchemaCachingRdfsPlusInferencer();
	}
}
