package com.denkbares.semanticcore.graphdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import com.ontotext.trree.config.OWLIMSailSchema;
import org.jetbrains.annotations.NotNull;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.sail.config.SailRepositorySchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denkbares.semanticcore.config.RepositoryConfig;
import com.denkbares.utils.Files;
import com.denkbares.utils.Log;
import com.denkbares.utils.Streams;

import static com.denkbares.semanticcore.SemanticCore.DEFAULT_NAMESPACE;

/**
 * Abstract config class for GraphDB repositories.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public abstract class GraphDBConfig implements RepositoryConfig {

	private final String configFile;
	private final String ruleSet;

	public GraphDBConfig(String ruleSet) {
		this(ruleSet, null);
	}

	public GraphDBConfig(String ruleSet, String configFile) {
		// fix to avoid weird exception log with graphdb-free-runtime 7.0.3
		// ERROR com.ontotext.GraphDBConfigParameters - Exception when trying to find the plugins/ folder
		// com.ontotext.graphdb.ConfigException: graphdb.dist must be set to the GraphDB distribution directory...
		try {
			File tempDir = Files.createTempDir();
			tempDir.delete();
			tempDir.deleteOnExit();
			System.setProperty("graphdb.dist", tempDir.getAbsolutePath());
		}
		catch (IOException e) {
			Log.warning("Exception while creating workaround graphdb dist folder.");
		}

		// Configure logging
		Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (rootLogger instanceof ch.qos.logback.classic.Logger) {
			((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.ERROR);
		}

		// prepare rule set
		if (ruleSet == null) {
			ruleSet = "empty";
		}
		else {
			// check if the rule set is provided as pie file in the resource
			// if yes, copy it to a temp folder, since GraphDB doesn't get resources
			InputStream ruleSetStream = getClass().getResourceAsStream(ruleSet);
			if (ruleSetStream != null) {
				try {
					File ruleSetResourceCopy = File.createTempFile("RuleSetResourceCopy_", "_" + getName() + ".pie");
					ruleSetResourceCopy.deleteOnExit();
					OutputStream ruleSetCopyStream = new FileOutputStream(ruleSetResourceCopy);
					Streams.streamAndClose(ruleSetStream, ruleSetCopyStream);
					ruleSet = ruleSetResourceCopy.getAbsolutePath();
				}
				catch (IOException e) {
					Log.warning("Exception while trying to cache rule set file for usage", e);
				}
			}
		}
		if (configFile == null) configFile = "/des-defaults.ttl";
		this.configFile = configFile;
		this.ruleSet = ruleSet;
	}

	@Override
	public org.openrdf.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		try {
			return createGraphDBConfig(repositoryId, repositoryLabel, overrides);
		}
		catch (RDFParseException | RDFHandlerException | IOException e) {
			throw new RepositoryConfigException(e);
		}
	}

	@NotNull
	private org.openrdf.repository.config.RepositoryConfig createGraphDBConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RDFParseException, RDFHandlerException, IOException, RepositoryConfigException {
		Model graph = parseConfigFile(getConfigFile(), RDFFormat.TURTLE, DEFAULT_NAMESPACE);

		Resource repositoryNode = graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY).subjectResource();

		graph.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID, new LiteralImpl(repositoryId));

		if (repositoryLabel != null) {
			graph.add(repositoryNode, RDFS.LABEL, new LiteralImpl(repositoryLabel));
		}

		if (overrides == null) {
			overrides = new HashMap<>();
		}
		overrides.putIfAbsent("ruleset", getRuleSet());
		Resource configNode = graph.filter(null, SailRepositorySchema.SAILIMPL, null).objectResource();
		for (Map.Entry<String, String> entry : overrides.entrySet()) {
			URI key = new URIImpl(OWLIMSailSchema.NAMESPACE + entry.getKey());
			Literal value = new LiteralImpl(entry.getValue());
			graph.remove(configNode, key, null);
			graph.add(configNode, key, value);
		}

		// Create a configuration object from the configuration graph
		// and add it to the repositoryManager
		return org.openrdf.repository.config.RepositoryConfig.create(graph, repositoryNode);
	}

	public String getRuleSet() {
		return ruleSet;
	}

	public String getConfigFile() {
		return configFile;
	}

	private Model parseConfigFile(String configurationFile, RDFFormat format, String defaultNamespace) throws RDFParseException,
			RDFHandlerException, IOException {

		InputStream configStream = getClass().getResourceAsStream(configurationFile);

		final Model graph = new TreeModel();
		RDFParser parser = Rio.createParser(format);
		parser.setRDFHandler(new StatementCollector(graph));
		parser.parse(configStream, defaultNamespace);
		return graph;
	}

	@Override
	public String getName() {
		return ruleSet.replaceAll("-", "_").toUpperCase();
	}

}
