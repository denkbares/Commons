package com.denkbares.semanticcore.test;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

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
		TupleQueryResult query = instance.query("SELECT * WHERE { ?x ?y ?z} ");

		// check if we get the 87 triples from the rdf
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}
}
