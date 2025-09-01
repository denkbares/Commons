/*
 * Copyright (C) 2025 denkbares GmbH, Germany
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

package com.denkbares.util.nio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denkbares.collections.Iterators;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Predicates;
import com.denkbares.utils.Streams;

import static java.nio.file.StandardCopyOption.*;

/**
 * Class for common utils methods to deal with java non-blocking io.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 10.03.2015
 */
public class Paths {
	private static final Logger LOGGER = LoggerFactory.getLogger(Paths.class);

	public static final String FILE_PREFIX_CONTENT_HASH = ".com.denkbares.contentHash.";

	private static final String MANIFEST_CONTENT_HASH = "contentHash";
	private static final String MANIFEST_LAST_MODIFIED = "lastModified";

	/**
	 * Return a canonical {@link Path} like {@link File#getCanonicalPath()}. The nio equivalent
	 * {@link Path#toRealPath(LinkOption...)} checks exist of file, which is not always wanted.
	 *
	 * @param path to create canonical path
	 * @return the absolute and normalized path
	 */
	public static Path getCanonicalPath(Path path) {
		return path.toAbsolutePath().normalize();
	}

	/**
	 * Returns the contents of the specified file as a String.
	 *
	 * @param file the input file to read from
	 * @return the content of the file
	 * @throws IOException if the specified file cannot be read completely
	 * @created 01.10.2013
	 */
	public static String getText(Path file) throws IOException {
		try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
			return Streams.getText(in);
		}
	}

	/**
	 * Checks if two files have the same content. Returns true if both files exists, both denote a file (not a
	 * directory), and the bytes of each file are identical.
	 *
	 * @param file1 the first file to compare
	 * @param file2 the second file to compare
	 * @return if both files have the same content
	 */
	public static boolean hasEqualContent(Path file1, Path file2) throws IOException {
		if (!Files.isRegularFile(file1)) return false;
		if (!Files.isRegularFile(file2)) return false;

		if (Files.isSameFile(file1, file2)) return true;
		if (Files.size(file1) != Files.size(file2)) return false;

		try (InputStream in1 = Files.newInputStream(file1);
			 InputStream in2 = Files.newInputStream(file2)) {
			return Streams.hasEqualContent(in1, in2, 8192);
		}
	}

	/**
	 * Returns the content hash of the contents of the specified file. The method will read the file and create a hash
	 * code of the contents. If the content has already been calculated (and persisted) the method will only read the
	 * persisted value if it is still up-to-date. If allowed, the method will also persist the content hash.
	 *
	 * @param file    the file to get the content hash for
	 * @param persist flag that indicates if the method is allowed to persist the content hash for future use
	 * @return the content hash
	 * @throws java.io.IOException if the contents of the file cannot be read
	 */
	public static long getContentHash(Path file, boolean persist) throws IOException {
		// check for existing manifest with valid content hash
		Path manifestFile = file.resolveSibling(String.format("%s%s.dat",
				FILE_PREFIX_CONTENT_HASH, file.getFileName()));
		Properties manifest = readManifest(manifestFile);
		String contentHash = manifest.getProperty(MANIFEST_CONTENT_HASH);
		String lastModified = manifest.getProperty(MANIFEST_LAST_MODIFIED);
		if (contentHash != null && lastModified != null) {
			if (Long.parseLong(lastModified) == Files.getLastModifiedTime(file).toMillis()) {
				return Long.parseLong(contentHash);
			}
		}
		// otherwise read the file content to build the hash for
		long hash = Files.isDirectory(file) ? createDirectoryHash(file, persist) : createFileHash(file);
		// if persist is active update manifest to disc
		if (persist) {
			manifest.setProperty(MANIFEST_CONTENT_HASH, String.valueOf(hash));
			manifest.setProperty(MANIFEST_LAST_MODIFIED,
					String.valueOf(Files.getLastModifiedTime(file).toMillis()));
			writeManifest(manifestFile, manifest);
		}
		return hash;
	}

	private static long createDirectoryHash(Path folder, boolean persist) throws IOException {
		CRC32 crc32 = new CRC32();
		List<Path> files;
		try (Stream<Path> fileStream = Files.list(folder)) {
			files = fileStream.sorted().toList();
		}
		for (Path file : files) {
			// we skip all hidden files, so avoid having changes,
			// because the content hash has been persisted
			if (file.getFileName().toString().startsWith(".")) continue;
			long hash = getContentHash(file, persist);
			for (int i = 7; i >= 0; i--) {
				crc32.update((int) (hash & 0xFF));
				hash >>= 8;
			}
		}
		return crc32.getValue();
	}

	private static long createFileHash(Path file) throws IOException {
		long hash;
		try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
			CRC32 crc32 = new CRC32();
			int data;
			while ((data = inputStream.read()) != -1) {
				crc32.update(data);
			}
			hash = crc32.getValue();
		}
		return hash;
	}

	private static Properties readManifest(Path manifestPath) throws IOException {
		Properties manifest = new Properties();
		if (Files.exists(manifestPath)) {
			try (Reader reader = Files.newBufferedReader(manifestPath)) {
				manifest.load(reader);
			}
		}
		return manifest;
	}

	private static void writeManifest(Path manifestPath, Properties manifest) throws IOException {
		try (Writer writer = Files.newBufferedWriter(manifestPath)) {
			manifest.store(writer, "manifest created at " + new Date());
		}
	}

	/**
	 * Deletes a specified file or folder subtree from the file system. If the specified file does not exists, nothing
	 * is done. If the method exists normally, the file or folder does no longer exists. Otherwise an IOException is
	 * thrown.
	 *
	 * @param fileOrFolder the file or folder to be deleted
	 * @throws IOException if any file cannot be deleted
	 */
	public static void deleteTree(Path fileOrFolder) throws IOException {
		// ignore if already be deleted
		if (!Files.exists(fileOrFolder, LinkOption.NOFOLLOW_LINKS)) return;

		// delete regular files
		if (!Files.isDirectory(fileOrFolder, LinkOption.NOFOLLOW_LINKS)) {
			Files.delete(fileOrFolder);
			return;
		}

		// delete recursively
		Files.walkFileTree(fileOrFolder, new SimpleFileVisitor<>() {
			@Override
			@NotNull
			public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			@NotNull
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
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
		return com.denkbares.utils.Files.getExtension(filename);
	}

	/**
	 * Returns the file extension without the leading ".". If the file has no ".", the empty String is returned. if the
	 * file is null, null is returned.
	 *
	 * @param file the file to get the extension from
	 * @return the extension of the specified file
	 * @created 15.02.2014
	 */
	public static String getExtension(Path file) {
		if (file == null) return null;
		return getExtension(file.getFileName().toString());
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
		return com.denkbares.utils.Files.hasExtension(fileName, extensions);
	}

	/**
	 * Returns true if the specified file has one of the specified file extensions. The extensions are tested case
	 * insensitive. The specified extension must contain only the characters after the separating ".", not the "."
	 * itself. The characters are compared case insensitive.
	 *
	 * @param file       the file to be tested
	 * @param extensions the extensions to be tested for
	 * @return if the file has any of the specified extensions
	 */
	public static boolean hasExtension(Path file, String... extensions) {
		return (file != null) && hasExtension(file.getFileName().toString(), extensions);
	}

	/**
	 * Returns the file path without its extension and without the "." before the extension. If the file has no ".", the
	 * original file path is returned. if the file is null, null is returned.
	 *
	 * @param filename the file to remove the extension from
	 * @return the path of the specified file without the extension
	 * @created 03.06.2015
	 */
	public static String stripExtension(String filename) {
		return com.denkbares.utils.Files.stripExtension(filename);
	}

	/**
	 * Returns the file path without its extension and without the "." before the extension. If the file has no ".", the
	 * original file path is returned. if the file is null, null is returned.
	 *
	 * @param file the file to remove the extension from
	 * @return the path of the specified file without the extension
	 * @created 03.06.2015
	 */
	public static Path stripExtension(Path file) {
		return replaceExtension(file, null);
	}

	/**
	 * Returns the file path without its original extension but with the specified new extension. If the file has no
	 * ".", the original file path is appended by the extension. if the file is null, null is returned. If the extension
	 * is null, the original extension is removed.
	 *
	 * @param file      the file to remove the extension from
	 * @param extension the new extension to be used (without a leading ".")
	 * @return the path of the specified file with the replaced the extension
	 * @created 03.06.2015
	 */
	public static Path replaceExtension(Path file, String extension) {
		if (file == null) return null;
		if (file.getNameCount() == 0) return file;
		String name = stripExtension(file.getFileName().toString());
		if (extension != null) name += "." + extension;
		return file.getParent().resolve(name);
	}

	/**
	 * Returns true if a specified file or folder is suspected to be hidden. In contrast to Files#isHidden, the
	 * implementation additionally using a platform-independent heuristic to determine hidden files. This is necessary
	 * when copying folders among different operating systems, also copying the hidden files of the host system to the
	 * target system, or when using a alternative file system, e.g. for zipped folders.
	 * <p>
	 * The path is hidden if the Files#isHidden is true or if it is a "__MACOSX" folder or if the file starts with a "."
	 * in its name.
	 *
	 * @param path the path to examine to be hidden
	 * @return true if the specified path appears to be hidden
	 */
	public static boolean isHidden(Path path) throws IOException {
		//noinspection SimplifiableIfStatement
		if (path.getNameCount() == 0) return false;
		return path.endsWith("__MACOSX")
				|| path.getFileName().toString().startsWith(".")
				|| Files.isHidden(path);
	}

	/**
	 * Returns true if the specified file exists and the specified path matches the real notation of the existing file.
	 * For case sensitive file systems this is identical to simply check the file for existence. Using this method you
	 * can validate that the file at the specified path will be found on any file system, even when migrate to an other
	 * host system (with case sensitive file system).
	 *
	 * @param path the path to check for existence
	 * @return true if the file exists, considering the case of the path
	 * @throws IOException if accessing the file fails
	 */
	public static boolean existsCaseSensitive(Path path) throws IOException {
		if (!Files.exists(path)) return false;
		Path realPath = path.toRealPath();
		int realCount = realPath.getNameCount();
		int pathCount = path.getNameCount();
		for (int i = 0; i < pathCount; i++) {
			String realName = realPath.getName(realCount - pathCount + i).toString();
			String pathName = path.getName(i).toString();
			if (!Strings.equals(pathName, realName)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Copies the entire source directory structure (or a simple file) to the target file. Existing files will be
	 * overwritten. Missing directories will be created. If there is already a directory structure at the specified
	 * target, the copied one will be integrated into the existing one without deleting any files or directories (except
	 * overwriting existing files with the copied ones if they conflict).
	 *
	 * @param sourcePath the source path to copy from
	 * @param targetPath the target path to copy to
	 * @throws IOException if any of the files cannot be copied or any directory could not been created
	 */
	public static void copyTree(Path sourcePath, Path targetPath) throws IOException {
		Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
			@Override
			@NotNull
			public FileVisitResult preVisitDirectory(final Path dir,
													 final @NotNull BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(crossResolve(targetPath, sourcePath.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			@NotNull
			public FileVisitResult visitFile(final Path file,
											 final @NotNull BasicFileAttributes attrs) throws IOException {
				Files.copy(file, crossResolve(targetPath, sourcePath.relativize(file)), REPLACE_EXISTING, COPY_ATTRIBUTES);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Similar to Path.resolve, but allows resolving across file systems.
	 */
	private static Path crossResolve(Path self, Path relative) {
		if (relative.isAbsolute()) {
			self = self.getRoot();
		}
		for (Path item : relative) {
			String name = item.toString();
			if (name.isEmpty()) continue;
			self = self.resolve(name);
		}
		return self;
	}

	/**
	 * Creates a list of all files in a directory. The listing is similar to "ls -al", but does not contain the parent
	 * directory ("../") in the list. The returned list is sorted alphabetically.
	 *
	 * @param directory the directory to create the listing for
	 * @return the directory contents
	 * @throws IOException if the tree could not been traversed correctly
	 */
	public static List<String> listFiles(Path directory) throws IOException {
		return listFiles(directory, Predicates.TRUE());
	}

	/**
	 * Creates a list of all files in a directory, that matches the specified filter predicate. Other files are ignored.
	 * The listing is similar to "ls -al", but does not contain the parent directory ("../") in the list. The returned
	 * list is sorted alphabetically.
	 *
	 * @param directory the directory to create the listing for
	 * @param filter    a filter to filter for the entries to be displayed
	 * @return the directory contents
	 * @throws IOException if the tree could not been traversed correctly
	 */
	public static List<String> listFiles(Path directory, Predicate<Path> filter) throws IOException {
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(directory)) {
			return Iterators.stream(dir)
					.filter(filter)
					.sorted(Comparator.comparing(Path::toString))
					.map(child -> listSingleEntry(directory, child))
					.collect(Collectors.toList());
		}
	}

	/**
	 * Creates a list of all files of a directory tree, starting with the specified root path. The listing is similar to
	 * "ls -al", but does not contain the parent directory ("../") in the list. Each entry contains the relative path to
	 * the specified root path. The returned list is sorted alphabetically to the relative path, so the tree is listed
	 * depth first.
	 *
	 * @param root the root path to create the listing for
	 * @return the whole directory tree
	 * @throws IOException if the tree could not been traversed correctly
	 */
	public static List<String> listTree(Path root) throws IOException {
		return listTree(root, Predicates.TRUE());
	}

	/**
	 * Creates a list of all files of a directory tree, starting with the specified root path. The listing is similar to
	 * "ls -al", but does not contain the parent directory ("../") in the list. Each entry contains the relative path to
	 * the specified root path. The returned list is sorted alphabetically to the relative path, so the tree is listed
	 * depth first.
	 *
	 * @param root the root path to create the listing for
	 * @param filter    a filter to filter for the entries to be displayed
	 * @return the whole directory tree
	 * @throws IOException if the tree could not been traversed correctly
	 */
	public static List<String> listTree(Path root, Predicate<Path> filter) throws IOException {
		try (var walk = Files.walk(root)) {
			return walk
					.filter(filter)
					.sorted(Comparator.comparing(Path::toString))
					.map(successor -> listSingleEntry(root, successor))
					.collect(Collectors.toList());
		}
	}

	private static String listSingleEntry(Path root, Path entry) {
		String dateStr = "---------- --:--:--";
		String sizeStr = "         --";
		if (Files.isRegularFile(entry)) {
			try {
				Calendar date = new GregorianCalendar();
				date.setTimeInMillis(Files.getLastModifiedTime(entry).toMillis());
				dateStr = String.format("%1$tY-%1$tm-%1$te %1tT", date);
				sizeStr = String.format("% 10dB", Files.size(entry));
			}
			catch (IOException e) {
				LOGGER.error("cannot access date/size of file: {}", entry, e);
				dateStr = "????-??-?? ??:??:??";
				sizeStr = "         ??";
			}
		}

		String relativePath = root.relativize(entry).toString();
		if (relativePath.isEmpty()) relativePath = ".";
		if (Files.isDirectory(entry)) relativePath += '/';
		return String.format("%c%c%c%c %s  %s  %s",
				Files.isDirectory(entry) ? 'd' : '-',
				Files.isReadable(entry) ? 'r' : '-',
				Files.isWritable(entry) ? 'w' : '-',
				Files.isExecutable(entry) ? 'x' : '-',
				sizeStr, dateStr, relativePath);
	}

	/**
	 * Provides a default implementation to create a java nio "path matcher". This method may be useful if you plan to
	 * implement your own {@link java.nio.file.FileSystem}, which requires to implement {@link
	 * java.nio.file.FileSystem#getPathMatcher(String)}. Currently the path matcher syntax for "glob" and "regex" is
	 * supported.
	 *
	 * @param syntaxAndPattern the path matcher string: "syntax:expression"
	 * @return the path matcher
	 */
	public static PathMatcher getPathMatcher(String syntaxAndPattern) {
		if (!syntaxAndPattern.startsWith("glob:") && !syntaxAndPattern.startsWith("regex:")) {
			throw new UnsupportedOperationException("Syntax not recognized: " + syntaxAndPattern);
		}
		return FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
	}

	/**
	 * Creates a new (buffered) input stream for the specified file, that is capable to read an input file, previously
	 * written via an atomic output stream. Using this method, instead of directly creating the file input stream, has
	 * the advantage that any broken transaction will be reverted before.
	 *
	 * @param file the file to read form
	 * @return the (buffered) input stream
	 * @throws IOException if the file could not be read or a detected broken transaction could not been recovered
	 */
	public static InputStream newAtomicInputStream(Path file) throws IOException {
		// recover non-completed transaction
		recover(file);
		return new BufferedInputStream(Files.newInputStream(file));
	}

	/**
	 * Creates a new (buffered) output stream for the specified file. In contrast to normal file stream the actual
	 * writing will be done in a temp file. When closing the stream, the target will be replaced in an almost atomic
	 * transaction. If the transaction fails, it will be possible to detect this and fully recover a valid file state,
	 * either the old file or the written file, depending when the atomic write has failed.
	 *
	 * @param file the file to write in an atomic transaction (when closing the stream)
	 * @return the stream to write to
	 * @throws IOException if the target file could not been opened
	 */
	public static OutputStream newAtomicOutputStream(Path file) throws IOException {
		checkWriteableFile(file);
		// write the new file and commit on close
		return new BufferedOutputStream(Files.newOutputStream(toNew(file))) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
					commit(file);
				}
				finally {
					// also try to recover if something failed before
					recover(file);
				}
			}
		};
	}

	/**
	 * Moves the specified source file to the specified target file. In contrast to normal file stream the actual
	 * moving will be done to a temp file (which may include copying. Before this operation completes, the target
	 * will be replaced in an almost atomic transaction. If the transaction fails, it will be possible to detect
	 * this and fully recover a valid file state, either the old file or the written file, depending on when the
	 * atomic write has failed.
	 * <p>
	 * Note that this operation is only valid on regular files, but NOT (!) directories.
	 *
	 * @param source the source file to move in an atomic transaction
	 * @param target the target file to move the source file to in an atomic transaction
	 * @throws IOException if the file could not been moved
	 */
	public static void moveAtomic(Path source, Path target) throws IOException {
		checkReadableRegularFile(source);
		checkWriteableFile(target);
		try {
			// try to perform a move on the source file
			Files.move(source, toNew(target), REPLACE_EXISTING);
			commit(target);
		}
		finally {
			// also try to recover if something failed before
			recover(target);
		}
	}

	/**
	 * Copies the specified source file to the specified target file. In contrast to normal file stream the actual
	 * moving will be done to a temp file (which may include copying. Before this operation completes, the target
	 * will be replaced in an almost atomic transaction. If the transaction fails, it will be possible to detect
	 * this and fully recover a valid file state, either the old file or the written file, depending on when the
	 * atomic write has failed.
	 * <p>
	 * Note that this operation is only valid on regular files, but NOT (!) directories.
	 *
	 * @param source the source file to copy in an atomic transaction
	 * @param target the target file to copy the source file to in an atomic transaction
	 * @throws IOException if the file could not been copied
	 */
	public static void copyAtomic(Path source, Path target) throws IOException {
		checkReadableRegularFile(source);
		checkWriteableFile(target);
		try {
			// try to perform a copy on the source file
			Files.copy(source, toNew(target), REPLACE_EXISTING);
			commit(target);
		}
		finally {
			// also try to recover if something failed before
			recover(target);
		}
	}

	/**
	 * Checks if the specified file can be overwritten by a regular file.
	 */
	private static void checkWriteableFile(Path file) throws FileAlreadyExistsException, AccessDeniedException {
		// check some common problems for writing that will cause the commit to fail (to fail early)
		if (Files.isDirectory(file)) {
			throw new FileAlreadyExistsException(
					"Cannot create target file, there is already a directory of that path: " + file.toAbsolutePath());
		}
		if (Files.exists(file) && !Files.isWritable(file)) {
			throw new AccessDeniedException("cannot write target file: " + file.toAbsolutePath());
		}
	}

	/**
	 * Checks if the specified path is an existing and regular file, that can be read.
	 */
	private static void checkReadableRegularFile(Path file) throws NoSuchFileException, AccessDeniedException {
		// check some common problems for writing that will cause the commit to fail (to fail early)
		if (!Files.exists(file)) {
			throw new NoSuchFileException("file does not exist: " + file.toAbsolutePath());
		}
		if (!Files.isRegularFile(file)) {
			throw new AccessDeniedException("not a regular file: " + file.toAbsolutePath());
		}
		if (!Files.isReadable(file)) {
			throw new AccessDeniedException("cannot read file: " + file.toAbsolutePath());
		}
	}

	private static Path toNew(Path file) {
		return Paths.replaceExtension(file, "new");
	}

	private static Path toOld(Path file) {
		return Paths.replaceExtension(file, "bak");
	}

	/**
	 * Reverts any non-completely finished atomic write if not completed successfully. You need not to call this method
	 * if you using {@link #newAtomicInputStream(Path)} to access the file.
	 */
	public static void recover(Path file) throws IOException {
		Path oldFile = toOld(file);
		Path newFile = toNew(file);
		boolean exists = Files.exists(file);
		boolean oldExists = Files.exists(oldFile);
		boolean newExists = Files.exists(newFile);

		// if the file exists, the write and move has been completed,
		// so we can simply delete any old file (complete almost completed transaction)
		if (exists && oldExists) {
			Files.delete(oldFile);
			oldExists = false;
		}

		// if the file exists and a new one, the new one is probably not completed, so we delete it
		if (exists && newExists) {
			Files.delete(newFile);
			newExists = false;
		}

		// if the file does not exist, but an old one and a new one
		// the write has been completed (because renaming the file to old is done afterwards)
		// so we can complete the transaction
		if (newExists /* && !exists (always true) */ && oldExists) {
			Files.move(newFile, file, ATOMIC_MOVE);
			Files.delete(oldFile);
		}
	}

	/**
	 * After the new file has been written, replace the existing one by the new file.
	 */
	private static void commit(Path file) throws IOException {
		Path oldFile = toOld(file);
		Path newFile = toNew(file);

		if (!Files.exists(newFile)) {
			throw new IOException("invalid transaction state, new file does not exist: " + newFile);
		}

		// delete old file if previously existed
		if (Files.exists(oldFile)) Files.delete(oldFile);

		// move original file if target is not newly created
		boolean hasPreviousFile = Files.exists(file);
		if (hasPreviousFile) {
			Files.move(file, oldFile, ATOMIC_MOVE);
		}

		// move new file to target and delete old one afterwards (if existed)
		Files.move(newFile, file, ATOMIC_MOVE);
		if (hasPreviousFile) {
			Files.delete(oldFile);
		}
	}
}
