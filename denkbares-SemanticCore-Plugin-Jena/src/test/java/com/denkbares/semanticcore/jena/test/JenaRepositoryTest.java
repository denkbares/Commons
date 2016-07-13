package com.denkbares.semanticcore.jena.test;

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
				RepositoryConfigs.get(com.denkbares.semanticcore.jena.sail.RdfConfig.class), "target/testSail");
		instance.addData(new FileInputStream("src/test/resources/rdf-schema.xml"), RDFFormat.RDFXML);

		TupleQueryResult query = instance.query("SELECT * WHERE { ?x ?y ?z}");
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}

	@Test
	public void testBasic() throws IOException, RDFParseException, RepositoryException, QueryEvaluationException, MalformedQueryException {
		SemanticCore instance = SemanticCore.createInstance("BasicTest", RepositoryConfigs.get(RdfConfig.class), "target/testBasic");
		instance.addData(new FileInputStream("src/test/resources/rdf-schema.xml"), RDFFormat.RDFXML);

		TupleQueryResult query = instance.query("SELECT * WHERE { ?x ?y ?z}");
		assertEquals(87, query.cachedAndClosed().getBindingSets().size());
	}
}
