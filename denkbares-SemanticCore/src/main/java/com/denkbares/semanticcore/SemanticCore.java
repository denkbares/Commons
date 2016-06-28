package com.denkbares.semanticcore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.openrdf.OpenRDFException;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.denkbares.semanticcore.config.RepositoryConfig;
import de.d3web.strings.Strings;
import de.d3web.utils.Files;
import de.d3web.utils.Log;
import de.d3web.utils.Stopwatch;
import de.d3web.utils.Streams;

public final class SemanticCore {

	private static final Object DUMMY = new Object();
	private static final Map<String, SemanticCore> instances = new HashMap<>();
	private static final Object repositoryManagerMutex = new Object();
	private static volatile LocalRepositoryManager repositoryManager = null;
	private static final int THRESHOLD_TIME = 1000 * 60 * 2; // 2 min...
	public static final String DEFAULT_NAMESPACE = "http://www.denkbares.com/ssc/ds#";
	private static final int TEMP_DIR_ATTEMPTS = 1000;
	private final String repositoryId;
	private final AtomicLong allocationCounter = new AtomicLong(0);

	private final Repository repository;
	private final ConcurrentHashMap<ConnectionInfo, Object> connections = new ConcurrentHashMap<>();
	//	private AtomicLong connectionCounter = new AtomicLong(0);
	private final ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread();
		thread.setDaemon(true);
		return thread;
	});

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

	public static void shutDownAllRepositories() {
		// create new list to avoid concurrent modification exception
		new ArrayList<>(instances.values()).forEach(com.denkbares.semanticcore.SemanticCore::shutdown);
	}

	private SemanticCore(String repositoryId, String repositoryLabel, RepositoryConfig repositoryConfig, String tmpFolder, Map<String, String> overrides) throws IOException {
		this.repositoryId = repositoryId;
		initializeRepositoryManagerIfNecessary(tmpFolder);
		try {
			if (repositoryManager.hasRepositoryConfig(repositoryId)) {
				throw new RuntimeException("Repository " + repositoryId + " already exists.");
			}

			org.openrdf.repository.config.RepositoryConfig openRdfRepositoryConfig = repositoryConfig.createRepositoryConfig(repositoryId, repositoryLabel, overrides);

			repositoryManager.addRepositoryConfig(openRdfRepositoryConfig);

			// Get the repository and connect to it!
			this.repository = repositoryManager.getRepository(repositoryId);
			repositoryConfig.postCreationHook(this.repository);

			try (RepositoryConnection connection = getConnection()) {
				connection.setNamespace("des", DEFAULT_NAMESPACE);
			}
		}
		catch (OpenRDFException e) {
			throw new IOException("Cannot initialize repository", e);
		}
		initConnectionDaemon();

	}

	private void initConnectionDaemon() {
		// checks every 2 min for open connections and warns about them...
		// noinspection CodeBlock2Expr
		daemon.scheduleAtFixedRate(() -> {
			connections.forEachKey(Long.MAX_VALUE, connectionInfo -> {
				if (connectionInfo.stopWatch.getTime() < THRESHOLD_TIME) return;
				try {
					if (connectionInfo.connection.isOpen()) {
						Log.warning("There is an connection that is open since "
								+ connectionInfo.stopWatch.getDisplay()
								+ ". If these messages continue, it might be an indication that something "
								+ "went wrong in the code.\n"
								+ Strings.concat("\n\t", connectionInfo.stackTrace));
					}
					else {
						connections.remove(connectionInfo);
					}
				}
				catch (RepositoryException e) {
					Log.severe("Exception while checking connection status", e);
				}
			});
		}, 0, 120, TimeUnit.SECONDS);
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
	 * Releases a previously allocated core. If the core has been released as many times as it has
	 * been allocated, the underlying repository is shut down and the core is removed from the
	 * internal SemanticCore caches. You must make sure that for every time allocate is called there
	 * is exactly one call to release as well.
	 */
	public void release() {
		long counter = allocationCounter.decrementAndGet();
		if (counter == 0) {
			shutdown();
		}
	}

	/**
	 * Shuts down this semantic core, independently from any allocation / release state. It destroys
	 * this instance so that is should not be used any longer. It is also removed from the internal
	 * SemanticCore caches.
	 *
	 * @see #allocate()
	 * @see #release()
	 * @see #requestShutdown()
	 */
	public void shutdown() {
		try {
			connections.forEachKey(Long.MAX_VALUE, connectionInfo -> {
				try {
					connectionInfo.connection.close();
					connections.remove(connectionInfo);
				}
				catch (RepositoryException e) {
					Log.info("Unable to shutdown connection", e);
				}
			});
			daemon.shutdown();
			repository.shutDown();
			Log.info("SemanticCore " + repositoryId + " shut down successful.");
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

	/**
	 * Shuts down this semantic core, if it is not allocated. In this case destroys this instance so
	 * that is should not be used any longer. It is also removed from the internal SemanticCore
	 * caches. If this instance is allocated at least once, the method does nothing.
	 *
	 * @see #allocate()
	 * @see #release()
	 * @see #shutdown()
	 */
	public void requestShutdown() {
		if (allocationCounter.get() <= 0) shutdown();
	}

	private static void initializeRepositoryManagerIfNecessary(String repositoryPath) throws IOException {
		if (repositoryManager == null) {
			synchronized (repositoryManagerMutex) {
				if (repositoryManager == null) {
					if (repositoryPath == null) repositoryPath = createRepositoryPath("Default");
					initializeRepositoryManager(repositoryPath);
				}
			}
		}
	}

	public static void initializeRepositoryManager(String repositoryPath) throws IOException {
		File repositoryFolder = new File(repositoryPath, "repositories");
		// clean repository folder...
		if (repositoryFolder.exists() && repositoryFolder.isDirectory()) {
			FileUtils.deleteDirectory(repositoryFolder);
		}
		File tempFolder = new File(repositoryPath);
		repositoryManager = new LocalRepositoryManager(tempFolder);
		Log.info("Created new repository manager at: " + tempFolder.getCanonicalPath());
		try {
			repositoryManager.initialize();
		}
		catch (RepositoryException e) {
			throw new IOException("Cannot initialize repository", e);
		}
	}

	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	public RepositoryConnection getConnection() throws RepositoryException {
		org.openrdf.repository.RepositoryConnection connection = repository.getConnection();
		connections.put(new ConnectionInfo(connection), DUMMY);
//		System.out.println(connections.size() + " open connections on repo " + repositoryId + ", " + connectionCounter.incrementAndGet() + " created since startup");
		return new RepositoryConnection(connection);
	}

	public void addData(InputStream is, String fileExtention) throws RDFParseException, RepositoryException, IOException {
		File tempFile = File.createTempFile("SemanticCore", "." + fileExtention);
		tempFile.deleteOnExit();
		Streams.streamAndClose(is, new FileOutputStream(tempFile));
		addData(tempFile);
	}

	public void addData(InputStream is, RDFFormat format) throws RDFParseException, RepositoryException, IOException {
		org.openrdf.repository.RepositoryConnection connection = this.getConnection();
		try {
			connection.add(is, DEFAULT_NAMESPACE, format);
		}
		catch (Exception e) {
			Log.severe("Exception while adding data to semantic core.", e);
		}
		finally {
			//noinspection ThrowFromFinallyBlock
			connection.close();
			// throwing inside finally should be ok with logging of exception above
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
		this.getConnection().add(file, DEFAULT_NAMESPACE, format);
	}

	private void addDataFromInputStream(InputStream is, RDFFormat format)
			throws IOException, RDFParseException, RepositoryException {
		this.getConnection().add(is, DEFAULT_NAMESPACE, format);
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
			format = Rio.getParserFormatForFileName(fileName);
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
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
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
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public TupleQueryResult query(String queryString) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		RepositoryConnection connection = getConnection();
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		return new TupleQueryResult(connection, query.evaluate());
	}

	/**
	 * Reads an ASK query from a file and executes it
	 *
	 * @param sparqlFile   Input File
	 * @param replacements String arrays of replacements to run on the query
	 * @return Result of the ASK query
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
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
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	public boolean ask(String queryString) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = getConnection()) {
			BooleanQuery query = connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		}
	}

	/**
	 * Reads an UPDATE query from a file and executes it
	 *
	 * @param sparqlFile   Input File
	 * @param replacements String arrays of replacements to run on the query
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws UpdateExecutionException
	 * @created 09.01.2015
	 */
	public void update(String sparqlFile, String[]... replacements) throws RepositoryException, MalformedQueryException, QueryEvaluationException, UpdateExecutionException {
		String queryString = SPARQLLoader.load(sparqlFile, getClass());
		for (String[] replacement : replacements) {
			queryString = queryString.replaceAll(replacement[0], replacement[1]);
		}
		update(queryString);
	}

	/**
	 * Performs an UPDATE query.
	 * <p>
	 * Please be aware that this actually updates the content of the SemanticCore, i.e. adds or
	 * deletes statements!
	 *
	 * @param queryString the query to update the contents
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws UpdateExecutionException
	 * @created 09.01.2015
	 */
	public void update(String queryString) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		Update query = getConnection().prepareUpdate(QueryLanguage.SPARQL, queryString);
		query.setIncludeInferred(true);
		query.execute();
	}

	public void export(File file) throws IOException, RepositoryException, RDFHandlerException {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
			export(writer);
			writer.close();
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

	private static class ConnectionInfo {

		private final Stopwatch stopWatch;
		private final org.openrdf.repository.RepositoryConnection connection;
		private final StackTraceElement[] stackTrace;

		ConnectionInfo(org.openrdf.repository.RepositoryConnection connection) {
			this.connection = connection;
			// for debugging purposes....
			StackTraceElement[] stackTrace = new Exception().getStackTrace();
			this.stackTrace = Arrays.copyOfRange(stackTrace, 2, stackTrace.length);
			this.stopWatch = new Stopwatch();
		}
	}
}
