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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests that documents with many character references can be parsed, which exceed the JAXP entity size limits of
 * JDK 25 by default, and tests the child element helpers.
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

	@Test
	public void childElements() throws IOException {
		Document document = XMLUtils.streamToDocument(new ByteArrayInputStream(
				"<root>text<!-- comment --><a/>more text<B/><a>inner<c/></a><?pi data?></root>".getBytes(StandardCharsets.UTF_8)));
		Element root = document.getDocumentElement();
		Assert.assertEquals(List.of("a", "B", "a"), names(XMLUtils.getChildren(root)));
		Assert.assertEquals(List.of("a", "B", "a"), names(XMLUtils.getElementList(root.getChildNodes())));
		Assert.assertEquals(List.of("a", "a"), names(XMLUtils.getChildren(root, "a")));
		Assert.assertEquals(List.of("a", "B", "a"), names(XMLUtils.getElementList(root.getChildNodes(), "b", "A")));
		Assert.assertEquals("a", XMLUtils.getFirstChild(root).getTagName());
		Assert.assertNull(XMLUtils.getFirstChild(XMLUtils.getFirstChild(root)));
		Assert.assertNull(XMLUtils.getFirstChild(null));
		// node lists that are not the child list of a node
		NodeList deep = root.getElementsByTagName("*");
		Assert.assertEquals(List.of("a", "B", "a", "c"), names(XMLUtils.getElementList(deep)));
		Assert.assertEquals(List.of("c"), names(XMLUtils.getElementList(deep, "C")));
	}

	/**
	 * The child element helpers must not corrupt the pool of NodeList index caches of the JDK's Xerces: afterwards,
	 * nested indexed traversals of the document, as Saxon's DOMSender does when serializing it, took quadratic time
	 * in the number of children (e.g. 10 seconds to save the mminfo.xml of a large knowledge base).
	 */
	@Test
	public void indexedTraversalStaysLinear() throws IOException {
		Document untouched = createWideDocument(false);
		Document touched = createWideDocument(true);
		// warm up, then measure
		walkIndexed(untouched);
		walkIndexed(touched);
		long start = System.nanoTime();
		int expected = walkIndexed(untouched);
		long untouchedNanos = System.nanoTime() - start;
		start = System.nanoTime();
		int actual = walkIndexed(touched);
		long touchedNanos = System.nanoTime() - start;
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("indexed traversal took " + touchedNanos / 1_000_000 + " ms instead of "
						+ untouchedNanos / 1_000_000 + " ms",
				touchedNanos < 10 * untouchedNanos + 200_000_000L);
	}

	private static Document createWideDocument(boolean useHelpers) throws IOException {
		Document document = XMLUtils.createEmptyDocument();
		Element root = document.createElement("root");
		document.appendChild(root);
		for (int i = 0; i < 40_000; i++) {
			Element child = document.createElement("child");
			child.appendChild(document.createElement("a"));
			child.appendChild(document.createElement("b"));
			if (useHelpers) {
				Assert.assertEquals(2, XMLUtils.getChildren(child).size());
				Assert.assertEquals("a", XMLUtils.getFirstChild(child).getTagName());
			}
			root.appendChild(child);
		}
		return document;
	}

	/**
	 * Visits all nodes depth first by NodeList index, as Saxon's DOMSender does.
	 */
	private static int walkIndexed(Node node) {
		int count = 1;
		NodeList children = node.getChildNodes();
		int length = children.getLength();
		for (int i = 0; i < length; i++) {
			count += walkIndexed(children.item(i));
		}
		return count;
	}

	private static List<String> names(List<Element> elements) {
		return elements.stream().map(Element::getTagName).toList();
	}
}
