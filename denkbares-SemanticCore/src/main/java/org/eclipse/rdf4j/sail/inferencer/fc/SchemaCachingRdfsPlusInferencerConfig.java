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

package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerFactory;

/**
 * Config for SchemaCachingRdfsPlusInferencerConfig... same as RDFS inferencing, but extended with inversOf (and maybe
 * more later).
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 26.02.2025
 */
public class SchemaCachingRdfsPlusInferencerConfig extends AbstractDelegatingSailImplConfig {

	public SchemaCachingRdfsPlusInferencerConfig() {
		super(SchemaCachingRdfsPlusInferencerFactory.SAIL_TYPE);
	}

	public SchemaCachingRdfsPlusInferencerConfig(SailImplConfig delegate) {
		super(SchemaCachingRdfsPlusInferencerFactory.SAIL_TYPE, delegate);
	}
}
