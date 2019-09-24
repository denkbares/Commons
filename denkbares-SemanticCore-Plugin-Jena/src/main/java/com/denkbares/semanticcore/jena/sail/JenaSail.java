/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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

package com.denkbares.semanticcore.jena.sail;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.SailBase;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSail extends SailBase {

	private Model model;

	@Override
	protected void initializeInternal() throws SailException {
		model = ModelFactory.createMemModelMaker().createFreshModel();
	}

	@Override
	protected void shutDownInternal() throws SailException {
		model.close();
	}

	@Override
	protected SailConnection getConnectionInternal() throws SailException {
		return new JenaSailConnection(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	@Override
	public ValueFactory getValueFactory() {
		return ValueFactoryImpl.getInstance();
	}

	Model getModel() {
		return model;
	}
}
