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
import com.ontotext.trree.statistics.StatisticsSettings;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denkbares.events.EventManager;
import com.denkbares.semanticcore.RepositoryConfigCreatedEvent;
import com.denkbares.semanticcore.config.RepositoryConfig;
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
	private final Map<String, String> defaultOverrides = new HashMap<>();

	public GraphDBConfig(String ruleSet) {
		this(ruleSet, null);
		EventManager.getInstance().fireEvent(new RepositoryConfigCreatedEvent(this));
	}

	public void setDefaultOverride(String key, String value) {
		defaultOverrides.put(key, value);
	}

	public GraphDBConfig(String ruleSet, String configFile) {
		// we deactivate statistics, because it can cause memory leaks on web app redeploy.
		StatisticsSettings.getInstance().setStatisticsEnabled(false);

		// fix to avoid weird exception log with graphdb-free-runtime 7.0.3
		// ERROR com.ontotext.GraphDBConfigParameters - Exception when trying to find the plugins/ folder
		// com.ontotext.graphdb.ConfigException: graphdb.dist must be set to the GraphDB distribution directory...
//		if (System.getProperty("graphdb.dist") == null) {
//			try {
//				// try static, unchanging directory first
//				File tempDir = new File(Files.getSystemTempDir(), "graphdb-dist-mock-dir");
//				tempDir.mkdirs();
//				if (!tempDir.isDirectory() || !tempDir.canRead()) {
//					tempDir = Files.createTempDir();
//					tempDir.deleteOnExit();
//				}
//				System.setProperty("graphdb.dist", tempDir.getAbsolutePath());
//			}
//			catch (IOException e) {
//				Log.warning("Exception while creating workaround graphdb dist folder.");
//			}
//		}

		// Configure logging
		Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (rootLogger instanceof ch.qos.logback.classic.Logger) {
			((ch.qos.logback.classic.Logger) rootLogger).setLevel(Level.ERROR);
		}
		else {
			throw new IllegalStateException("GraphDB requires " + ch.qos.logback.classic.Logger.class.getName()
					+ ", but was " + rootLogger.getClass().getName() + ".\nTo fix this, remove (e.g. by excluding) "
					+ "the dependency to the second logger from the class path, also see "
					+ "https://www.slf4j.org/codes.html#multiple_bindings.");
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
	public org.eclipse.rdf4j.repository.config.RepositoryConfig createRepositoryConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RepositoryConfigException {
		try {
			return createGraphDBConfig(repositoryId, repositoryLabel, overrides);
		}
		catch (RDFParseException | RDFHandlerException | IOException e) {
			throw new RepositoryConfigException(e);
		}
	}

	@NotNull
	private org.eclipse.rdf4j.repository.config.RepositoryConfig createGraphDBConfig(String repositoryId, String repositoryLabel, Map<String, String> overrides) throws RDFParseException, RDFHandlerException, IOException, RepositoryConfigException {
		Model graph = parseConfigFile(getConfigFile(), RDFFormat.TURTLE, DEFAULT_NAMESPACE);

		Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
				.orElseThrow(() -> new RepositoryConfigException("Unable to retrieve repository node"));

		SimpleValueFactory factory = SimpleValueFactory.getInstance();
		graph.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID, factory.createLiteral(repositoryId));

		if (repositoryLabel != null) {
			graph.add(repositoryNode, RDFS.LABEL, factory.createLiteral(repositoryLabel));
		}

		if (overrides == null) {
			overrides = new HashMap<>();
		}
		overrides.putAll(defaultOverrides);
		overrides.putIfAbsent(OWLIMSailSchema.NAMESPACE + "ruleset", getRuleSet());
		String overriddenType = overrides.remove(RepositoryConfigSchema.REPOSITORYTYPE.toString());
		if (overriddenType != null) {
			Resource implNode = Models.object(graph.filter(repositoryNode, RepositoryConfigSchema.REPOSITORYIMPL, null))
					.map(v -> (Resource) v)
					.orElseThrow(() -> new RepositoryConfigException("Unable to retrieve repository impl node"));
			graph.remove(implNode, RepositoryConfigSchema.REPOSITORYTYPE, null);
			graph.add(implNode, RepositoryConfigSchema.REPOSITORYTYPE, factory.createLiteral(overriddenType));
		}

		Resource configNode = Models.objectResource(graph.filter(null, SailRepositorySchema.SAILIMPL, null))
				.orElseThrow(() -> new RepositoryConfigException("Unable to retrieve config node"));
		for (Map.Entry<String, String> entry : overrides.entrySet()) {
			IRI key = factory.createIRI(entry.getKey());
			Literal value = factory.createLiteral(entry.getValue());
			graph.remove(configNode, key, null);
			graph.add(configNode, key, value);
		}

		// Create a configuration object from the configuration graph
		// and add it to the repositoryManager
		return org.eclipse.rdf4j.repository.config.RepositoryConfig.create(graph, repositoryNode);
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
