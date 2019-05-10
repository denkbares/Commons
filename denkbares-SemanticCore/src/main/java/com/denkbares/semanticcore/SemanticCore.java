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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
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
import com.denkbares.strings.Strings;
import com.denkbares.utils.Files;
import com.denkbares.utils.Log;
import com.denkbares.utils.Stopwatch;
import com.denkbares.utils.Streams;

public final class SemanticCore {

	private static final long MAX_SHUTDOWN_WAIT = 1000 * 60;

	public enum State {
		initializing, active, shutdown
	}

	private volatile State state = State.initializing;

	private final Object connectionsMutex = new Object();
	private static final Map<String, SemanticCore> instances = new HashMap<>();
	private static final Object lazyInitializationMutex = new Object();
	private static volatile boolean lazyInitializationDone = false;
	private static LocalRepositoryManager repositoryManager = null;
	private static final int THRESHOLD_TIME = 1000 * 60 * 2; // 2 min...
	public static final String DEFAULT_NAMESPACE = "http://www.denkbares.com/ssc/ds#";
	private static final int TEMP_DIR_ATTEMPTS = 1000;
	private final String repositoryId;
	private final Repository repository;
	private final AtomicLong allocationCounter = new AtomicLong(0);
	private final Set<ConnectionInfo> connections = new HashSet<>();
	private final static boolean rememberConnectionOpeningStack = "true".equals(System.getProperty("semanticcore.connection.debug.stacktrace", "false"));
	//	private AtomicLong connectionCounter = new AtomicLong(0);

	static {
		Log.info("Connection debugging with stack traces: " + rememberConnectionOpeningStack);
	}

	private final ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor();

	public static SemanticCore getInstance(String key) {
		return instances.get(key);
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning) throws IOException {
		return getOrCreateInstance(key, reasoning, (String) null);
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, File tmpPath) throws IOException {
		return getOrCreateInstance(key, reasoning, tmpPath.getPath());
	}

	public static SemanticCore getOrCreateInstance(String key, RepositoryConfig reasoning, String tmpPath) throws IOException {
		SemanticCore instance = getInstance(key);
		if (instance == null) {
			instance = createInstance(key, reasoning, tmpPath);
		}
		return instance;
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning) throws IOException {
		return createInstance(key, reasoning, null);
	}

	public static SemanticCore createInstance(String key, RepositoryConfig reasoning, String tmpFolder) throws IOException {
		Objects.requireNonNull(reasoning);
		SemanticCore instance = new SemanticCore(key, null, reasoning, tmpFolder, null);
		instances.put(key, instance);
		instance.state = State.active;
		Log.info("Created SemanticCore '" + instance.repositoryId + "' with config " + reasoning.getName());
		return instance;
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

		initializeLazy(tmpFolder);

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
		initConnectionDaemon();
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

	private void initConnectionDaemon() {
		// checks every 2 min for open connections and warns about them...
		// noinspection CodeBlock2Expr
		daemon.scheduleWithFixedDelay(() -> {
			synchronized (connectionsMutex) {
				connections.removeIf(connectionInfo -> {
					try {
						if (connectionInfo.connection.isOpen()) {
							if (connectionInfo.stopWatch.getTime() > THRESHOLD_TIME) {
								String message = "There is a connection that is open for "
										+ connectionInfo.stopWatch.getDisplay()
										+ ". If these messages continue, it might be an indication that something "
										+ "went wrong in the code.";
								if (connectionInfo.stackTrace != null) {
									message += "\n" + Strings.concat("\n\t", connectionInfo.stackTrace);
								}
								Log.warning(message);
							}
							return false;
						}
						else {
							return true; // remove connection if closed
						}
					}
					catch (RepositoryException e) {
						Log.severe("Exception while checking connection status", e);
					}
					return false;
				});
			}
		}, 0, THRESHOLD_TIME, TimeUnit.MILLISECONDS);
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
			shutdown();
		}
	}

