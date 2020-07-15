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
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
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
import org.jetbrains.annotations.NotNull;

import com.denkbares.collections.Matrix;
import com.denkbares.events.EventManager;
import com.denkbares.semanticcore.config.RepositoryConfig;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Files;
import com.denkbares.utils.Log;
import com.denkbares.utils.Stopwatch;
import com.denkbares.utils.Streams;

public final class SemanticCore implements SPARQLEndpoint {

	public enum State {
		active, shutdown
	}

	private volatile State state;

	// we use a map of atomic reference, to immediately add the reference to block the key,
	// even before the initialization of the semantic core is completed
	private static final Map<String, AtomicReference<SemanticCore>> instances = new ConcurrentHashMap<>();

	private static final Object lazyInitializationMutex = new Object();
	private static volatile boolean lazyInitializationDone = false;
	private static LocalRepositoryManager repositoryManager = null;
	private static final int THRESHOLD_TIME = 1000 * 60 * 2; // 2 min...
	public static final String DEFAULT_NAMESPACE = "http://www.denkbares.com/ssc/ds#";
	private static final int TEMP_DIR_ATTEMPTS = 1000;
	private final String repositoryId;
	private final Repository repository;
	private final AtomicLong allocationCounter = new AtomicLong(0);

	public static SemanticCore getInstance(String key) {
		AtomicReference<SemanticCore> reference = instances.get(key);
		if (reference == null) {
			return null;
		}
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (reference) {
			return reference.get();
		}
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning) throws IOException {
		return getOrCreateInstance(key, reasoning, (String) null);
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, File tmpPath) throws IOException {
		return getOrCreateInstance(key, reasoning, tmpPath.getPath());
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, String tmpPath) throws IOException {
		AtomicReference<SemanticCore> reference = instances.computeIfAbsent(key, k -> new AtomicReference<>());
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (reference) {
			createInstance(reference, key, reasoning, tmpPath);
		}
		return reference.get();
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning) throws IOException {
		return createInstance(key, reasoning, null);
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning, String tmpFolder) throws IOException {
		Objects.requireNonNull(reasoning);
		AtomicReference<SemanticCore> reference = new AtomicReference<>();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (reference) {
			instances.put(key, reference);
			createInstance(reference, key, reasoning, tmpFolder);
		}
		SemanticCore instance = reference.get();
		Log.info("Created SemanticCore '" + instance.repositoryId + "' with config " + reasoning.getName());
		return instance;
	}

	private static void createInstance(AtomicReference<SemanticCore> reference, String key, RepositoryConfig reasoning, String tmpFolder) throws IOException {
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

	public static String createRepositoryPath(String suffix) throws IOException {
		@NotNull File systemTempDir = Files.getSystemTempDir();
		String baseName = SemanticCore.class.getName().replaceAll("\\W", "-") + "-" + suffix + "-";
		for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
			File tempDirCandidate = new File(systemTempDir, baseName + counter);
			if (tempDirCandidate.mkdir()) {
				tempDirCandidate.deleteOnExit();
				tryToLock(tempDirCandidate); // lock dir so other JVMs can't use it
				return tempDirCandidate.getAbsolutePath();
			}
			else {
				// if the dir already exists, try to reuse it
				// we reuse it, if we can get a lock on it
				// (meaning no other SemanticCore from another JVM is currently locking it)
				try {
					tryToLock(tempDirCandidate);
					return tempDirCandidate.getAbsolutePath();
				}
				catch (Exception ignore) {
					// ignore and try next one
				}
			}
		}
		throw new IOException("Failed to create temp directory");
	}

	private static FileLock tryToLock(File tempDirCandidate) throws IOException {
		FileChannel channel = new RandomAccessFile(
				new File(tempDirCandidate, "lock"), "rw").getChannel();
		FileLock fileLock = channel.tryLock();
		if (fileLock == null) {
			throw new IOException("Unable to lock file " + tempDirCandidate);
		}
		return fileLock;
	}

