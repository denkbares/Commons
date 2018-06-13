package com.denkbares.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.collections.Matrix;
import com.denkbares.strings.StringFragment;
import com.denkbares.strings.Strings;

public class Files {

	private static final int TEMP_DIR_ATTEMPTS = 1000;

	/**
	 * Create a new temporary directory in the systems temp directory. Use {@link #recursiveDelete(File)} to clean this
	 * directory up since it isn't deleted automatically
	 *
	 * @return the new directory
	 * @throws IOException if there is an error creating the temporary directory
	 */
	@NotNull
	public static File createTempDir() throws IOException {
		return findTempDir(getSystemTempDir());
	}

	/**
	 * Create a new temporary directory as a sub-directory of the specified base directory. Use {@link
	 * #recursiveDelete(File)} to clean this directory up since it isn't deleted automatically.
	 *
	 * @return the new directory
	 * @throws IOException if there is an error creating the temporary directory
	 */
	@NotNull
	public static File createTempDir(File baseDir) throws IOException {
		assertTempDir(baseDir);
		return findTempDir(baseDir);
	}

	@NotNull
	private static File findTempDir(File baseDir) throws IOException {
		String baseName = System.currentTimeMillis() + "-";
		for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
			File tempDir = new File(baseDir, baseName + counter);
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}
		throw new IOException("Failed to create temp directory");
	}

	/**
	 * Writes the given string content to a file with the given path.
	 *
	 * @param path    the path to the file to be written
	 * @param content the content of the file to be written
	 * @throws IOException if writing fails
	 */
	public static void writeFile(String path, String content) throws IOException {
		writeFile(new File(path), content);
	}

	/**
	 * Writes the given string content to the given file. Creates missing parent directories is required.
	 *
	 * @param file    the file to be written
	 * @param content the content of the file to be written
	 * @throws IOException if writing fails
	 */
	public static void writeFile(File file, String content) throws IOException {
		file.getParentFile().mkdirs();
		try (Writer stream = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
			BufferedWriter out = new BufferedWriter(stream);
			out.write(content);
			out.close();
		}
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 *
	 * @param filePath the file to be loaded
	 * @return the contents of the file
	 * @throws IOException          if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 * @created 16.09.2012
	 */
	public static String readFile(String filePath) throws IOException {
		return readFile(readFile(filePath, "UTF-8"));
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 *
	 * @param file the file to be loaded
	 * @return the contents of the file
	 * @throws IOException          if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 * @created 13.06.2018
	 */
	public static String readFile(String file, String charset) throws IOException {
		return readFile(new File(file), charset);
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 *
	 * @param file the file to be loaded
	 * @return the contents of the file
	 * @throws IOException          if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 * @created 16.09.2012
	 */
	public static String readFile(File file) throws IOException {
		return readFile(file, "UTF-8");
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 *
	 * @param file the file to be loaded
	 * @return the contents of the file
	 * @throws IOException          if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 * @created 13.06.2018
	 */
	public static String readFile(File file, String charset) throws IOException {
		return Streams.readStream(new FileInputStream(file), charset);
	}

	/**
	 * Convenience method to get the default temp dir of the OS.
	 *
	 * @return the system's temp dir
	 * @throws IOException if access is not possible
	 */
	@NotNull
	public static File getSystemTempDir() throws IOException {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		assertTempDir(baseDir);
		return baseDir;
	}

	private static void assertTempDir(File baseDir) throws IOException {
		baseDir.mkdirs();
		if (!baseDir.isDirectory()) {
			throw new IOException("Failed to access temp directory: " + baseDir);
		}
	}

	/**
	 * Recursively delete file or directory
	 *
	 * @param fileOrDir the file or dir to delete
	 * @return true iff all files are successfully deleted
	 */
	public static boolean recursiveDelete(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			// recursively delete contents
			//noinspection ConstantConditions
			for (File innerFile : fileOrDir.listFiles()) {
				if (!recursiveDelete(innerFile)) {
					return false;
				}
			}
		}
		return fileOrDir.delete();
	}

	/**
	 * Recursively copy a file or directory.
	 *
	 * @param source the source file or directory to read from
	 * @param target the target file or directory
	 */
	public static void recursiveCopy(File source, File target) throws IOException {
		if (source.isDirectory()) {
			// recursively copy contents
			//noinspection ConstantConditions
			for (File innerSource : source.listFiles()) {
				File innerTarget = new File(target, innerSource.getName());
				recursiveCopy(innerSource, innerTarget);
			}
		}
		else {
			copy(source, target);
		}
	}

	/**
	 * Copies the source file to the target file. If the target file already exists it will be overwritten. If any of
	 * the specified files denote a folder, an IOException is thrown. If the path of the target file does not exists,
	 * the required parent folders will be created.
	 *
	 * @param source the source file to read from
	 * @param target the target file
	 * @throws IOException if the file cannot be copied
	 */
	public static void copy(File source, File target) throws IOException {
		target.getAbsoluteFile().getParentFile().mkdirs();
		try (InputStream in = new FileInputStream(source);
			 OutputStream out = new FileOutputStream(target)) {
			Streams.stream(in, out);
		}
		//noinspection ResultOfMethodCallIgnored
		target.setLastModified(source.lastModified());
	}

	/**
	 * Checks if two files have the same fingerprint, including the timestamp, so it appears that they have the same
	 * content, without fully reading the files contents! This method is much quicker that {#hasEqualContent} but you
	 * cannot be sure if the content really differs if the method return true.
	 * <p>
	 * Returns true if both files exists, both denote a file (not a directory), and the file seems to be identical.
	 *
	 * @param file1 the first file to compare
	 * @param file2 the second file to compare
	 * @return if both files seems to be identical
	 */
	@SuppressWarnings("RedundantIfStatement")
	public static boolean hasEqualFingerprint(File file1, File file2) throws IOException {
		if (!file1.isFile()) return false;
		if (!file2.isFile()) return false;
		if (file1.length() != file2.length()) return false;
		if (file1.lastModified() != file2.lastModified()) return false;
		return true;
	}

	/**
	 * Checks if two files have the same content. Returns true if both files exists, both denote a file (not a
	 * directory), and the bytes of each file are identical.
	 *
	 * @param file1 the first file to compare
	 * @param file2 the second file to compare
	 * @return if both files have the same content
	 */
	public static boolean hasEqualContent(File file1, File file2) throws IOException {
		if (!file1.isFile()) return false;
		if (!file2.isFile()) return false;
		if (file1.length() != file2.length()) return false;

		FileInputStream in1 = null, in2 = null;
		try {
			in1 = new FileInputStream(file1);
			in2 = new FileInputStream(file2);
			while (true) {
				int byte1 = in1.read();
				int byte2 = in2.read();
				if (byte1 != byte2) return false;
				if (byte1 == -1) return true;
			}
		}
		finally {
			Streams.closeQuietly(in1);
			Streams.closeQuietly(in2);
		}
	}

	/**
	 * Convenience method to get a reader from a file (using the correct (UTF-8) encoding).
	 *
	 * @param file the file to create the reader for
	 * @return a reader for the given file
	 */
	public static Reader getReader(File file) throws FileNotFoundException, UnsupportedEncodingException {
		return new InputStreamReader(new FileInputStream(file), "UTF-8");
	}

	/**
	 * Returns the contents of the specified file as a byte[].
	 *
	 * @param file the input file to read from
	 * @return the content of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 01.10.2013
	 */
	public static byte[] getBytes(File file) throws IOException {
		return Streams.getBytesAndClose(new FileInputStream(file));
	}

	/**
	 * Returns the contents of the specified file as a String.
	 *
	 * @param file the input file to read from
	 * @return the content of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 01.10.2013
	 */
	public static String getText(File file) throws IOException {
		return getText(file, "UTF-8");
	}

	/**
	 * Returns the contents of the specified file as a String.
	 *
	 * @param file the input file to read from
	 * @return the content of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 13.06.2018
	 */
	public static String getText(File file, String charset) throws IOException {
		return Streams.getTextAndClose(new FileInputStream(file));
	}

	/**
	 * Returns the lines of the specified file as a list of Strings.
	 *
	 * @param file the input file to read from
	 * @return the lines of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 01.10.2013
	 */
	public static List<String> getLines(File file) throws IOException {
		try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
			return getLines(reader);
		}
	}

	/**
	 * Reads a properties file into a newly created Properties objects and returns the Properties objects.
	 *
	 * @param file the properties file to be read
	 * @return the loaded file content
	 * @throws IOException if the file cannot be loaded or the file's content cannot be parsed
	 */
	public static Properties getProperties(File file) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(file)) {
			properties.load(in);
		}
		return properties;
	}

	/**
	 * Reads and rewrites the a properties file, adding one entry, overwriting all existing entries with the specified
	 * key. It preserves all other lines, including comments and the order of the lines. Only the lines with the
	 * specified key will be modified, where the first one is overwritten, and succeeding ones (if there are any) will
	 * be deleted. If there is no such line contained, the new property will be appended to the end of the file.
	 *
	 * @param file  the properties file to be updated
	 * @param key   the key to be overwritten or added
	 * @param value the (new) value for the key
	 * @throws IOException if the properties file could not been read or written
	 */
	public static void updatePropertiesFile(File file, String key, String value) throws IOException {
		updatePropertiesFile(file, Collections.singletonMap(key, value));
	}

	/**
	 * Reads and rewrites the a properties file, adding the specified entries, overwriting all existing entries that
	 * have one of the specified keys. It preserves all other lines, including comments and the order of the lines. Only
	 * the lines with the specified key will be modified (preserving their order), where the first one is overwritten,
	 * and succeeding ones (if there are any) will be deleted. If there are no such lines contained for some of the
	 * specified entries, the remaining entries will be appended to the end of the file.
	 * <p>
	 * The method is also capable to delete entries, if the key occurs in the specified entries with value null.
	 *
	 * @param file     the properties file to be updated
	 * @param entries  the keys to be overwritten with their (new) values
	 * @param comments extra comment lines to be added as the topmost lines (prefix '#' will be added automatically)
	 * @throws IOException if the properties file could not been read or written
	 */
	public static void updatePropertiesFile(File file, Map<String, String> entries, String... comments) throws IOException {
		// create well-encoded lines to be added;
		// preserve null to mark lines to be deleted
		Map<String, String> linesToAdd = new HashMap<>();
		for (Entry<String, String> entry : entries.entrySet()) {
			String newLine = null;
			if (entry.getValue() != null) {
				Properties newProperty = new Properties();
				newProperty.put(entry.getKey(), entry.getValue());
				StringWriter newLineBuffer = new StringWriter();
				newProperty.store(newLineBuffer, null);
				newLine = newLineBuffer.toString().replaceAll("(?m)^#.*$[\n\r]*", "").trim();
			}
			linesToAdd.put(entry.getKey(), newLine);
		}

		// first add the comment lines that are specified
		List<String> modifiedLines = new LinkedList<>();
		for (String comment : comments) {
			for (String line : comment.split("[\n\r]+")) {
				modifiedLines.add("#" + line);
			}
		}

		// then go through the file and add all lines, replacing single lines as requested
		if (file.exists()) {
			// read the properties file and iterate each line,
			// preserving comments and order
			List<String> lines = getLines(file);
			ListIterator<String> lineIterator = lines.listIterator();
			while (lineIterator.hasNext()) {
				// read next single line
				String line = lineIterator.next();
				// (to handle multi-line properties well, add lines as long we terminate with a '\')
				while (line.endsWith("\\") && lineIterator.hasNext()) {
					line += "\n" + lineIterator.next();
				}

				// parse each line as a property
				Properties parsedLine = new Properties();
				parsedLine.load(new StringReader(line));

				// if the lines specifies the key, it will be overwritten
				if (!parsedLine.isEmpty()) {
					String key = (String) parsedLine.keySet().iterator().next();
					if (linesToAdd.containsKey(key)) {
						// get the replacing line and overwrite our line with it
						// the get may return null, to mark to delete the line
						line = linesToAdd.get(key);
						linesToAdd.remove(key);
					}
				}

				// finally add the read or replaced line, if not going to be deleted/skipped
				if (line != null) {
					modifiedLines.add(line);
				}
			}

			// we append the new lines at the end, for all not inserted lines to add
			linesToAdd.values().stream().filter(Objects::nonNull).forEach(modifiedLines::add);
		}

		// and finally write the lines back to disc
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(Strings.concat("\n", modifiedLines).getBytes("UTF-8"));
		}
	}

	/**
	 * Returns the lines of the specified file as a list of Strings.
	 *
	 * @param reader the input file to read from
	 * @return the lines of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 15.12.2014
	 */
	public static List<String> getLines(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		List<String> result = new LinkedList<>();
		String line;
		while ((line = br.readLine()) != null) {
			result.add(line);
		}
		return result;
	}

	public static Matrix<String> getCSVCells(File file) throws IOException {
		return getCSVCells(file, ",");
	}

	public static Matrix<String> getCSVCells(File file, String splitSymbol) throws IOException {
		List<String> lines = getLines(file);
		Matrix<String> matrix = new Matrix<>();
		int row = 0;
		for (String line : lines) {
			List<StringFragment> fragments = Strings.splitUnquoted(line, splitSymbol);
			int col = 0;
			for (StringFragment fragment : fragments) {
				String raw = fragment.getContent().trim();
				matrix.set(row, col, Strings.unquote(raw));
				col++;
			}
			row++;
		}
		return matrix;
	}

	/**
	 * Returns the file extension without the leading ".". If the file has no ".", the empty String is returned. if the
	 * file is null, null is returned.
	 *
	 * @param filename the file to get the extension from
	 * @return the extension of the specified file
	 * @created 15.02.2014
	 */
	public static String getExtension(String filename) {
		if (filename == null) return null;
		int index = filename.lastIndexOf('.');
		if (index == -1) return "";
		return filename.substring(index + 1);
	}

	/**
	 * Returns the file extension without the leading ".". If the file has no ".", the empty String is returned. if the
	 * file is null, null is returned.
	 *
	 * @param file the file to get the extension from
	 * @return the extension of the specified file
	 * @created 15.02.2014
	 */
	public static String getExtension(File file) {
		if (file == null) return null;
		return getExtension(file.getName());
	}

	/**
	 * Returns true if the specified file has one of the specified file extensions. The extensions are tested case
	 * insensitive. The specified extension must contain only the characters after the separating ".", not the "."
	 * itself. The characters are compared case insensitive.
	 *
	 * @param fileName   the abstract path of the file to be tested
	 * @param extensions the extensions to be tested for
	 * @return if the file has any of the specified extensions
	 */
	public static boolean hasExtension(String fileName, String... extensions) {
		if (fileName == null) return false;
		if (extensions == null) return false;
		for (String extension : extensions) {
			if (extension == null) continue;
			if (fileName.length() <= extension.length() + 1) continue;
			if (Strings.endsWithIgnoreCase(fileName, extension)
					&& fileName.charAt(fileName.length() - extension.length() - 1) == '.'
					&& !isSeparatorChar(fileName.charAt(fileName.length() - extension.length() - 2))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSeparatorChar(char c) {
		return (c == '/' || c == '\\');
	}

	/**
	 * Returns true if the specified file has one of the specified file extensions. The extensions are tested case
	 * insensitive. The specified extension must contain only the characters after the separating ".", not the "."
	 * itself. The characters are compared case insensitive.
	 *
	 * @param file       the file to be tested
	 * @param extensions the extensions to be tested for
	 * @return if the file has any of the specified extensions
	 * @throws NullPointerException if the array of extensions is null or if any of the contained extension is null
	 */
	public static boolean hasExtension(File file, String... extensions) {
		return file != null && hasExtension(file.getName(), extensions);
	}

	/**
	 * Returns the file path without its extension and without the "." before the extension. If the file has no ".", the
	 * original file path is returned. if the file is null, null is returned.
	 *
	 * @param filename the file to remove the extension from
	 * @return the path of the specified file without the extension
	 * @created 15.02.2014
	 */
	public static String stripExtension(String filename) {
		if (filename == null) return null;
		// files starting with "." are no extensions, so iterate i>0 only
		for (int i = filename.length() - 1; i > 0; i--) {
			char c = filename.charAt(i);
			if (isSeparatorChar(c)) break;
			if (c == '.') {
				if (isSeparatorChar(filename.charAt(i - 1))) break; // we can do this, as i > 0 in the loop
				return filename.substring(0, i);
			}
		}
		return filename;
	}

	/**
	 * Returns the file path without its extension and without the "." before the extension. If the file has no ".", the
	 * original file path is returned. if the file is null, null is returned.
	 *
	 * @param file the file to remove the extension from
	 * @return the path of the specified file without the extension
	 * @created 15.02.2014
	 */
	public static String stripExtension(File file) {
		if (file == null) return null;
		return stripExtension(file.getPath());
	}

	/**
	 * Returns the specified file without its original extension but with the specified new extension. If the file has
	 * no ".", the original file path is appended by the extension. if the file is null, null is returned. If the
	 * extension is null, the original extension is removed.
	 *
	 * @param file      the file to remove the extension from
	 * @param extension the new extension to be used (without a leading ".")
	 * @return the path of the specified file with the replaced the extension
	 * @created 29.11.2017
	 */
	public static File replaceExtension(File file, String extension) {
		if (file == null) return null;
		return new File(replaceExtension(file.getPath(), extension));
	}

	/**
	 * Returns the specified filename without its original extension but with the specified new extension. If the file
	 * has no ".", the original file path is appended by the extension. if the filename is null, null is returned. If
	 * the extension is null, the original extension is removed.
	 *
	 * @param filename  the file to remove the extension from
	 * @param extension the new extension to be used (without a leading ".")
	 * @return the path of the specified file with the replaced the extension
	 * @created 29.11.2017
	 */
	public static String replaceExtension(String filename, String extension) {
		if (filename == null) return null;
		String name = stripExtension(filename);
		return (extension == null) ? name : (name + "." + extension);
	}

	/**
	 * Recursively gets all plain files matching the specified {@link FileFilter}. If no filter is specified, all files
	 * recursively contained in the specified directory are returned. The directories itself are not added to the
	 * returned files. If the specified root file is not a directory, an empty collection is returned (even if the
	 * specified root is a file, it is NOT (!) returned). The filter may be null to get all files found.
	 *
	 * @param root   the root directory
	 * @param filter filters the files
	 * @return all files matching the specified filter in the specified directory (recursively)
	 */
	public static Collection<File> recursiveGet(File root, FileFilter filter) {
		Collection<File> files = new LinkedList<>();
		File[] list = root.listFiles();
		if (list != null) {
			for (File f : list) {
				if (f.isDirectory()) {
					files.addAll(recursiveGet(f, filter));
				}
				else if (filter == null || filter.accept(f)) {
					files.add(f);
				}
			}
		}
		return files;
	}

	/**
	 * Unzips the specified zip file into the specified target folder. The directory structure of the zip file is
	 * reproduced in the directory structure, maybe with some file system specialities (if special characters are not
	 * supported in the file system).
	 *
	 * @param zipFile      the source zip file
	 * @param targetFolder the target folder to unzip into
	 * @throws IOException if the zip could not be read correctly or the target files/folders could not been created
	 */
	public static void unzip(File zipFile, File targetFolder) throws IOException {
		unzip(zipFile, targetFolder, null);
	}

	/**
	 * Unzips the specified zip file into the specified target folder. The directory structure of the zip file is
	 * reproduced in the directory structure, maybe with some file system specialities (if special characters are not
	 * supported in the file system). If a filter is specified, only the files matching the filter will be unzipped (the
	 * filter is given the target file object, before the file content is expanded).
	 *
	 * @param zipFile      the source zip file
	 * @param targetFolder the target folder to unzip into
	 * @param filter       the file filter or the target files
	 * @throws IOException if the zip could not be read correctly or the target files/folders could not been created
	 */
	public static void unzip(File zipFile, File targetFolder, @Nullable FileFilter filter) throws IOException {
		ZipFile zippy = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> all = zippy.entries();
		while (all.hasMoreElements()) {
			ZipEntry next = all.nextElement();
			File file = new File(targetFolder, next.getName());

			// skip all files that are not accepted by the filter
			if (filter != null && !filter.accept(file)) continue;

			// otherwise create folder and extract
			file.getParentFile().mkdirs();
			Streams.streamAndClose(zippy.getInputStream(next), new FileOutputStream(file));
		}
	}
}
