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

package com.denkbares.semanticcore.jena.test;

import java.io.File;
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
import com.denkbares.semanticcore.config.RepositoryConfigs;
import com.denkbares.semanticcore.jena.RdfConfig;
import com.denkbares.plugin.test.InitPluginManager;

import static org.junit.Assert.assertEquals;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 30.05.16
 */
public class JenaRepositoryTest {

	@BeforeClass
	public static void init() throws IOException {
		InitPluginManager.init();
	}

	//	@Test implementation not complete
	public void testSail() throws IOException, RDFParseException, RepositoryException, QueryEvaluationException, MalformedQueryException {
		SemanticCore instance = SemanticCore.createInstance("SailTest",
				RepositoryConfigs.get(com.denkbares.semanticcore.jena.sail.RdfConfig.class), new File("target/JenaSailTest"));
		instance.addData(new FileInputStream("src/test/resources/rdf-schema.xml"), RDFFormat.RDFXML);

		TupleQueryResult query = instance.sparqlSelect("SELECT * WHERE { ?x ?y ?z}");
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}

	@Test
	public void testBasic() throws IOException, RDFParseException, RepositoryException, QueryEvaluationException, MalformedQueryException {
		SemanticCore instance = SemanticCore.createInstance("BasicTest", RepositoryConfigs.get(RdfConfig.class), new File("target/JenaBasicTest"));
		instance.addData(new FileInputStream("src/test/resources/rdf-schema.xml"), RDFFormat.RDFXML);

		TupleQueryResult query = instance.sparqlSelect("SELECT * WHERE { ?x ?y ?z}");
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}
}
