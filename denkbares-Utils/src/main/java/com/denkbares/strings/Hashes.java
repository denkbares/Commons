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

package com.denkbares.strings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.denkbares.util.nio.PathFilter;
import com.denkbares.util.nio.Paths;

/**
 * Util class for creating various hashes from String or byte[] source data.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.11.2018
 */
public class Hashes {

	/**
	 * Creates a MD5 hash from the specified source data. Note that md5 ist not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value
	 */
	public static byte[] md5(byte[] source) {
		var digest = createMD5();
		return digest.digest(source);
	}

	/**
	 * Creates a MD5 hash from the specified source data. Note that md5 ist not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value
	 */
	public static byte[] md5(String source) {
		return md5(source.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Creates a MD5 hash from the specified source data, and returns the result as a formatted (block-wise) hex-string.
	 * Note that md5 ist not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value as a block-wise string
	 */
	public static String md5String(String source) {
		return toHexString(md5(source));
	}

	/**
	 * Creates a SHA hash from the specified source data. It tries to use SHA-256, if not available it falls back to
	 * SHA-1. Note that SHA-1 is not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the sha hash value
	 */
	public static byte[] sha(byte[] source) {
		var digest = createSHA();
		return digest.digest(source);
	}

	/**
	 * Creates a SHA hash from the specified source data. It tries to use SHA-256, if not available it falls back to
	 * SHA-1. Note that SHA-1 is not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the sha hash value
	 */
	public static byte[] sha(String source) {
		return sha(source.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Creates a SHA hash from the specified source data, and returns the result as a formatted (block-wise) hex-string.
	 * It tries to use SHA-256, if not available it falls back to SHA-1. Note that SHA-1 is not secure for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the sha hash value as a block-wise string
	 */
	public static String shaString(String source) {
		return toHexString(sha(source));
	}

	/**
	 * Creates a SHA hash from the specified directory subtree. It tries to use SHA-256, if not available it falls back
	 * to SHA-1. Note that SHA-1 is not secure for cryptography. It only considers the files that are no system files
	 * and no hidden files, as specified in {@link Paths#isHidden(Path)}.
	 *
	 * @param root the root path of all files to create the hash for
	 * @return the sha hash value
	 */
	public static byte[] sha(Path root) throws IOException {
		PathFilter filter = path -> !Paths.isHidden(path);
		return sha(root, filter);
	}

	/**
	 * Creates a SHA hash from the specified directory subtree. It tries to use SHA-256, if not available it falls back
	 * to SHA-1. Note that SHA-1 is not secure for cryptography. It only considers the files that are no system files
	 * and no hidden files, as specified in {@link Paths#isHidden(Path)}.
	 *
	 * @param root the root path of all files to create the hash for
	 * @return the sha hash value as a block-wise string
	 */
	public static String shaString(Path root) throws IOException {
		return toHexString(sha(root));
	}

	/**
	 * Creates a SHA hash from the specified directory subtree. It tries to use SHA-256, if not available it falls back
	 * to SHA-1. Note that SHA-1 is not secure for cryptography. It only considers the files that passes the specified
	 * path filter predicate.
	 *
	 * @param root the root path of all files to create the hash for
	 * @param included predicate to return true, for all files and directories that should be included
	 * @return the sha hash value
	 */
	public static byte[] sha(Path root, PathFilter included) throws IOException {
		return sha(root, included, true);
	}

	/**
	 * Creates a SHA hash from the specified directory subtree. It tries to use SHA-256, if not available it falls back
	 * to SHA-1. Note that SHA-1 is not secure for cryptography. It only considers the files that passes the specified
	 * path filter predicate.
	 *
	 * @param root the root path of all files to create the hash for
	 * @param included predicate to return true, for all files and directories that should be included
	 * @param hashContent true, if the file contents should be added to the hash,
	 *                           false if only file metadata (length and last modified date) will be considered
	 * @return the sha hash value
	 */
	public static byte[] sha(Path root, PathFilter included, boolean hashContent) throws IOException {
		// create deterministic and platform independent order of all files
		List<Path> files = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			@NotNull
			public FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
				if (Objects.equals(dir, root)) return FileVisitResult.CONTINUE;
				return included.accept(dir) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			@NotNull
			public FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
				if (attrs.isRegularFile() && included.accept(file)) {
					files.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		files.sort(Comparator.comparing(p -> toRelativePath(root, p)));

		// create message digest for all files
		MessageDigest md = createSHA();
		for (Path file : files) {
			// (Relative) Path: size + text
			String rel = toRelativePath(root, file);
			byte[] relBytes = rel.getBytes(StandardCharsets.UTF_8);
			md.update(intToBytes(relBytes.length));
			md.update(relBytes);

			// File (content or metadata)
			if (hashContent) {
				// read the file an stream to message digest
				byte[] buf = new byte[8192];
				try (InputStream in = Files.newInputStream(file);
					 DigestInputStream dis = new DigestInputStream(in, md)) {
					//noinspection StatementWithEmptyBody
					while (dis.read(buf) != -1) { /* no-op */ }
				}
			}
			else {
				// add file metadata to message digest
				md.update(longToBytes(Files.size(file)));
				md.update(longToBytes(Files.getLastModifiedTime(file).toMillis()));
			}
		}

		// and done
		return md.digest();
	}

	/**
	 * Creates a SHA hash from the specified directory subtree. It tries to use SHA-256, if not available it falls back
	 * to SHA-1. Note that SHA-1 is not secure for cryptography. It only considers the files that passes the specified
	 * path filter predicate.
	 *
	 * @param root the root path of all files to create the hash for
	 * @param included predicate to return true, for all files and directories that should be included
	 * @return the sha hash value as a block-wise string
	 */
	public static String shaString(Path root, PathFilter included) throws IOException {
		return toHexString(sha(root, included));
	}

	/**
	 * Creates a hex string out of the digest information.
	 *
	 * @param digest the digest to be printed as hex string
	 * @return the hex-formatted digest
	 */
	@NotNull
	public static String toHexString(byte[] digest) {
		// create blocks of 4-byte values
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			if (i > 0 && i % 4 == 0) result.append('-');
			result.append(String.format("%02X", 0xFF & digest[i]));
		}
		return result.toString();
	}

	/**
	 * Ensures that the specified digest has the maximum specified length. If the digest does not exceed the maximum
	 * length, the unmodified will be returned. Otherwise, a shortened version of the digest is returned.
	 *
	 * @param digest the original digest to be truncated (if required)
	 * @param maxLength the maximum length of the truncated digest
	 * @return the digest with the specified maximum length
	 */
	public static byte[] truncate(byte[] digest, int maxLength) {
		if (maxLength < 0) return digest;
		if (digest.length <= maxLength) return digest;
		return Arrays.copyOf(digest, maxLength);
	}

	private static byte[] intToBytes(int v) {
		return new byte[] {
				(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v
		};
	}

	private static byte[] longToBytes(long v) {
		return new byte[] {
				(byte) (v >>> 56), (byte) (v >>> 48), (byte) (v >>> 40), (byte) (v >>> 32),
				(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v
		};
	}

	@NotNull
	private static String toRelativePath(Path root, Path file) {
		return root.relativize(file).toString().replace(root.getFileSystem().getSeparator(), "/");
	}

	@NotNull
	private static MessageDigest createMD5() {
		try {
			return MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 missing", e);
		}
	}

	@NotNull
	private static MessageDigest createSHA() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException ignore) {
			try {
				return MessageDigest.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("SHA-1 missing", e);
			}
		}
	}
}
