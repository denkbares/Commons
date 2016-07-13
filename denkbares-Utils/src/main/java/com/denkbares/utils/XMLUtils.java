package com.denkbares.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * XML-related utilities
 *
 * @author Alex Legler (denkbares GmbH)
 * @created 2016-05-31
 */
public class XMLUtils {
	/**
	 * Creates a new {@link XPath} instance using the default {@link XPathFactory} and sets a matching
	 * {@link NamespaceContext} that resolves namespaces
	 *
	 * @param namespaceMap Map of identifier -> URI namespaces to use
	 * @return {@link XPath} aware of given namespaces
	 */
	public static XPath createNamespaceAwareXPath(Map<String, String> namespaceMap) {
		final XPathFactory factory = XPathFactory.newInstance();
		final XPath xpath = factory.newXPath();
		final NamespaceContext namespaceContext = new SimpleNamespaceContext(namespaceMap);
		xpath.setNamespaceContext(namespaceContext);

		return xpath;
	}

	private static class SimpleNamespaceContext implements NamespaceContext {
		private final Map<String, String> PREF_MAP = new HashMap<>();

		SimpleNamespaceContext(final Map<String, String> prefMap) {
			PREF_MAP.putAll(prefMap);
		}

		@Override
		public String getNamespaceURI(String prefix) {
			return PREF_MAP.get(prefix);
		}

		@Override
		public String getPrefix(String uri) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator getPrefixes(String uri) {
			throw new UnsupportedOperationException();
		}
	}
}
