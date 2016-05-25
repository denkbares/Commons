package com.denkbares.semanticcore.test;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import com.denkbares.semanticcore.SemanticCore;
import com.denkbares.semanticcore.config.RdfConfig;
import com.denkbares.semanticcore.config.RepositoryConfigs;
import de.d3web.plugin.test.InitPluginManager;

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
	 * This is more of a manual test, since we can't (easily) test the clean up of the repository temp folder after JVM
	 * shutdown.
	 */
	@Test
	public void cleanupSemanticCoreTest() throws IOException, RDFParseException, RepositoryException {
		String tmpFolderPath = "target/cleanupTest";
		SemanticCore instance = SemanticCore.createInstance("Just a test", RepositoryConfigs.get(RdfConfig.class), tmpFolderPath);
		instance.addData(new FileInputStream("src/test/resources/rdf-schema"), RDFFormat.RDFXML);
	}
}
