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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denkbares.collections.Matrix;
import com.denkbares.events.EventManager;
import com.denkbares.semanticcore.config.RepositoryConfig;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.semanticcore.utils.RDFUtils;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Files;
import com.denkbares.utils.Stopwatch;
import com.denkbares.utils.Streams;

public final class SemanticCore implements SPARQLEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(SemanticCore.class);
	public static final String SEMANTIC_CORE_REPOSITORY_PATH_SUFFIX = "semanticCore.repository.path.suffix";

	public enum State {
		active, shutdown
	}

	private volatile State state;

	// we use a map of atomic reference, to immediately add the reference to block the key,
	// even before the initialization of the semantic core is completed
	private static final Map<String, AtomicReference<SemanticCore>> instances = new ConcurrentHashMap<>();

	private static final Object lazyInitializationMutex = new Object();
	private volatile static LocalRepositoryManager repositoryManager = null;
	private static final int THRESHOLD_TIME = 1000 * 60 * 2; // 2 min...
	public static final String DEFAULT_NAMESPACE = "http://www.denkbares.com/ssc/ds#";
	private final String repositoryId;
	private final Repository repository;
	private final AtomicLong allocationCounter = new AtomicLong(0);

	public static SemanticCore getInstance(String key) {
		AtomicReference<SemanticCore> reference = instances.get(key);
		if (reference == null) {
			return null;
		}
		synchronized (reference) {
			return reference.get();
		}
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning) throws IOException {
		return getOrCreateInstance(key, reasoning, (String) null);
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, File tmpPath) throws IOException {
		AtomicReference<SemanticCore> reference = instances.computeIfAbsent(key, k -> new AtomicReference<>());
		synchronized (reference) {
			createInstance(reference, key, reasoning, tmpPath);
		}
		return reference.get();
	}

	/**
	 * @deprecated use getOrCreateInstance(String key, RepositoryConfig reasoning, File tmpFolder) instead
	 */
	@Deprecated
	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, String tmpPath) throws IOException {
		return getOrCreateInstance(key, reasoning, tmpPath == null ? null : new File(tmpPath));
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning) throws IOException {
		return createInstance(key, reasoning, (File) null);
	}

	/**
	 * @deprecated use createInstance(String key, RepositoryConfig reasoning, File tmpFolder) instead
	 */
	@Deprecated
	public static SemanticCore createInstance(String key, RepositoryConfig reasoning, String tmpFolder) throws IOException {
		return createInstance(key, reasoning, tmpFolder == null ? null : new File(tmpFolder));
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning, File tmpFolder) throws IOException {
		Objects.requireNonNull(reasoning);
		AtomicReference<SemanticCore> reference = new AtomicReference<>();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (reference) {
			instances.put(key, reference);
			createInstance(reference, key, reasoning, tmpFolder);
		}
		SemanticCore instance = reference.get();
		LOGGER.info("Created SemanticCore '" + instance.repositoryId + "' with config " + reasoning.getName());
		return instance;
	}

	private static void createInstance(AtomicReference<SemanticCore> reference, String key, RepositoryConfig reasoning, File tmpFolder) throws IOException {
		if (reference.get() != null) return;
		try {
			SemanticCore instance = new SemanticCore(key, null, reasoning, tmpFolder, null);
			reference.set(instance);
		}
		finally {
			if (reference.get() == null) {
				instances.remove(key);
			}
		}
	}

	public static File createRepositoryManagerDir(String suffix) throws IOException {
		suffix = System.getProperty(SEMANTIC_CORE_REPOSITORY_PATH_SUFFIX, suffix);
		@NotNull File systemTempDir = Files.getSystemTempDir();
		String baseName = SemanticCore.class.getName().replaceAll("\\W", "-") + "-" + suffix;
		File tempDirCandidate = new File(systemTempDir, baseName);
		tempDirCandidate.mkdir();
		tempDirCandidate.deleteOnExit();
		return tempDirCandidate;
	}

	private SemanticCore(@NotNull String repositoryId, @Nullable String repositoryLabel, @NotNull RepositoryConfig repositoryConfig, @Nullable File tmpFolder, @Nullable Map<String, String> overrides) throws IOException {
		initializeRepositoryManagerLazy(tmpFolder, repositoryId);
		this.repositoryId = repositoryId;
		try {
			if (repositoryManager.hasRepositoryConfig(repositoryId)) {
				throw new RuntimeException("Repository " + repositoryId + " already exists.");
			}

			org.eclipse.rdf4j.repository.config.RepositoryConfig openRdfRepositoryConfig = repositoryConfig.createRepositoryConfig(repositoryId, repositoryLabel, overrides);
			repositoryManager.addRepositoryConfig(openRdfRepositoryConfig);

			// Get the repository and connect to it!
			this.repository = repositoryManager.getRepository(repositoryId);
			EventManager.getInstance().fireEvent(new RepositoryCreatedEvent(repositoryConfig, repository));

			try (RepositoryConnection connection = getConnection()) {
				connection.setNamespace("des", DEFAULT_NAMESPACE);
			}
		}
		catch (RDF4JException e) {
			throw new IOException("Cannot initialize repository", e);
		}
		this.state = State.active;
	}

	private void initializeRepositoryManagerLazy(@Nullable File tmpFolder, String repositoryId) throws IOException {
		if (repositoryManager == null) {
			synchronized (lazyInitializationMutex) {
				if (repositoryManager == null) {
					if (tmpFolder == null) {
						tmpFolder = createRepositoryManagerDir(repositoryId);
					}
					initializeRepositoryManager(tmpFolder);
				}
			}
		}
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	/**
	 * Allocates the semantic core for longer use.
	 *
	 * @see #release() for more details
	 */
	public void allocate() {
		allocationCounter.incrementAndGet();
	}

	/**
	 * Releases a previously allocated core. If the core has been released as many times as it has been allocated, the
	 * underlying repository is shut down and the core is removed from the internal SemanticCore caches. You must make
	 * sure that for every time allocate is called there is exactly one call to release as well.
	 */
	public void release() {
		long counter = allocationCounter.decrementAndGet();
		if (counter == 0) {
			close();
		}
	}

	/**
	 * Shuts down this semantic core, if it is not allocated. In this case destroys this instance so that is should not
	 * be used any longer. It is also removed from the internal SemanticCore caches. If this instance is allocated at
	 * least once, the method does nothing.
	 *
	 * @see #allocate()
	 * @see #release()
	 * @see #close()
	 */
	public void requestShutdown() {
		if (!isAllocated()) close();
	}

	/**
	 * Checks if this core is currently allocated, meaning {@link #allocate()} was called more often than {@link
	 * #release()}.
	 *
	 * @return if this core is allocated or not
	 */
	public boolean isAllocated() {
		return allocationCounter.get() > 0;
	}

	/**
	 * Shuts down this semantic core, independently of any allocation / release state. It destroys this instance so
	 * that it can not be used any longer. It is also removed from the internal SemanticCore caches.
	 *
	 * @see #allocate()
	 * @see #release()
	 * @see #requestShutdown()
	 */
	@Override
	public void close() {
		state = State.shutdown;
		try {
			Stopwatch stopwatch = new Stopwatch();
			LOGGER.info("Beginning to shut down SemanticCore " + repositoryId);
			repository.shutDown();
			LOGGER.info("SemanticCore " + repositoryId + " shut down successfully in " + stopwatch.getDisplay());
		}
		catch (Exception e) {
			LOGGER.error("Exception while shutting down repository " + repositoryId + ", removing repository anyway", e);
		}
		finally {
			instances.remove(repositoryId);
			try {
				repositoryManager.removeRepository(repositoryId);
			}
			catch (RepositoryException | RepositoryConfigException e) {
				LOGGER.info("Unable to remove repository from manager", e);
			}
		}
	}

	public static void shutDownAllRepositories() {
		// create new list to avoid concurrent modification exception
		new ArrayList<>(instances.keySet()).stream().map(SemanticCore::getInstance)
				.filter(Objects::nonNull).forEach(SemanticCore::close);
	}

	public static void initializeRepositoryManager(@NotNull File repositoryManagerDir) throws IOException {
		if (repositoryManager != null) {
			throw new IllegalStateException("Repository manager already exists at location: " + repositoryManager.getBaseDir()
					.getAbsolutePath());
		}
		File repositoriesSubFolder = new File(repositoryManagerDir, "repositories");
		// clean repository folder...
		if (repositoriesSubFolder.exists() && repositoriesSubFolder.isDirectory()) {
			FileUtils.deleteDirectory(repositoriesSubFolder);
		}
		repositoryManagerDir.deleteOnExit();
		repositoryManager = new SyncedLocalRepositoryManager(repositoryManagerDir);
		LOGGER.info("Created new repository manager at: " + repositoryManagerDir.getCanonicalPath());
		try {
			repositoryManager.init();
		}
		catch (RepositoryException e) {
			throw new IOException("Cannot initialize repository", e);
		}
	}

	public static void shutDownRepositoryManager() {
		if (repositoryManager == null) return; // noting to shut down
		LOGGER.info("Shutting down repository manager.");
		// shut down any remaining repositories
		try {
			repositoryManager.getRepositoryIDs().forEach(id -> {
				try {
					repositoryManager.getRepository(id).shutDown();
					LOGGER.info("Repository " + id + " shut down successful.");
				}
				catch (RepositoryException | RepositoryConfigException e) {
					LOGGER.error("Unable to shut down repository " + id, e);
				}
			});
		}
		catch (RepositoryException e) {
			LOGGER.error("Unable to retrieve repositories during manager close", e);
		}
		repositoryManager.shutDown();
		repositoryManager = null;
	}

	@Override
	public Collection<Namespace> getNamespaces() throws RepositoryException {
		try (RepositoryConnection connection = getConnection()) {
			return Iterations.asList(connection.getNamespaces());
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		// check state also before synchronizing to avoid having to wait for connection shutdown
		// just to learn that the core is already shut down
		if (state == State.shutdown) throwShutdownException();
		return new RepositoryConnection(repository.getConnection());
	}

	private void throwShutdownException() throws RepositoryException {
		throw new RepositoryException("Repository was shut down, no new connections are accepted.");
	}

	public void addData(InputStream is, String fileExtention) throws RDFParseException, RepositoryException, IOException {
		File tempFile = File.createTempFile("SemanticCore", "." + fileExtention);
		tempFile.deleteOnExit();
		Streams.streamAndClose(is, new FileOutputStream(tempFile));
		addData(tempFile);
	}

	public void addData(InputStream is, RDFFormat format) throws RepositoryException, RDFParseException, IOException {
		try (is) {
			addData(connection -> connection.add(is, DEFAULT_NAMESPACE, format));
		}
	}

	public void addData(Reader reader, RDFFormat format) throws RepositoryException, RDFParseException, IOException {
		addData(connection -> connection.add(reader, DEFAULT_NAMESPACE, format));
	}

	private void addData(DataAdder adder) throws RepositoryException, RDFParseException, IOException {
		try (org.eclipse.rdf4j.repository.RepositoryConnection connection = this.getConnection()) {
			adder.run(connection);
		}
	}

	public void addData(File file) throws RDFParseException, RepositoryException, IOException {
		if (!file.exists()) {
			String message = "ontology file not found: " + file.getAbsolutePath();
			LOGGER.error(message);
		}

		String extension = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();

		// Load all ontology files of a ZIP-File
		if ("zip".equals(extension)) {
			ZipFile zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.startsWith("__MACOSX/")) return;
				InputStream input = zipFile.getInputStream(entry);
				RDFFormat format = getRdfFormat(name);
				if (format != null) {
					addDataFromInputStream(input, format);
				}
			}
			zipFile.close();

			// load single file
		}
		else {
			RDFFormat format = getRdfFormat(file.getAbsolutePath());
			if (format != null) {
				addDataFromFile(file, format);
			}
		}
	}

	private void addDataFromFile(File file, RDFFormat format) throws IOException, RDFParseException,
			RepositoryException {
		try (RepositoryConnection connection = this.getConnection()) {
			connection.add(file, DEFAULT_NAMESPACE, format);
			connection.commit();
		}
	}

	private void addDataFromInputStream(InputStream is, RDFFormat format)
			throws IOException, RDFParseException, RepositoryException {
		try (RepositoryConnection connection = this.getConnection()) {
			connection.add(is, DEFAULT_NAMESPACE, format);
			connection.commit();
		}
	}

	private RDFFormat getRdfFormat(String fileName) {
		RDFFormat format;
		String lsFileName = fileName.toLowerCase();
		if (lsFileName.endsWith(".xml.dan") || lsFileName.endsWith(".xml")) {
			format = RDFFormat.RDFXML;
		}
		else if (lsFileName.endsWith(".ttl.dan")) {
			format = RDFFormat.TURTLE;
		}
		else if (lsFileName.endsWith(".dan")) {
			format = RDFFormat.RDFXML;
		}
		else if (lsFileName.endsWith(".ttl")) {
			format = RDFFormat.TURTLE;
		}
		else {
			format = Rio.getParserFormatForFileName(fileName).orElse(RDFFormat.RDFXML);
		}
		return format;
	}

	public void addData(String file) throws RDFParseException, RepositoryException, IOException {
		addData(new File(file));
	}

	@Override
	public TupleQueryResult sparqlSelect(Collection<Namespace> namespaces, String queryString) throws QueryFailedException {
		// we overwrite default implementation to avoid multiple connection creation
		RepositoryConnection connection = getConnection();
		try {
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, toPrefixes(namespaces) + queryString);
			return new TupleQueryResult(query.evaluate()).onClose(connection::close);
		}
		catch (Exception e) {
			// if an exception occurs preparing the result instance, but after the connection has been created,
			// we have to close the connection manually, otherwise nobody would close
			connection.close();
			throw e;
		}
	}

	@Override
	public TupleQuery prepareSelect(Collection<Namespace> namespaces, String queryString) throws RepositoryException, MalformedQueryException {
		RepositoryConnection connection = getConnection();
		return connection.prepareTupleQuery(QueryLanguage.SPARQL, toPrefixes(namespaces) + queryString);
	}

	/**
	 * Performs the give SELECT queries. No additional namespace will be prepended.
	 *
	 * @return result of the ASK query
	 * @deprecated use {@link #sparqlSelect(Collection, String)} instead
	 */
	@Deprecated
	public TupleQueryResult query(String queryString) throws QueryFailedException {
		return sparqlSelect(Collections.emptyList(), queryString);
	}

	@Override
	public BooleanQuery prepareAsk(Collection<Namespace> namespaces, String queryString) {
		RepositoryConnection connection = getConnection();
		String query = toPrefixes(namespaces) + queryString;
		return new BooleanQuery(connection, connection.prepareBooleanQuery(QueryLanguage.SPARQL, query), query);
	}

	@NotNull
	private String toPrefixes(Collection<Namespace> namespaces) {
		StringBuilder prefixes = new StringBuilder();
		for (Namespace namespace : namespaces) {
			prefixes.append("PREFIX ")
					.append(namespace.getPrefix())
					.append(": <")
					.append(namespace.getName())
					.append(">\n");
		}
		return prefixes.toString();
	}

	/**
	 * Performs the give ASK queries. No additional namespace will be prepended.
	 *
	 * @return result of the ASK query
	 * @deprecated use {@link #sparqlAsk(Collection, String)} instead
	 */
	@Deprecated
	public boolean ask(String queryString) {
		return sparqlAsk(Collections.emptyList(), queryString);
	}

	/**
	 * Performs an UPDATE query.
	 * <p>
	 * Please be aware that this actually updates the content of the SemanticCore, i.e. adds or deletes statements!
	 *
	 * @param queryString the query to update the contents
	 * @created 09.01.2015
	 */
	public void update(String queryString) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		try (RepositoryConnection connection = getConnection()) {
			Update query = connection.prepareUpdate(QueryLanguage.SPARQL, queryString);
			query.setIncludeInferred(true);
			query.execute();
		}
	}

	public void export(File file) throws IOException, RepositoryException, RDFHandlerException {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			export(writer);
		}
	}

	public void export(Writer writer) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = Rio.createWriter(RDFFormat.RDFXML, writer);
		export(rdfWriter);
		writer.flush();
	}

	public void export(Writer writer, RDFFormat format) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = Rio.createWriter(getWriterFormat(format), writer);
		makePrettyTurtle(rdfWriter, format);
		export(rdfWriter);
		writer.flush();
	}

	public void export(OutputStream out, RDFFormat format) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = getWriter(out, format);
		export(rdfWriter);
		out.flush();
	}

	private RDFWriter getWriter(OutputStream out, RDFFormat format) {
		RDFWriter writer = Rio.createWriter(getWriterFormat(format), out);
		makePrettyTurtle(writer, format);
		return writer;
	}

	private static RDFFormat getWriterFormat(RDFFormat format) {
		return format == RDFUtils.TURTLE_PRETTY ? RDFFormat.TURTLE : format;
	}

	private void makePrettyTurtle(RDFWriter rdfWriter, RDFFormat format) {
		if (format == RDFUtils.TURTLE_PRETTY) {
			// Somehow this is the settings that makes the difference between ugly one line turtle files and
			// properly formatted turtle.
			// The java doc warns of potentially high memory usage, but that couldn't be confirmed yet, so this will be
			// the default for now. If it does cause problems for some exports, we always can use export(RDFWriter)
			// with a rdf writer that does not have this setting.
			rdfWriter.getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		}
	}

	public void export(RDFWriter rdfWriter) throws RepositoryException, RDFHandlerException {
		RepositoryConnection connection = getConnection();
		connection.export(rdfWriter);
		connection.close();
	}

	@Override
	public void dump(String query) {
		Matrix<String> matrix = new Matrix<>();
		Stopwatch stopwatch = new Stopwatch();
		//noinspection resource
		try (TupleQueryResult result = sparqlSelect(query).cachedAndClosed()) {
			// prepare headings and column length
			List<String> names = result.getBindingNames();

			// prepare values and update column lengths
			int row = 0;
			while (result.hasNext()) {
				BindingSet bindings = result.next();
				for (int col = 0; col < names.size(); col++) {
					Value value = bindings.getValue(names.get(col));
					if (value instanceof IRI) value = toShortIRI((IRI) value);
					String text = (value == null) ? "null" : value.stringValue();
					matrix.set(row, col, text);
				}
				row++;
			}

			// dump the matrix
			String time = stopwatch.getDisplay();
			matrix.dumpTable(names);
			//noinspection UseOfSystemOutOrSystemErr
			System.out.println("\ncreated " + Strings.pluralOf(matrix.getRowSize(), "row") + " in " + time);
		}
	}

	/**
	 * Creates a full (absolute) URI, based on the specified IRI string. The iri sting may be either an absolute IRI, or
	 * using any known namespace.
	 *
	 * @param shortOrFullIRI the absolute or prefixed IRI name
	 * @return the absolute URI
	 */
	public URI toURI(String shortOrFullIRI) {
		int splitPos = shortOrFullIRI.indexOf(':');
		if (splitPos >= 0) {
			// if there is a ':' contained, check if the text left of it is a known namespace
			for (Namespace namespace : getNamespaces()) {
				String prefix = namespace.getPrefix();
				if (prefix.length() == splitPos && shortOrFullIRI.startsWith(prefix)) {
					// construct full URI based on the prefix name + the specified local name
					return URI.create(namespace.getName() + shortOrFullIRI.substring(splitPos + 1));
				}
			}
		}

		// if not containing a ':', or no prefix matches, create URI from specified IRI
		return URI.create(shortOrFullIRI);
	}

	@NotNull
	@Override
	public IRI toShortIRI(IRI iri) {
		String uriText = iri.toString();
		int length = 0;
		IRI shortURI = iri;
		for (Namespace namespace : getNamespaces()) {
			String partURI = namespace.getName();
			int partLength = partURI.length();
			if (partLength > length && uriText.length() > partLength && uriText.startsWith(partURI)) {
				String shortText = namespace.getPrefix() + ":" + uriText.substring(partLength);
				shortURI = getValueFactory().createIRI(shortText);
				length = partLength;
			}
		}
		return shortURI;
	}

	private interface DataAdder {

		void run(org.eclipse.rdf4j.repository.RepositoryConnection connection) throws IOException, RDFParseException, RepositoryException;
	}

	/**
	 * Overriding {@link LocalRepositoryManager}, which has a potential dead lock when creating and removing
	 * repositories asynchronously.
	 */
	private static class SyncedLocalRepositoryManager extends LocalRepositoryManager {
		private static final Logger LOGGER = LoggerFactory.getLogger(SyncedLocalRepositoryManager.class);

		public SyncedLocalRepositoryManager(File baseDir) {
			super(baseDir);
		}

		@Override
		public synchronized Repository getRepository(String identity) throws RepositoryConfigException, RepositoryException {
			return super.getRepository(identity);
		}
	}
}
