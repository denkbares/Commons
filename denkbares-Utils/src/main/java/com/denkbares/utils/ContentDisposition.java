/*
 * Copyright (C) 2026 denkbares GmbH, Germany
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
package com.denkbares.utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.strings.Strings;

/**
 * Creates values for the HTTP <tt>Content-Disposition</tt> response header as specified by RFC 6266.
 * <p>
 * Always use this class instead of concatenating the header value yourself. Download file names usually originate
 * from user input (a request parameter, a wiki page name, an attachment name); a quote in such a name terminates the
 * quoted string of the <tt>filename</tt> parameter early and allows to inject arbitrary further header parameters,
 * e.g. a <tt>filename*</tt> that takes precedence over the actual name and lets an attacker choose the name and
 * extension a victim is offered to save.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 10.09.2026
 */
public class ContentDisposition {

	/**
	 * Fallback used if the given file name is empty or consists of unusable characters only.
	 */
	public static final String DEFAULT_FILE_NAME = "download";

	private static final Pattern NON_HEADER_CHARS = Pattern.compile("[^\\x20-\\x7E]");

	/**
	 * Characters that may appear unencoded in the extended <tt>filename*</tt> parameter, see RFC 5987 attr-char.
	 */
	private static final String ATTR_CHARS = "!#$&+-.^_`|~";

	private ContentDisposition() {
	}

	/**
	 * Creates the header value offering the content as a file download, e.g. <tt>attachment;
	 * filename="Ontology.nt"</tt>.
	 *
	 * @param filename the file name to be suggested to the client
	 * @return the header value
	 */
	@NotNull
	public static String attachment(@Nullable String filename) {
		return of("attachment", filename);
	}

	/**
	 * Creates the header value displaying the content inside the browser, e.g. <tt>inline;
	 * filename="Report.html"</tt>.
	 *
	 * @param filename the file name to be suggested to the client
	 * @return the header value
	 */
	@NotNull
	public static String inline(@Nullable String filename) {
		return of("inline", filename);
	}

	/**
	 * Creates the header value offering the content as a file download, using the file name of the given file, e.g.
	 * <tt>attachment; filename="Ontology.nt"</tt> for <tt>/tmp/exports/Ontology.nt</tt>. Only the file name of the
	 * path is used, never any of its directories.
	 *
	 * @param file the file whose name is to be suggested to the client
	 * @return the header value
	 */
	@NotNull
	public static String attachment(@Nullable Path file) {
		return of("attachment", file);
	}

	/**
	 * Creates the header value displaying the content of the given file inside the browser, e.g. <tt>inline;
	 * filename="Report.html"</tt> for <tt>/tmp/exports/Report.html</tt>. Only the file name of the path is used,
	 * never any of its directories.
	 *
	 * @param file the file whose name is to be suggested to the client
	 * @return the header value
	 */
	@NotNull
	public static String inline(@Nullable Path file) {
		return of("inline", file);
	}

	/**
	 * Creates the header value for the given disposition type, using the file name of the given file. Only the file
	 * name of the path is used, never any of its directories.
	 *
	 * @param type the disposition type, usually <tt>attachment</tt> or <tt>inline</tt>
	 * @param file the file whose name is to be suggested to the client
	 * @return the header value
	 * @see #of(String, String)
	 */
	@NotNull
	public static String of(@NotNull String type, @Nullable Path file) {
		Path name = file == null ? null : file.getFileName();
		return of(type, name == null ? null : name.toString());
	}

	/**
	 * Creates the header value for the given disposition type and file name. The file name is sanitized by {@link
	 * Strings#encodeFileName(String)} and rendered as a plain ASCII <tt>filename</tt> parameter. If it contains
	 * characters that cannot be represented in a header value, an additional <tt>filename*</tt> parameter with the
	 * UTF-8 encoded name is appended, as specified by RFC 6266 and RFC 5987.
	 *
	 * @param type     the disposition type, usually <tt>attachment</tt> or <tt>inline</tt>
	 * @param filename the file name to be suggested to the client
	 * @return the header value
	 */
	@NotNull
	public static String of(@NotNull String type, @Nullable String filename) {
		// removes control characters, path separators and quotes, but keeps non-ascii characters
		String safeName = Strings.encodeFileName(filename);
		if (Strings.isBlank(safeName)) safeName = DEFAULT_FILE_NAME;
		String headerName = NON_HEADER_CHARS.matcher(safeName).replaceAll("_").trim();
		if (headerName.isEmpty()) headerName = DEFAULT_FILE_NAME;
		StringBuilder result = new StringBuilder(type).append("; filename=\"").append(headerName).append('"');
		if (!headerName.equals(safeName)) {
			// header values are latin-1 only, so additionally provide the original name utf-8 encoded
			result.append("; filename*=UTF-8''").append(encodeAttrChars(safeName));
		}
		return result.toString();
	}

	@NotNull
	private static String encodeAttrChars(@NotNull String text) {
		StringBuilder result = new StringBuilder();
		for (byte encoded : text.getBytes(StandardCharsets.UTF_8)) {
			int value = encoded & 0xFF;
			char c = (char) value;
			if (value < 128 && (Character.isLetterOrDigit(c) || ATTR_CHARS.indexOf(c) >= 0)) {
				result.append(c);
			}
			else {
				result.append('%').append(String.format("%02X", value));
			}
		}
		return result.toString();
	}
}
