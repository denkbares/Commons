/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
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

package de.d3web.strings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

public class Strings {

	/**
	 * This method appends the strings or objects and separates them with the
	 * specified separation string in between (but not at the end). You can
	 * specify all types of objects, they will be printed as
	 * {@link String#valueOf(Object)} would do.
	 * 
	 * @param separator the separating text in between the concatenated strings
	 * @param strings the strings to be concatenated
	 * @return the resulting concatenation
	 */
	public static String concat(String separator, Collection<?> strings) {
		if (strings == null) return "";
		return concat(separator, strings.toArray());
	}

	/**
	 * This method appends the strings or objects and separates them with the
	 * specified separation string in between (but not at the end). You can
	 * specify all types of objects, they will be printed as
	 * {@link String#valueOf(Object)} would do.
	 * 
	 * @param separator the separating text in between the concatenated strings
	 * @param strings the strings to be concatenated
	 * @return the resulting concatenation
	 */
	public static String concat(String separator, Object[] strings) {
		StringBuilder result = new StringBuilder();
		if (strings != null) {
			for (int i = 0; i < strings.length; i++) {
				if (i > 0) result.append(separator);
				result.append(strings[i]);
			}
		}
		return result.toString();
	}

	public static boolean containsUnquoted(String text, String symbol) {
		return splitUnquoted(text + "1", symbol).size() > 1;
	}

	/**
	 * Tests if the specified text string ends with the specified prefix.
	 * 
	 * 
	 * @created 18.10.2010
	 * @param text the text string to be checked
	 * @param prefix the prefix to be looked for
	 * @return <code>true</code> if the character sequence represented by the
	 *         argument is a suffix of the character sequence represented by the
	 *         specified text string; <code>false</code> otherwise. Note also
	 *         that <code>true</code> will be returned if the argument is an
	 *         empty string or is equal to this <code>String</code> object as
	 *         determined by the {@link #equals(Object)} method.
	 * @throws NullPointerException if any of the specified strings is null
	 */
	public static boolean endsWithIgnoreCase(String text, String suffix) {
		int length = suffix.length();
		int offset = text.length() - length;
		if (offset < 0) return false;
		for (int i = 0; i < length; i++) {
			char tc = Character.toLowerCase(text.charAt(offset + i));
			char pc = Character.toLowerCase(suffix.charAt(i));
			if (tc != pc) return false;
		}
		return true;
	}

