/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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

import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;

import de.d3web.utils.Log;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @since 23.07.2014
 */
public class RDFTransformator implements Transformator {

	@Override
	public void transform(String targetFile, SemanticCore core) {
		try {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.RDFXML, new FileWriter(targetFile));
			core.getConnection().export(rdfWriter);
		}
		catch (UnsupportedRDFormatException e) {
			Log.severe("Exporting RDF failed.", e);
		}
		catch (IOException e) {
			Log.severe("Exporting RDF failed.", e);
		}
		catch (RepositoryException e) {
			Log.severe("Exporting RDF failed.", e);
		}
		catch (RDFHandlerException e) {
			Log.severe("Exporting RDF failed.", e);
		}
	}

	@Override
	public boolean isFileTransformator() {
		return true;
	}

	@Override
	public String getTitle() {
		return "RDF-XML";
	}

}