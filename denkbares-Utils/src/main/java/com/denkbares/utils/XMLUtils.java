package com.denkbares.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML-related utilities
 *
 * @author Alex Legler (denkbares GmbH)
 * @created 2016-05-31
 */
public class XMLUtils {

	/**
	 * Creates a new {@link XPath} instance using the default {@link XPathFactory} and sets a
	 * matching {@link NamespaceContext} that resolves namespaces
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

	/**
	 * Extracts primitive Values form a string
	 *
	 * @param textContent Sting containing the primitive value
	 * @param clazz       Name of the Class
	 * @return Extracted Value
	 * @throws IOException if the class is not supported
	 */
	public static Object getPrimitiveValue(String textContent, String clazz) throws IOException {
		if (clazz.equals(String.class.getName())) {
			return textContent;
		}
		else if (clazz.equals(Integer.class.getName())) {
			return Integer.parseInt(textContent);
		}
		else if (clazz.equals(Double.class.getName())) {
			return Double.parseDouble(textContent);
		}
		else if (clazz.equals(Boolean.class.getName())) {
			return Boolean.parseBoolean(textContent);
		}
		else if (clazz.equals(URL.class.getName())) {
			return new URL(textContent);
		}
		else if (clazz.equals(Float.class.getName())) {
			return Float.parseFloat(textContent);
		}
		else {
			throw new IOException("Class " + clazz + " is not supported");
		}
	}

	/**
	 * Get the children of a document element as a normal {@link List}, filtered by the
	 * given <tt>tagName</tt>.
	 *
	 * @param element the element to get the children from
	 * @return the children of the given element with the given <tt>tagName</tt> as a normal {@link
	 * List}
	 */
	public static List<Element> getChildren(Element element, String... tagName) {
		return getElementList(element.getChildNodes(), tagName);
	}

	/**
	 * Get the children of a document element as a normal  {@link List}.
	 *
	 * @param element the element to get the children from
	 * @return the children of the given element as a normal {@link List}
	 */
	public static List<Element> getChildren(Element element) {
		return getElementList(element.getChildNodes());
	}