	/**
	 * For a given index of an opening symbol (usually brackets) it finds (char
	 * index of) the corresponding closing bracket/symbol
	 * 
	 * @param text
	 * @param openBracketIndex
	 * @param open
	 * @param close
	 * @return
	 */
	public static int findIndexOfClosingBracket(String text, int openBracketIndex, char open, char close) {
		if (text.charAt(openBracketIndex) == open) {
			boolean quoted = false;
			int closedBrackets = -1;
			// scanning the text
			for (int i = openBracketIndex + 1; i < text.length(); i++) {
				char current = text.charAt(i);

				// toggle quote state
				if (isUnEscapedQuote(text, i)) {
					quoted = !quoted;
				}
				// decrement closed brackets when open bracket is found
				else if (!quoted && current == open) {
					closedBrackets--;
				}
				// increment closed brackets when closed bracket found
				else if (!quoted && current == close) {
					closedBrackets++;
				}

				// we have close the desired bracket
				if (closedBrackets == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Scans the 'text' for occurrences of 'symbol' which are not embraced by
	 * (unquoted) brackets (opening bracket 'open' and closing bracket 'close')
	 * Here the kind of bracket can be passed as char, however it will also work
	 * with char that are not brackets.. ;-)
	 * 
	 * @param text
	 * @param symbol
	 * @param open
	 * @param close
	 * @return
	 */
	public static List<Integer> findIndicesOfUnbraced(String text, String symbol, char open, char close) {
		List<Integer> result = new ArrayList<Integer>();
		boolean quoted = false;
		int openBrackets = 0;
		// scanning the text
		for (int i = 0; i < text.length(); i++) {
			char current = text.charAt(i);

			// toggle quote state
			if (isUnEscapedQuote(text, i)) {
				quoted = !quoted;
			}
			// decrement closed brackets when open bracket is found
			else if (!quoted && current == open) {
				openBrackets--;
			}
			// increment closed brackets when closed bracket found
			else if (!quoted && current == close) {
				openBrackets++;
			}

			// we have no bracket open => check for key symbol
			else if (openBrackets == 0 && !quoted) {
				if (text.substring(i).startsWith(symbol)) {
					result.add(i);
				}
			}

		}
		return result;

	}

	public static String[] getCharacterChains(String text) {
		String content = text.trim();
		String[] entries = content.split(" ");

		List<String> nonEmpty = new ArrayList<String>();
		for (String string : entries) {
			if (!string.equals("")) {
				nonEmpty.add(string);
			}
		}
		return nonEmpty.toArray(new String[nonEmpty.size()]);
	}

	public static StringFragment getFirstNonEmptyLineContent(String text) {
		List<StringFragment> lineFragmentation = getLineFragmentation(text);
		for (StringFragment stringFragment : lineFragmentation) {
			if (stringFragment.getContent().trim().length() > 0) return stringFragment;
		}
		return null;

	}

	public static List<StringFragment> getLineFragmentation(String text) {
		List<StringFragment> result = new ArrayList<StringFragment>();
		Pattern pattern = Pattern.compile("\\r?\\n");
		Matcher m = pattern.matcher(text);
		int lastIndex = 0;
		while (m.find()) {
			result.add(new StringFragment(text.substring(lastIndex, m.start()),
					lastIndex, text));
			lastIndex = m.end();
		}
		return result;
	}

	/**
	 * Scans the 'text' for the (first) occurrence of 'symbol' which is not
	 * embedded in quotes ('"')
	 * 
	 * @param text the text to search in
	 * @param symbol the symbol to be searched
	 * @return the index of the first unquoted occurrence of the symbol
	 */
	public static int indexOfUnquoted(String text, String symbol) {
		boolean quoted = false;
		// scanning the text
		for (int i = 0; i < text.length(); i++) {

			// toggle quote state
			if (isUnEscapedQuote(text, i)) {
				quoted = !quoted;
			}
			// ignore quoted symbols
			if (quoted) {
				continue;
			}
			// when symbol discovered return index
			if ((i + symbol.length() <= text.length())
					&& text.subSequence(i, i + symbol.length()).equals(symbol)) {
				return i;
			}

		}
		return -1;
	}

	/**
	 * Returns whether the specified {@link String} is null or only consists of
	 * whitespaces.
	 * <p>
	 * The method returns as follows:
	 * <ul>
	 * <li>StringUtils.isBlank(null): true
	 * <li>StringUtils.isBlank(""): true
	 * <li>StringUtils.isBlank(" "): true
	 * <li>StringUtils.isBlank(" d3web "): false
	 * </ul>
	 * 
	 * @param text the string to be checked
	 * @return <code>true</code> iff the string has no non-whitespace character
	 */
	public static boolean isBlank(String text) {
		if (text == null) return true;
		return text.matches("[\\s\\xA0]*");
	}

	/**
	 * Return whether some index in a string is in quotes or not. The indices of
	 * the quote characters are considered to also be in quotes.
	 * 
	 * If a index is given which does not fit inside the given text, an
	 * {@link IllegalArgumentException} is thrown.
	 * 
	 * @param text the text which may contain quotes
	 * @param index the index or position in the text which will be check if it
	 *        is in quotes or not
	 */
	public static boolean isQuoted(String text, int index) {
		if (index < 0 || index > text.length() - 1) {
			throw new IllegalArgumentException(index + " is not an index in the string '" + text
					+ "'");
		}
		boolean quoted = false;
		// scanning the text
		for (int i = 0; i < text.length(); i++) {

			// we consider the indexes of the opening and
			// closing quotes as also in quotes
			boolean isClosingQuote = false;

			// toggle quote state
			if (isUnEscapedQuote(text, i)) {
				if (quoted) isClosingQuote = true;
				quoted = !quoted;
			}
			// when symbol discovered return quoted
			if ((i == index)) {
				return quoted || isClosingQuote;
			}

		}
		return false;
	}

	/**
	 * Checks whether the given text is correctly and completely quoted. This
	 * means that it starts and ends with a quote that is not escaped and the
	 * text does not have any other not escaped quotes in between.<br/>
	 * An escaped quote is a quote that is preceded by a backslash -> \"<br/>
	 * The escaping backslash cannot be escaped itself by another backslash.
	 * 
	 * 
	 * @created 30.05.2012
	 * @param text the text to be checked
	 * @returns whether the given text is quoted
	 */
	public static boolean isQuoted(String text) {
		if (text.length() < 2) return false;

		boolean quoted = false;
		for (int i = 0; i < text.length(); i++) {
			if (isUnEscapedQuote(text, i)) {
				if (i == 0) {
					quoted = true;
				}
				else if (quoted) {
					return i == text.length() - 1;
				}
			}
			if (i >= 0 && !quoted) break;
		}

		return false;
	}

	public static boolean isUnEscapedQuote(String text, int i, char quoteChar) {
		return text.length() > i && text.charAt(i) == quoteChar
				&& getNumberOfDirectlyPrecedingBackSlashes(text, i) % 2 == 0;
	}

	public static boolean isUnEscapedQuote(String text, int i) {
		return isUnEscapedQuote(text, i, '"');
	}

	private static int getNumberOfDirectlyPrecedingBackSlashes(String text, int i) {
		int number = 0;
		i--;
		while (i >= 0) {
			if (text.charAt(i) == '\\') {
				number++;
				i--;
			}
			else {
				break;
			}
		}
		return number;
	}

	/**
	 * scans the 'text' for the last occurrence of 'symbol' which is not
	 * embraced in quotes ('"')
	 * 
	 * @param text
	 * @param symbol
	 * @return
	 */
	public static int lastIndexOfUnquoted(String text, String symbol) {
		boolean quoted = false;
		int lastIndex = -1;
		// scanning the text
		for (int i = 0; i < text.length(); i++) {

			// toggle quote state
			if (isUnEscapedQuote(text, i)) {
				quoted = !quoted;
			}
			// ignore quoted content
			if (quoted) {
				continue;
			}
			// if symbol found at that location remember index
			if ((i + symbol.length() <= text.length())
					&& text.subSequence(i, i + symbol.length()).equals(symbol)) {
				lastIndex = i;
			}

		}
		return lastIndex;
	}

	private static void mask(StringBuilder buffer, String toReplace) {
		int index = buffer.indexOf(toReplace);
		while (index >= 0) {
			// string starts with substring which should be replaced
			// or the char before the substring is not ~
			if (index == 0 || !buffer.substring(index - 1, index).equals("~")) {
				buffer.replace(index, index + toReplace.length(), "~" + toReplace);
			}
			index = buffer.indexOf(toReplace, index + 1);
		}
	}

	private static void unmask(StringBuilder buffer, String toReplace) {
		int index = buffer.indexOf(toReplace);
		while (index >= 0) {
			// string does not start with substring which should be replaced
			// or the char before the substring is ~
			if (index != 0 || buffer.substring(index - 1, index).equals("~")) {
				buffer.replace(index - 1, index + toReplace.length(), toReplace);
			}
			index = buffer.indexOf(toReplace, index + 1);
		}
	}

	// /**
	// *
	// * masks output strings
	// *
	// * @param htmlContent
	// * @return
	// */
	// public static String maskHTML(String htmlContent) {
	//
	// return "";
	// }

	/**
	 * Masks [, ], ----, {{{, }}} and %% so that JSPWiki will render and not
	 * interpret them, if the characters are already escaped, it will do nothing
	 * 
	 * @created 03.03.2011
	 */
	public static String maskJSPWikiMarkup(String string) {
		StringBuilder temp = new StringBuilder(string);
		Strings.maskJSPWikiMarkup(temp);
		return temp.toString();
	}

	/**
	 * Unmasks [, ], ----, {{{, }}} and %% so that tests of error messages run
	 * properly.
	 * 
	 * @created 28.09.2012
	 * @param builder
	 */
	public static String unmaskJSPWikiMarkup(String string) {
		StringBuilder temp = new StringBuilder(string);
		Strings.unmaskJSPWikiMarkup(temp);
		return temp.toString();
	}

	/**
	 * Masks [, ], ----, {{{, }}} and %% so that JSPWiki will render and not
	 * interpret them, if the characters are already escaped, it will do nothing
	 * 
	 * @created 03.03.2011
	 * @param builder
	 */
	public static void maskJSPWikiMarkup(StringBuilder builder) {
		mask(builder, "[");
		mask(builder, "]");
		mask(builder, "----");
		mask(builder, "{{{");
		mask(builder, "}}}");
		mask(builder, "%%");
		mask(builder, "\\");
	}

	/**
	 * Unmasks [, ], ----, {{{, }}} and %% so that tests of error messages run
	 * properly.
	 * 
	 * @created 28.09.2012
	 * @param builder
	 */
	public static void unmaskJSPWikiMarkup(StringBuilder builder) {
		unmask(builder, "[");
		unmask(builder, "]");
		unmask(builder, "----");
		unmask(builder, "{{{");
		unmask(builder, "}}}");
		unmask(builder, "%%");
		unmask(builder, "\\");
	}

	public static String replaceUmlaut(String text) {
		String result = text;
		result = result.replaceAll("Ä", "AE");
		result = result.replaceAll("Ö", "OE");
		result = result.replaceAll("Ü", "UE");
		result = result.replaceAll("ä", "ae");
		result = result.replaceAll("ö", "oe");
		result = result.replaceAll("ü", "ue");
		result = result.replaceAll("ß", "ss");
		return result;
	}

	public static List<StringFragment> splitUnquoted(String text, String splitSymbol) {
		return splitUnquoted(text, splitSymbol, true);
	}

	/**
	 * Splits the text by the <tt>splitSymbol</tt> disregarding splitSymbols
	 * which are quoted.
	 * 
	 * @param text
	 * @param splitSymbol
	 * @return the fragments of the text
	 */
	public static List<StringFragment> splitUnquoted(String text, String splitSymbol, boolean includeBlancFragments) {
		List<StringFragment> parts = new ArrayList<StringFragment>();
		if (text == null) return parts;
		boolean quoted = false;
		StringBuffer actualPart = new StringBuffer();
		// scanning the text
		int startOfNewPart = 0;
		for (int i = 0; i < text.length(); i++) {
			// toggle quote state
			if (isUnEscapedQuote(text, i)) {
				quoted = !quoted;
			}
			if (quoted) {
				actualPart.append(text.charAt(i));
				continue;
			}
			if (foundSplitSymbol(text, splitSymbol, i)) {
				String actualPartString = actualPart.toString();
				if (includeBlancFragments || !isBlank(actualPartString)) {
					parts.add(new StringFragment(actualPartString, startOfNewPart, text));
				}
				actualPart = new StringBuffer();
				i += splitSymbol.length() - 1;
				startOfNewPart = i + 1;
				continue;
			}
			actualPart.append(text.charAt(i));

		}
		String actualPartString = actualPart.toString();
		if (includeBlancFragments || !isBlank(actualPartString)) {
			parts.add(new StringFragment(actualPartString, startOfNewPart, text));
		}
		return parts;
	}

	private static boolean foundSplitSymbol(String text, String splitSymbol, int i) {
		return i + splitSymbol.length() <= text.length()
				&& text.regionMatches(i, splitSymbol, 0, splitSymbol.length());
		// && text.subSequence(i, i + splitSymbol.length()).equals(splitSymbol);
	}

	/**
	 * Writes the stack trace of a throwable instance into a string.
	 * 
	 * @created 06.06.2011
	 * @param e the throwable to be printed into the string
	 * @return the stack trace
	 */
	public static String stackTrace(Throwable e) {
		return ExceptionUtils.getStackTrace(e);
	}

	/**
	 * Tests if the specified text string starts with the specified prefix.
	 * 
	 * 
	 * @created 18.10.2010
	 * @param text the text string to be checked
	 * @param prefix the prefix to be looked for
	 * @return <code>true</code> if the character sequence represented by the
	 *         argument is a prefix of the character sequence represented by the
	 *         specified text string; <code>false</code> otherwise. Note also
	 *         that <code>true</code> will be returned if the argument is an
	 *         empty string or is equal to this <code>String</code> object as
	 *         determined by the {@link #equals(Object)} method.
	 * @throws NullPointerException if any of the specified strings is null
	 */
	public static boolean startsWithIgnoreCase(String text, String prefix) {
		int length = prefix.length();
		if (length > text.length()) return false;
		for (int i = 0; i < length; i++) {
			char tc = Character.toLowerCase(text.charAt(i));
			char pc = Character.toLowerCase(prefix.charAt(i));
			if (tc != pc) return false;
		}
		return true;
	}

	/**
	 * Returns a copy of the string, with leading whitespace omitted.
	 * <p>
	 * If this <code>String</code> object represents an empty character
	 * sequence, or the first character of character sequence represented by
	 * this <code>String</code> object has a code greater than
	 * <code>'&#92;u0020'</code> (the space character), then a reference to this
	 * <code>String</code> object is returned.
	 * <p>
	 * Otherwise, if there is no character with a code greater than
	 * <code>'&#92;u0020'</code> in the string, then a new <code>String</code>
	 * object representing an empty string is created and returned.
	 * <p>
	 * Otherwise, let <i>k</i> be the index of the first character in the string
	 * whose code is greater than <code>'&#92;u0020'</code>. A new
	 * <code>String</code> object is created, representing the substring of this
	 * string that begins with the character at index <i>k</i>, the result of
	 * <code>this.substring(<i>k</i>)</code>.
	 * <p>
	 * This method may be used to trim whitespace (as defined above) from the
	 * beginning and end of a string.
	 * 
	 * @return A copy of this string with leading white space removed, or this
	 *         string if it has no leading white space.
	 */
	public static String trimLeft(String text) {
		if (text == null) return null;
		int pos = 0;
		int len = text.length();
		while ((pos < len) && ((text.charAt(pos) <= ' ') || isNonBreakingSpace(text.charAt(pos)))) {
			pos++;
		}
		return (pos == 0) ? text : text.substring(pos);
	}

	public static String trim(String text) {
		return trimLeft(trimRight(text));
	}

	public static String trimQuotes(String text) {
		return unquote(trim(text));
	}

	/**
	 * Returns a copy of the string, with trailing whitespace omitted.
	 * <p>
	 * If this <code>String</code> object represents an empty character
	 * sequence, or the first character of character sequence represented by
	 * this <code>String</code> object has a code greater than
	 * <code>'&#92;u0020'</code> (the space character), then a reference to this
	 * <code>String</code> object is returned.
	 * <p>
	 * Otherwise, if there is no character with a code greater than
	 * <code>'&#92;u0020'</code> in the string, then a new <code>String</code>
	 * object representing an empty string is created and returned.
	 * <p>
	 * Otherwise, let <i>k</i> be the index of the first character in the string
	 * whose code is greater than <code>'&#92;u0020'</code>. A new
	 * <code>String</code> object is created, representing the substring of this
	 * string that begins with the character at index <i>k</i>, the result of
	 * <code>this.substring(<i>k</i>)</code>.
	 * <p>
	 * This method may be used to trim whitespace (as defined above) from the
	 * beginning and end of a string.
	 * 
	 * @return A copy of this string with leading white space removed, or this
	 *         string if it has no leading white space.
	 */
	public static String trimRight(String text) {
		if (text == null) return null;
		int pos = text.length();
		while ((pos > 0)
				&& ((text.charAt(pos - 1) <= ' ') || isNonBreakingSpace(text.charAt(pos - 1)))) {
			pos--;
		}
		return (pos == text.length()) ? text : text.substring(0, pos);
	}

	private static boolean isNonBreakingSpace(char c) {
		return c == (char) 160;
	}

	/**
	 * Quotes the given String with ". If the String contains ", it will be
	 * escaped with the escape char \.
	 * 
	 * @param element the string to be quoted
	 */
	public static String quote(String element) {
		return quote(element, '"');
	}

	/**
	 * Quotes the given String with a given quote char. If the String contains
	 * the quote char, it will be escaped with the escape char \. Don't use \ as
	 * the quote char for this reason.
	 * 
	 * @param element the string to be quoted
	 * @param quoteChar the char used to quote
	 */
	public static String quote(String element, char quoteChar) {
		String[] QUOTE_UNESCAPED = new String[] {
				"\\", Character.toString(quoteChar) };
		String[] QUOTE_ESCAPED = new String[] {
				"\\\\", "\\" + quoteChar };
		return quoteChar + StringUtils.replaceEach(element, QUOTE_UNESCAPED, QUOTE_ESCAPED)
				+ quoteChar;
	}

	/**
	 * Unquotes the given String. If the String contains an escaped quote char
	 * (\"), it will be unescaped.
	 * 
	 * @param element the string to be unquoted
	 */
	public static String unquote(String text) {
		return unquote(text, '"');
	}

	/**
	 * Unquotes the given String from the given quote char. If the String
	 * contains an escaped quote char (escaped with \), it will be unescaped.
	 * 
	 * @param element the string to be unquoted
	 * @param quoteChar the char the string was quoted with
	 */
	public static String unquote(String text, char quoteChar) {

		if (text == null) return null;

		if (text.length() == 1 && text.charAt(0) == quoteChar) return "";

		if (isUnEscapedQuote(text, 0, quoteChar)
				&& isUnEscapedQuote(text, text.length() - 1, quoteChar)) {
			text = text.substring(1, text.length() - 1);
			// unmask " and \
			String[] UNQOUTE_ESCAPED = new String[] {
					"\\" + quoteChar, "\\\\" };
			String[] UNQOUTE_UNESCAPED = new String[] {
					Character.toString(quoteChar), "\\" };
			return StringUtils.replaceEach(text, UNQOUTE_ESCAPED, UNQOUTE_UNESCAPED);
		}

		return text;
	}

	/**
	 * Safe way to url-encode strings without dealing with
	 * {@link UnsupportedEncodingException} of
	 * {@link URLEncoder#encode(String, String)}.
	 * 
	 * @created 03.05.2012
	 * @param text the text to be encoded
	 * @return the encoded string
	 */
	public static String encodeURL(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			Logger.getLogger(Strings.class.getName()).log(
					Level.WARNING, "Unsupported encoding UTF-8", e);
			return text;
		}
	}

	/**
	 * Escapes the given string for safely using user-input in web sites.
	 * 
	 * @param text Text to escape
	 * @return sanitized text
	 */
	public static String encodeHtml(String text) {
		if (text == null) return null;
		return text.replace("&", "&amp;").
				replace("\"", "&quot;").
				// this encoding are not decoded by the browser,
				// we can not use it
				// replaceAll("'", "&#39;")
				replace("<", "&lt;").
				replace(">", "&gt;").
				replace("#", "&#35;").
				replace("\\", "&#92;");
	}

	/**
	 * Safe way to url-decode strings without dealing with
	 * {@link UnsupportedEncodingException} of
	 * {@link URLEncoder#encode(String, String)}.
	 * 
	 * @created 03.05.2012
	 * @param text the text to be encoded
	 * @return the encoded string
	 */
	public static String decodeURL(String text) {
		try {
			return URLDecoder.decode(text, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			Logger.getLogger(Strings.class.getName()).log(
					Level.WARNING, e.getMessage());
			return text;
		}
		catch (IllegalArgumentException e) {
			Logger.getLogger(Strings.class.getName()).log(
					Level.WARNING, e.getMessage());
			return text;
		}
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 * 
	 * @created 16.09.2012
	 * @param filePath the file to be loaded
	 * @return the contents of the file
	 * @throws IOException if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 */
	public static String readFile(String filePath) throws IOException {
		File file = new File(filePath);
		return readFile(file);
	}

	/**
	 * Reads the contents of a file into a String and return the string.
	 * 
	 * @created 16.09.2012
	 * @param file the file to be loaded
	 * @return the contents of the file
	 * @throws IOException if there was any problem reading the file
	 * @throws NullPointerException if the argument is null.
	 */
	public static String readFile(File file) throws IOException {
		return readStream(new FileInputStream(file));
	}

	/**
	 * Reads the contents of a stream into a String and return the string.
	 * 
	 * @created 16.09.2012
	 * @param inputStream the stream to load from
	 * @return the contents of the file
	 * @throws IOException if there was any problem reading the file
	 */
	public static String readStream(InputStream inputStream) throws IOException {
		StringBuilder result = new StringBuilder();

		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream, "UTF-8"));
		char[] buf = new char[1024];
		int readCount = 0;
		while ((readCount = bufferedReader.read(buf)) != -1) {
			result.append(new String(buf, 0, readCount));
		}

		return result.toString();
	}

	public static void writeFile(String path, String content) throws IOException {
		FileWriter fstream = new FileWriter(path);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(content);
		out.close();
	}

}
