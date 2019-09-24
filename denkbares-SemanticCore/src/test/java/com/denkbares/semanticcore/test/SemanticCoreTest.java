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

package com.denkbares.semanticcore.test;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import com.denkbares.semanticcore.SemanticCore;
import com.denkbares.semanticcore.TupleQueryResult;
import com.denkbares.semanticcore.config.RdfConfig;
import com.denkbares.semanticcore.config.RepositoryConfigs;
import com.denkbares.plugin.test.InitPluginManager;

import static org.junit.Assert.assertEquals;

/**
 * Default test class for SemanticCore.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class SemanticCoreTest {

	@BeforeClass
	public static void init() throws IOException {
		InitPluginManager.init();
	}

	/**
	 * Just load a file and query...
	 */
	@Test
	public void basic() throws IOException, RDFParseException, RepositoryException, QueryEvaluationException, MalformedQueryException {
		String tmpFolderPath = "target/basic";
		SemanticCore instance = SemanticCore.createInstance("Just a test", RepositoryConfigs.get(RdfConfig.class), tmpFolderPath);
		instance.addData(new FileInputStream("src/test/resources/rdf-schema.xml"), RDFFormat.RDFXML);
		TupleQueryResult query = instance.sparqlSelect("SELECT * WHERE { ?x ?y ?z} ");

		// check if we get the 87 triples from the rdf
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}
}