	private SemanticCore(String repositoryId, String repositoryLabel, RepositoryConfig repositoryConfig, String tmpFolder, Map<String, String> overrides) throws IOException {
		initializeLazy(tmpFolder == null ? createRepositoryPath(repositoryId) : tmpFolder);
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

	private void initializeLazy(String tmpFolder) throws IOException {
		if (!lazyInitializationDone) {
			synchronized (lazyInitializationMutex) {
				if (!lazyInitializationDone) {
					initializeRepositoryManagerLazy(tmpFolder);
					lazyInitializationDone = true;
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
	 * Shuts down this semantic core, independently from any allocation / release state. It destroys this instance so
	 * that is should not be used any longer. It is also removed from the internal SemanticCore caches.
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
			repository.shutDown();
			Log.info("SemanticCore " + repositoryId + " shut down successfully in " + stopwatch.getDisplay());
		}
		catch (Exception e) {
			Log.severe("Exception while shutting down repository " + repositoryId + ", removing repository anyway", e);
		}
		finally {
			instances.remove(repositoryId);
			try {
				repositoryManager.removeRepository(repositoryId);
			}
			catch (RepositoryException | RepositoryConfigException e) {
				Log.info("Unable to remove repository from manager", e);
			}
		}
	}

	public static void shutDownAllRepositories() {
		// create new list to avoid concurrent modification exception
		new ArrayList<>(instances.keySet()).stream().map(SemanticCore::getInstance)
				.filter(Objects::nonNull).forEach(SemanticCore::close);
	}

	private synchronized static void initializeRepositoryManagerLazy(String repositoryPath) throws IOException {
		if (repositoryManager != null) return; // could already be initialized externally
		if (repositoryPath == null) {
			repositoryPath = createRepositoryPath("Default");
		}
		initializeRepositoryManager(repositoryPath);
	}

	public static void initializeRepositoryManager(String repositoryPath) throws IOException {
		File repositoryFolder = new File(repositoryPath, "repositories");
		// clean repository folder...
		if (repositoryFolder.exists() && repositoryFolder.isDirectory()) {
			FileUtils.deleteDirectory(repositoryFolder);
		}
		File tempFolder = new File(repositoryPath);
		repositoryManager = new SyncedLocalRepositoryManager(tempFolder);
		Log.info("Created new repository manager at: " + tempFolder.getCanonicalPath());
		try {
			repositoryManager.init();
		}
		catch (RepositoryException e) {
			throw new IOException("Cannot initialize repository", e);
		}
	}

	public static void shutDownRepositoryManager() {
		Log.info("Shutting down repository manager.");
		// shut down any remaining repositories
		try {
			repositoryManager.getRepositoryIDs().forEach(id -> {
				try {
					repositoryManager.getRepository(id).shutDown();
					Log.info("Repository " + id + " shut down successful.");
				}
				catch (RepositoryException | RepositoryConfigException e) {
					Log.severe("Unable to shut down repository " + id, e);
				}
			});
		}
		catch (RepositoryException e) {
			Log.severe("Unable to retrieve repositories during manager close", e);
		}
		repositoryManager.shutDown();
		lazyInitializationDone = false;
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
		try {
			addData(connection -> connection.add(is, DEFAULT_NAMESPACE, format));
		}
		finally {
			is.close();
		}
	}

	public void addData(Reader reader, RDFFormat format) throws RepositoryException, RDFParseException, IOException {
		addData(connection -> connection.add(reader, DEFAULT_NAMESPACE, format));
	}

	public void addData(DataAdder adder) throws RepositoryException, RDFParseException, IOException {
		try (org.eclipse.rdf4j.repository.RepositoryConnection connection = this.getConnection()) {
			adder.run(connection);
		}
	}

	public void addData(File file) throws RDFParseException, RepositoryException, IOException {
		if (!file.exists()) {
			String message = "ontology file not found: " + file.getAbsolutePath();
			Log.severe(message);
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
		RepositoryConnection connection = getConnection();
		connection.export(rdfWriter);
		writer.flush();
		connection.close();
	}

	public void export(Writer writer, RDFFormat format) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = Rio.createWriter(format, writer);
		export(rdfWriter);
		writer.flush();
	}

	public void export(RDFWriter rdfWriter) throws RepositoryException, RDFHandlerException {
		RepositoryConnection connection = getConnection();
		connection.export(rdfWriter);
		connection.close();
	}

	public void export(OutputStream out, RDFFormat format) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = Rio.createWriter(format, out);
		export(rdfWriter);
		out.flush();
	}

	@Override
	public void dump(String query) {
		Matrix<String> matrix = new Matrix<>();
		Stopwatch stopwatch = new Stopwatch();
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

	public IRI toShortIRI(URI uri) {
		return toShortIRI(getValueFactory().createIRI(uri.toString()));
	}

	private IRI toShortIRI(IRI iri) {
		String uriText = iri.toString();
		int length = 0;
		IRI shortURI = iri;
		for (Namespace namespace : getNamespaces()) {
			String partURI = namespace.getName();
			int partLength = partURI.length();
			if (partLength > length && uriText.length() > partLength && uriText.startsWith(partURI)) {
				String shortText = namespace.getPrefix() + ":" + uriText.substring(partLength);
				shortURI = new SimpleIRI(shortText) {
					private static final long serialVersionUID = 8831976782866898688L;
				};
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

		public SyncedLocalRepositoryManager(File baseDir) {
			super(baseDir);
		}

		@Override
		public synchronized Repository getRepository(String identity) throws RepositoryConfigException, RepositoryException {
			return super.getRepository(identity);
		}
	}
}