	/**
	 * Shuts down this semantic core, if it is not allocated. In this case destroys this instance so that is should not
	 * be used any longer. It is also removed from the internal SemanticCore caches. If this instance is allocated at
	 * least once, the method does nothing.
	 *
	 * @see #allocate()
	 * @see #release()
	 * @see #shutdown()
	 */
	public void requestShutdown() {
		if (!isAllocated()) shutdown();
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
	public void shutdown() {
		state = State.shutdown;
		try {
			Stopwatch stopwatch = new Stopwatch();
			synchronized (connectionsMutex) {
				// we check the connections and wait a finite MAX_SHUTDOWN_WAIT time for them to close if they are still open
				long start = System.currentTimeMillis();
				while (true) {
					connections.removeIf(connectionInfo -> {
						try {
							if (!connectionInfo.connection.isOpen()) return true;
						}
						catch (RepositoryException e) {
							Log.info("Exception while checking connection status", e);
						}
						Log.info("Waiting for connection to close...");
						return false;
					});
					if (connections.isEmpty() || System.currentTimeMillis() - start > MAX_SHUTDOWN_WAIT) {
						break;
					}
					// sleep some time till the next try
					//noinspection BusyWait
					Thread.sleep(1000);
				}
				// if there are still open questions, they will be closed by force
				connections.forEach(connectionInfo -> {
					try {
						connectionInfo.connection.close();

						String message = "Shutting down repository connection by force, this might cause subsequent exceptions.";
						if (connectionInfo.stackTrace != null) {
							String trace = Strings.concat("\n\t", connectionInfo.stackTrace);
							message += "\nConnection was opened with the following trace:\n" + trace;
						}
						Log.severe(message);
					}
					catch (RepositoryException e) {
						Log.info("Unable to shutdown connection", e);
					}
				});
				connections.clear();
			}
			daemon.shutdown();
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
		new ArrayList<>(instances.values()).forEach(SemanticCore::shutdown);
	}

	private synchronized static void initializeRepositoryManagerLazy(String repositoryPath) throws IOException {
		if (repositoryManager != null) return; // could already be initialized externally
		if (repositoryPath == null) repositoryPath = createRepositoryPath("Default");
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
			repositoryManager.initialize();
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
			Log.severe("Unable to retrieve repositories during manager shutdown", e);
		}
		repositoryManager.shutDown();
		lazyInitializationDone = false;
	}

	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		// check state also before synchronizing to avoid having to wait for connection shutdown
		// just to learn that the core is already shut down
		if (state == State.shutdown) throwShutdownException();
		synchronized (connectionsMutex) {
			if (state == State.shutdown) throwShutdownException();
			org.eclipse.rdf4j.repository.RepositoryConnection connection = repository.getConnection();
			connections.add(new ConnectionInfo(connection));
			return new RepositoryConnection(connection);
		}
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

	/**
	 * Reads an UPDATE query from a file and executes it
	 *
	 * @param sparqlFile   Input File
	 * @param replacements String arrays of replacements to run on the query
	 * @created 09.01.2015
	 */
	public void update(String sparqlFile, String[]... replacements) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		String queryString = SPARQLLoader.load(sparqlFile, getClass());
		for (String[] replacement : replacements) {
			queryString = queryString.replaceAll(replacement[0], replacement[1]);
		}
		update(queryString);
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

	/**
	 * Reads a SELECT query from a file and executes it
	 *
	 * @param sparqlFile   Input File
	 * @param replacements String arrays of replacements to run on the query
	 * @return A {@see TupleQueryResult}
	 */
	public TupleQueryResult query(String sparqlFile, String[]... replacements) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String queryString = SPARQLLoader.load(sparqlFile, getClass());
		for (String[] replacement : replacements) {
			if (replacement.length > 1) {
				queryString = queryString.replaceAll(replacement[0], replacement[1]);
			}
		}
		return query(queryString);
	}

	/**
	 * Performs a SELECT query
	 *
	 * @param queryString SPARQL query
	 * @return A {@see TupleQueryResult}
	 */
	public TupleQueryResult query(String queryString) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		RepositoryConnection connection = getConnection();
		try {
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			return new TupleQueryResult(connection, query.evaluate());
		}
		catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			// if an exception occurs preparing the result instance, but after the connection has been created,
			// we have to close the connection manually, otherwise nobody would close
			connection.close();
			throw e;
		}
	}

	/**
	 * Reads an ASK query from a file and executes it
	 *
	 * @param sparqlFile   Input File
	 * @param replacements String arrays of replacements to run on the query
	 * @return Result of the ASK query
	 */
	public boolean ask(String sparqlFile, String[]... replacements) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String queryString = SPARQLLoader.load(sparqlFile, getClass());
		for (String[] replacement : replacements) {
			queryString = queryString.replaceAll(replacement[0], replacement[1]);
		}
		return ask(queryString);
	}

	/**
	 * Performs an ASK query
	 *
	 * @param queryString SPARQL query
	 * @return Result of the ASK query
	 */
	public boolean ask(String queryString) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = getConnection()) {
			BooleanQuery query = connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		}
	}

	private interface DataAdder {

		void run(org.eclipse.rdf4j.repository.RepositoryConnection connection) throws IOException, RDFParseException, RepositoryException;
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
		Update query = getConnection().prepareUpdate(QueryLanguage.SPARQL, queryString);
		query.setIncludeInferred(true);
		query.execute();
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

	private void export(RDFWriter rdfWriter) throws RepositoryException, RDFHandlerException {
		RepositoryConnection connection = getConnection();
		connection.export(rdfWriter);
		connection.close();
	}

	public void export(OutputStream out, RDFFormat format) throws RepositoryException, RDFHandlerException, IOException {
		RDFWriter rdfWriter = Rio.createWriter(format, out);
		export(rdfWriter);
		out.flush();
	}

	/**
	 * Executes the sparql query, and dumps the result to the console, as a human-readable ascii formatted table. The
	 * bound variables are in the title of the table, the column widths are adjusted to the content of each column. URI
	 * references are abbreviated as the namespace is known to this core.
	 *
	 * @param query the sparql query to be executed
	 */
	@SuppressWarnings("unused")
	public void dump(String query) {
		Matrix<String> matrix = new Matrix<>();
		try (TupleQueryResult result = query(query).cachedAndClosed(true)) {
			// prepare headings and column length
			List<String> names = result.getBindingNames();

			// prepare values and update column lengths
			int row = 0;
			while (result.hasNext()) {
				BindingSet bindings = result.next();
				for (int col = 0; col < names.size(); col++) {
					Value value = bindings.getValue(names.get(col));
					if (value instanceof IRI) value = result.toShortIRI((IRI) value);
					String text = (value == null) ? "null" : value.stringValue();
					matrix.set(row, col, text);
				}
				row++;
			}

			// dump the matrix
			matrix.dumpTable(names);
		}
	}

	private static class ConnectionInfo {

		private final Stopwatch stopWatch;
		private final org.eclipse.rdf4j.repository.RepositoryConnection connection;
		private final StackTraceElement[] stackTrace;

		ConnectionInfo(org.eclipse.rdf4j.repository.RepositoryConnection connection) {
			this.connection = connection;
			// for debugging purposes....
			if (rememberConnectionOpeningStack) {
				StackTraceElement[] stackTrace = new Exception().getStackTrace();
				this.stackTrace = Arrays.copyOfRange(stackTrace, 2, stackTrace.length);
			}
			else {
				this.stackTrace = null;
			}
			this.stopWatch = new Stopwatch();
		}
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
