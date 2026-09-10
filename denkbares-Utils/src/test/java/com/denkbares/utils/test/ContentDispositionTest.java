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
package com.denkbares.utils.test;

import java.nio.file.Path;

import org.junit.Test;

import com.denkbares.utils.ContentDisposition;
import com.denkbares.utils.Files;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ContentDisposition}, which has to defend against header injection, because download file names are
 * usually taken from request parameters.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 10.09.2026
 */
public class ContentDispositionTest {

	@Test
	public void plainNames() {
		assertEquals("attachment; filename=\"Ontology.nt\"", ContentDisposition.attachment("Ontology.nt"));
		assertEquals("inline; filename=\"report.html\"", ContentDisposition.inline("report.html"));
		// readability of the name is preserved
		assertEquals("attachment; filename=\"My Ontology (v2).nt\"",
				ContentDisposition.attachment("My Ontology (v2).nt"));
	}

	@Test
	public void fileNamesAreTakenFromPathsWithoutDirectories() {
		assertEquals("attachment; filename=\"Ontology.nt\"",
				ContentDisposition.attachment(Path.of("/tmp/exports/Ontology.nt")));
		assertEquals("inline; filename=\"download\"", ContentDisposition.inline((Path) null));
	}

	@Test
	public void missingNames() {
		assertEquals("attachment; filename=\"download\"", ContentDisposition.attachment((String) null));
		assertEquals("attachment; filename=\"download\"", ContentDisposition.attachment(""));
		assertEquals("attachment; filename=\"download\"", ContentDisposition.attachment("  "));
	}

	@Test
	public void quotesCannotEscapeTheQuotedString() {
		// without sanitizing, this would add a filename* parameter taking precedence over the actual name
		String header = ContentDisposition.attachment("a.nt\"; filename*=UTF-8''evil.html");
		assertEquals("attachment; filename=\"a.nt'_ filename_=UTF-8''evil.html\"", header);
		// exactly the opening and the closing quote of the filename parameter
		assertEquals(2, header.chars().filter(c -> c == '"').count());
	}

	@Test
	public void controlCharactersAreRemoved() {
		assertEquals("attachment; filename=\"a.nt X-Injected_ yes\"",
				ContentDisposition.attachment("a.nt\r\nX-Injected: yes"));
	}

	@Test
	public void pathSeparatorsAreRemoved() {
		assertEquals("attachment; filename=\".._.._etc_passwd\"",
				ContentDisposition.attachment("../../etc/passwd"));
	}

	@Test
	public void nonAsciiNamesGetAnAdditionalEncodedParameter() {
		assertEquals("attachment; filename=\"Fahrzeugkl_rung.nt\"; filename*=UTF-8''Fahrzeugkl%C3%A4rung.nt",
				ContentDisposition.attachment("Fahrzeugklärung.nt"));
	}

	@Test
	public void cleanFileNameStillBehavesAsBefore() {
		// cleanFileName now delegates to Strings#encodeFileName, which must not change its established results
		assertEquals("2026.6-SNAPSHOT", Files.cleanFileName("2026.6-SNAPSHOT"));
		assertEquals("safe-mode-20260910-080000-2026.6-SNAPSHOT-",
				Files.cleanFileName("safe-mode-20260910-080000-2026.6-SNAPSHOT-"));
		assertEquals("Some_Log_Name", Files.cleanFileName("Some Log Name"));
		assertEquals("null", Files.cleanFileName(null));
		// as before, a name of unusable characters does not collapse to an empty path element
		assertEquals("", Files.cleanFileName(""));
		assertEquals("_", Files.cleanFileName("   "));
		// but control characters are removed now as well
		assertEquals("a_b", Files.cleanFileName("a\u0001b"));
	}
}
