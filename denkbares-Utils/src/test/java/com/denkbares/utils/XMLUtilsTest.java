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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests that documents with many character references can be parsed, which exceed the JAXP entity size limits of
 * JDK 25 by default.
 */
public class XMLUtilsTest {

	private static final int REFERENCES = 150_000;

	private static String createLargeDocument() {
		return "<root><text>" + "a&#10;&amp;".repeat(REFERENCES) + "</text></root>";
	}

	@Test
	public void parseManyCharacterReferencesDOM() throws IOException {
		Document document = XMLUtils.streamToDocument(
				new ByteArrayInputStream(createLargeDocument().getBytes(StandardCharsets.UTF_8)));
		Assert.assertEquals(REFERENCES * 3, document.getDocumentElement().getTextContent().length());
	}

	@Test
	public void parseManyCharacterReferencesSAX() throws IOException, ParserConfigurationException, SAXException {
		int[] length = { 0 };
		XMLUtils.newSAXParser()
				.parse(new ByteArrayInputStream(createLargeDocument().getBytes(StandardCharsets.UTF_8)), new DefaultHandler() {
					@Override
					public void characters(char[] ch, int start, int count) {
						length[0] += count;
					}
				});
		Assert.assertEquals(REFERENCES * 3, length[0]);
	}
}