	/**
	 * Get the first child element of a document element. The method returns null if no child
	 * element is available or if the specified element is null.
	 *
	 * @param element the element to get the child element from
	 * @return the first child element of the given element
	 */
	public static Element getFirstChild(Element element) {
		if (element == null) return null;
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i) instanceof Element) {
				return (Element) list.item(i);
			}
		}
		return null;
	}

	/**
	 * Filters all elements of a NodeList and returns them in a collection.
	 *
	 * @param list Nodelist containing all types of nodes (text nodes etc.)
	 * @return a list containing all elements from nodeist, but not containing other nodes such as
	 * text nodes etc.
	 */
	public static List<Element> getElementList(NodeList list) {
		List<Element> col = new ArrayList<>(list.getLength());
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i) instanceof Element) {
				col.add((Element) list.item(i));
			}
		}
		return col;
	}

	/**
	 * Filters all elements of a NodeList and returns them in a collection. The
	 * list will only contain that elements of the NodeList that match the
	 * specified node name. The name selection is case insensitive.
	 *
	 * @param list      Nodelist containing all types of nodes (text nodes etc.)
	 * @param nodeNames the name of the elements to be selected (case insensitive)
	 * @return a list containing all elements from nodelist, but not containing other nodes such as
	 * text nodes etc.
	 */
	public static List<Element> getElementList(NodeList list, String... nodeNames) {
		List<Element> col = new ArrayList<>();
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item instanceof Element) {
				for (String nodeName : nodeNames) {
					if (item.getNodeName().equalsIgnoreCase(nodeName)) {
						col.add((Element) item);
						break;
					}
				}
			}
		}

		return col;
	}

	/**
	 * Creates an XML {@link Document} from the given String.
	 *
	 * @param string the string representing the XML {@link Document}
	 * @return a XML Document representation of the given string
	 */
	public static Document stringToDocument(String string) throws IOException, SAXException, ParserConfigurationException {
		return streamToDocument(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Creates an XML {@link Document} from the given {@link InputStream}.
	 *
	 * @param stream the XML input stream
	 * @return Document the document created from the stream
	 * @throws IOException if the stream cannot be read or does not contains valid XML content or
	 *                     the XML parser cannot be configured
	 */
	public static Document streamToDocument(InputStream stream) throws IOException {
		return streamToDocument(stream, null);
	}

	/**
	 * Creates an XML {@link Document} from the given {@link InputStream}.
	 *
	 * @param stream   the XML input stream
	 * @param resolver is a {@link EntityResolver} to specify how entities given in the {@link
	 *                 Document} should be resolved
	 * @return Document the document created from the stream
	 * @throws IOException if the stream cannot be read or does not contains valid XML content or
	 *                     the XML parser cannot be configured
	 */
	public static Document streamToDocument(InputStream stream, EntityResolver resolver) throws IOException {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser;
		try {
			parser = fac.newDocumentBuilder();
			if (resolver != null) parser.setEntityResolver(resolver);
			return parser.parse(new InputSource(stream));
		}
		catch (ParserConfigurationException | SAXException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Creates an XML {@link Document} from the {@link File}.
	 *
	 * @param file the file to be read
	 * @return Document the document created from the stream
	 * @throws IOException when an error occurs
	 */
	public static Document fileToDocument(File file) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return streamToDocument(in);
		}
	}

	/**
	 * Writes a number of Strings to the specified element with a specified tag
	 * name. Each specified string will create its own element with the
	 * specified tag name.
	 *
	 * @param element the element to add the tag(s) to
	 * @param tagName the name of the tag(s) to be created
	 * @param values  the string values to add
	 * @created 25.01.2014
	 */
	public static void writeStrings(Element element, String tagName, String... values) {
		if (values == null) return;
		for (String value : values) {
			Element node = element.getOwnerDocument().createElement(tagName);
			node.setTextContent(value);
			element.appendChild(node);
		}
	}

	/**
	 * Writes a number of enum values to the specified element with a specified
	 * tag name. Each specified string will create its own element with the
	 * specified tag name.
	 *
	 * @param element the element to add the tag(s) to
	 * @param tagName the name of the tag(s) to be created
	 * @param values  the string values to add
	 * @created 25.01.2014
	 */
	public static void writeEnums(Element element, String tagName, Enum<?>... values) {
		if (values == null) return;
		for (Enum<?> value : values) {
			Element node = element.getOwnerDocument().createElement(tagName);
			node.setTextContent(value.name());
			element.appendChild(node);
		}
	}

	/**
	 * Reads a all elements with the specified tag name that are children of the
	 * specified element and return their text contents as a string array.
	 *
	 * @param element the element to get the tag's text contents for
	 * @param tagName the tag name of the child elements to get the text contents for
	 * @return the text contents of the matched elements
	 * @created 25.01.2014
	 */
	public static String[] readStrings(Element element, String tagName) {
		List<String> result = new ArrayList<>(10);
		List<Element> list = getElementList(element.getChildNodes(), tagName);
		for (Element node : list) {
			result.add(node.getTextContent());
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Reads a all elements with the specified tag name that are children of the
	 * specified element and return their text contents as an array of enum
	 * values of the specified enum class.
	 *
	 * @param element the element to get the tag's text contents for
	 * @param tagName the tag name of the child elements to get the text contents for
	 * @return the text contents of the matched elements
	 * @created 25.01.2014
	 */
	public static <T extends Enum<T>> T[] readEnums(Element element, String tagName, Class<T> clazz) {
		String[] names = readStrings(element, tagName);
		@SuppressWarnings("unchecked")
		T[] values = (T[]) Array.newInstance(clazz, names.length);
		for (int i = 0; i < names.length; i++) {
			values[i] = Enum.valueOf(clazz, names[i]);
		}
		return values;
	}

	/**
	 * Gets the string representation for the given XML element, including tag and attributes.
	 *
	 * @param element the element to get the string representation for
	 * @return the string representation for the given element
	 */
	public static String getStringRepresentation(Element element) {
		DOMImplementationLS implementation = (DOMImplementationLS) element.getOwnerDocument()
				.getImplementation();
		LSSerializer lsSerializer = implementation.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false);
		return lsSerializer.writeToString(element);
	}

	/**
	 * Writes the document to the given output stream with the given encoding.
	 *
	 * @param document     document to write to the stream
	 * @param outputStream output stream to write the document to
	 * @param encoding     the encoding to use when writing
	 */
	public static void documentToStream(Document document, OutputStream outputStream, String encoding) throws IOException {
		Source source = new DOMSource(document);
		Result result = new StreamResult(outputStream);
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			if (document.getXmlEncoding() == null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
			}
			else {
				transformer.setOutputProperty(OutputKeys.ENCODING, document.getXmlEncoding());
			}
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(source, result);
		}
		catch (TransformerFactoryConfigurationError | TransformerException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Writes the XML {@link Document} to the given output file.
	 *
	 * @param document     document to write to the stream
	 * @param outputStream output stream to write the document to
	 */
	public static void documentToStream(Document document, OutputStream outputStream) throws IOException {
		documentToStream(document, outputStream, "UTF-8");
	}

	/**
	 * Writes the XML {@link Document} as a string and returns it.
	 *
	 * @param document the document for which you want the content as a string
	 * @return the string content of the document
	 */
	public static String documentToString(Document document) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		documentToStream(document, stream);
		return new String(stream.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Writes the XML {@link Document} to the given output file.
	 *
	 * @param document   document to write to the stream
	 * @param outputFile output stream to write the document to
	 */
	public static void documentToFile(Document document, File outputFile) throws IOException {
		documentToStream(document, new FileOutputStream(outputFile));
	}

	/**
	 * Creates a new empty document
	 *
	 * @return a newly created empty document
	 */
	public static Document createEmptyDocument() throws IOException {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = fac.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new IOException(e.getMessage());
		}
		return builder.newDocument();
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
