/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import com.denkbares.semanticcore.config.RepositoryConfig;
import com.denkbares.utils.Streams;

/**
 * Implementation of a SesameEndpoint for a single turtle file that gets the connection from the
 * SemanticCore and handles its allocation/release well.
 */
public class TurtleFileEndpoint extends SesameEndpoint {
	private final String ontologyName;
	private SemanticCore sc;

	/**
	 * Creates a new endpoint by loading the turtle file into a newly created semantic core.
	 *
	 * @param sourceFile the turtle file to be loaded
	 * @param reasoning  the reasoning config to be used by this endpoint
	 * @param tempFolder the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	public TurtleFileEndpoint(URL sourceFile, RepositoryConfig reasoning, File tempFolder) throws IOException {
		this(sourceFile, reasoning, createOntologyName(sourceFile), tempFolder);
	}

	/**
	 * Creates a new endpoint by loading the turtle file into a newly created semantic core. The
	 * ontologyName should be unique otherwise a semantic core with that id will be reused, if there
	 * is any.
	 *
	 * @param sourceFile   the turtle file to be loaded
	 * @param reasoning    the reasoning config to be used by this endpoint
	 * @param ontologyName the name/id of the ontology
	 * @param tempFolder   the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	public TurtleFileEndpoint(URL sourceFile, RepositoryConfig reasoning, String ontologyName, File tempFolder) throws IOException {
		this(Collections.singleton(new InputStreamReader(sourceFile.openStream(), "UTF-8")),
				reasoning, true, ontologyName, tempFolder);
	}

	/**
	 * Creates a new endpoint by loading the turtle stream into a newly created semantic core. The
	 * ontologyName should be unique otherwise a semantic core with that id will be reused, if there
	 * is any.
	 *
	 * @param source       turtle file to be loaded
	 * @param reasoning    the reasoning config to be used by this endpoint
	 * @param ontologyName the name/id of the ontology
	 * @param tempFolder   the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	public TurtleFileEndpoint(Path source, RepositoryConfig reasoning, String ontologyName, File tempFolder) throws IOException {
		this(Collections.singleton(new BufferedReader(new InputStreamReader(new FileInputStream(source.toFile()), "UTF-8"))),
				reasoning, true, ontologyName, tempFolder);
	}

	/**
	 * Creates a new endpoint by loading the turtle file into a newly created semantic core. The
	 * ontologyName should be unique otherwise a semantic core with that id will be reused, if there
	 * is any.
	 *
	 * @param source       streamed turtle content to be loaded
	 * @param reasoning    the reasoning config to be used by this endpoint
	 * @param ontologyName the name/id of the ontology
	 * @param tempFolder   the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	public TurtleFileEndpoint(Reader source, RepositoryConfig reasoning, String ontologyName, File tempFolder) throws IOException {
		this(Collections.singleton(source), reasoning, false, ontologyName, tempFolder);
	}

	/**
	 * Creates a new endpoint by loading the turtle stream into a newly created semantic core. The
	 * ontologyName should be unique otherwise a semantic core with that id will be reused, if there
	 * is any.
	 *
	 * @param sources      streamed turtle contents to be loaded sequentially
	 * @param reasoning    the reasoning config to be used by this endpoint
	 * @param autoClose    if the specified stream shall be closed by this method after reading
	 * @param ontologyName the name/id of the ontology
	 * @param tempFolder   the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	private TurtleFileEndpoint(Collection<Reader> sources, RepositoryConfig reasoning, boolean autoClose, String ontologyName, File tempFolder) throws IOException {
		this.ontologyName = ontologyName;
		this.sc = SemanticCore.getOrCreateInstance(ontologyName, reasoning, tempFolder);
		try {
			for (Reader source : sources) {
				sc.addData(source, RDFFormat.TURTLE);
			}
			setConnection(sc.getConnection());
		}
		catch (RepositoryException | RDFParseException e) {
			throw new IOException("cannot initialize ontology from resource stream", e);
		}
		finally {
			if (autoClose) {
				for (Reader source : sources) {
					Streams.closeQuietly(source);
				}
			}
		}
		// allocate core if everything if this instance is created and returned
		sc.allocate();
	}

	/**
	 * Creates a new endpoint by loading the turtle files into a newly created semantic core. The
	 * ontologyName should be unique otherwise a semantic core with that id will be reused, if there
	 * is any.
	 *
	 * @param sources      turtle files to be loaded
	 * @param reasoning    the reasoning config to be used by this endpoint
	 * @param ontologyName the name/id of the ontology
	 * @param tempFolder   the folder to eventually create the repository in
	 * @throws IOException if the turtle could not be loaded or the repository could not be created
	 */
	public static TurtleFileEndpoint fromPaths(Collection<Path> sources, RepositoryConfig reasoning, String ontologyName, File tempFolder) throws IOException {
		Collection<Reader> streams = new ArrayList<>(sources.size());
		for (Path source : sources) {
			streams.add(new BufferedReader(new InputStreamReader(new FileInputStream(source.toFile()), "UTF-8")));
		}
		return new TurtleFileEndpoint(streams, reasoning, true, ontologyName, tempFolder);
	}

	private static String createOntologyName(URL sourceFile) {
		return sourceFile.getPath().replaceFirst("^.*/[^/]+]", "")
				.replaceAll("[^\\w\\d_\\-.()=+#]+", "-") + "-#" +
				String.format("%08x", (0xFFFFFFFFL & sourceFile.hashCode())).toUpperCase();
	}

	public String getOntologyName() {
		return ontologyName;
	}

	@Override
	public String toString() {
		return "TurtleFileEndpoint:" + ontologyName;
	}

	@Override
	public synchronized void close() throws RepositoryException {
		try {
			super.close();
		}
		finally {
			// release core when this instance is closed, but only once!
			if (sc != null) {
				sc.release();
				sc = null;
			}
		}
	}
}
