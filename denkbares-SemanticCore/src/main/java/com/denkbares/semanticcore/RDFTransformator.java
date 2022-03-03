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
package com.denkbares.semanticcore;

import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @since 23.07.2014
 */
public class RDFTransformator implements Transformator {
	private static final Logger LOGGER = LoggerFactory.getLogger(RDFTransformator.class);

	@Override
	public void transform(String targetFile, SemanticCore core) {
		try {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, new FileWriter(targetFile));
			core.getConnection().export(rdfWriter);
		}
		catch (UnsupportedRDFormatException | RDFHandlerException | RepositoryException | IOException e) {
			LOGGER.error("Exporting RDF failed.", e);
		}
	}

	@Override
	public boolean isFileTransformator() {
		return true;
	}

	@Override
	public String getTitle() {
		return "Turtle (RDF)";
	}

	@Override
	public String getFileExtension() {
		return ".ttl.dan";
	}
}
