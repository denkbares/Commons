/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denkbares.collections.MinimizedHashSet;
import com.denkbares.utils.Files;
import com.denkbares.utils.Java;
import com.denkbares.utils.Pair;
import com.denkbares.utils.Streams;

public class Strings {
	private static final Logger LOGGER = LoggerFactory.getLogger(Strings.class);

	private static final Pattern PATTERN_BLANK = Pattern.compile("[\\s\\xA0]*");
	public static final char QUOTE_DOUBLE = '"';
	public static final String TRIPLE_QUOTES = "\"\"\"";
	public static final char QUOTE_SINGLE = '\'';
	private static final long[] TIME_FACTORS = {
			TimeUnit.MILLISECONDS.toMillis(1),
			TimeUnit.SECONDS.toMillis(1),
			TimeUnit.MINUTES.toMillis(1),
			TimeUnit.HOURS.toMillis(1),
			TimeUnit.DAYS.toMillis(1) };

	private static final String[] TIME_UNITS = {
			"ms", "s", "min", "h", "d" };

	private static final String[] TIME_UNITS_LONG = {
			"millisecond", "second", "minute", "hour", "day" };

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");

	/**
	 * TODO: Make private again when d3web-Persistence:XMLUtil.writeDate/readDate are removed
	 */
	public static final SimpleDateFormat DATE_FORMAT_COMPATIBILITY = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
	public static final SimpleDateFormat DATE_FORMAT_NO_SECONDS = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public static final SimpleDateFormat DATE_FORMAT_NO_TIME = new SimpleDateFormat("yyyy-MM-dd");

	private static final Map<Locale, Map<String, Set<Locale>>> DISPLAY_NAME_TO_LOCALE = new ConcurrentHashMap<>();
	private static final List<Locale> AVAILABLE_LOCALES = Arrays.asList(Locale.getAvailableLocales());
	private static final List<Locale> DEFAULT_LOCALES = new ArrayList<>();

	static {
		DEFAULT_LOCALES.add(Locale.GERMAN);
		DEFAULT_LOCALES.add(Locale.ENGLISH);
		DEFAULT_LOCALES.add(Locale.FRENCH);
		DEFAULT_LOCALES.add(Locale.ITALIAN);
		DEFAULT_LOCALES.add(Locale.CHINESE);
		DEFAULT_LOCALES.add(Locale.JAPANESE);
		DEFAULT_LOCALES.add(Locale.KOREAN);
		// make sure the default locale is first
		DEFAULT_LOCALES.remove(Locale.getDefault());
		DEFAULT_LOCALES.add(0, Locale.getDefault());
	}

	/**
	 * A Comparator that orders <code>String</code> objects as by <code>compareToIgnoreCase</code>. The comparator
	 * behaves identical to {@link String#CASE_INSENSITIVE_ORDER}, but handles <code>null</code> as the lowest string.
	 */
	public static final Comparator<String> CASE_INSENSITIVE_ORDER = (o1, o2) -> {
		//noinspection StringEquality
		if (o1 == o2) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		// both are != null, use String.CASE_INSENSITIVE_ORDER
		return String.CASE_INSENSITIVE_ORDER.compare(o1, o2);
	};

	/**
	 * This method appends the strings or objects and separates them with the specified separation string in between
	 * (but not at the end). You can specify all types of objects, they will be printed as {@link
	 * String#valueOf(Object)} would do. Empty or null values are ignored.
	 *
	 * @param separator the separating text in between the concatenated strings
	 * @param strings   the strings to be concatenated
	 * @return the resulting concatenation
	 */
	public static String concat(String separator, Collection<?> strings) {
		if (strings == null) return "";
		return concat(separator, strings.toArray());
	}

	/**
	 * This method appends the strings or objects and separates them with the specified separation string in between
	 * (but not at the end). You can specify all types of objects, they will be printed as {@link
	 * String#valueOf(Object)} would do. Empty or null values are ignored.
	 *
	 * @param separator the separating text in between the concatenated strings
	 * @param strings   the strings to be concatenated
	 * @return the resulting concatenation
	 */
	public static String concat(String separator, Object[] strings) {
		if (strings == null || strings.length == 0) return "";
		return Stream.of(strings)
				.filter(Objects::nonNull)
				.map(Object::toString)
				.filter(Strings::isNotBlank)
				.collect(Collectors.joining(separator));
	}

	public static boolean containsUnquoted(String text, String symbol) {
		return splitUnquoted(text + "1", symbol).size() > 1;
	}

	/**
	 * Tests if the specified text string ends with the specified suffix. If any of the specified strings are null,
	 * false is returned.
	 *
	 * @param text   the text string to be checked
	 * @param suffix the suffix to be looked for
	 * @return <code>true</code> if the character sequence represented by the argument is a suffix of the character
	 * sequence represented by the specified text string; <code>false</code> otherwise. Note also that <code>true</code>
	 * will be returned if the argument is an empty string or is equal to this <code>String</code> object as determined
	 * by the {@link #equals(Object)} method.
	 * @created 18.10.2010
	 */
	public static boolean endsWithIgnoreCase(String text, String suffix) {
		if (text == null) return false;
		if (suffix == null) return false;
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
	 * For a given index of an opening symbol (usually brackets) it finds (char index of) the corresponding closing
	 * bracket/symbol. If there are any opening brackets in between, there must be multiple closing brackets until the
	 * corresponding one is found. If there is no corresponding closing bracket/symbol -1 is returned. If there is no
	 * open bracket at the specified position -1 is also returned.
	 *
	 * @param text             the text to be searched
	 * @param openBracketIndex the index of the bracket
	 * @param open             the open bracket character
	 * @param close            the closing bracket character
	 * @return the index of the corresponding closing bracket character
	 */
	public static int indexOfClosingBracket(String text, int openBracketIndex, char open, char close) {
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
	 * Scans the 'text' for occurrences of 'symbol' which are not embraced by (unquoted) brackets (opening bracket
	 * 'open' and closing bracket 'close') Here the kind of bracket can be passed as char, however it will also work
	 * with char that are not brackets.. ;-)
	 *
	 * @param text   the text to be searched
	 * @param symbol the symbol to be matched
	 * @param open   the opening bracket character
	 * @param close  the closing bracket character
	 * @return the index of the first un-embraced character
	 */
	public static List<Integer> indicesOfUnbraced(String text, String symbol, char open, char close) {
		List<Integer> result = new ArrayList<>();
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

		List<String> nonEmpty = new ArrayList<>();
		for (String string : entries) {
			if (!string.isEmpty()) {
				nonEmpty.add(string);
			}
		}
		return nonEmpty.toArray(new String[0]);
	}

	public static StringFragment getFirstNonEmptyLineContent(String text) {
		List<StringFragment> lineFragmentation = getLineFragmentation(text);
		for (StringFragment stringFragment : lineFragmentation) {
			if (!stringFragment.getContent().trim().isEmpty()) return stringFragment;
		}
		return null;
	}

	public static List<StringFragment> getLineFragmentation(String text) {
		List<StringFragment> result = new ArrayList<>();
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
	 * Returns true if one of the given strings is contained in the given text. The case of the text and the strings are
	 * ignored. If text is null it is treated as an empty string.
	 *
	 * @param text    the text to search in
	 * @param strings the strings to be searched
	 * @return weather there is an occurrence of any of the strings in the text
	 */
	public static boolean containsIgnoreCase(String text, String... strings) {
		return indexOfIgnoreCase(text, strings) >= 0;
	}

	/**
	 * Finds the index of the first occurrence of one of the given strings in the given text. The case of the text and
	 * the strings are ignored. If text is null it is treated as an empty string.
	 *
	 * @param text    the text to search in
	 * @param strings the strings to be searched
	 * @return the index of the first occurrence of the strings
	 */
	public static int indexOfIgnoreCase(String text, String... strings) {
		return indexOf(text, CASE_INSENSITIVE, strings);
	}

	/**
	 * Finds the index of the first occurrence of one of the given strings in the given text. Occurrences between quotes
	 * are ignored. If text is null it is treated as an empty string.
	 *
	 * @param text    the text to search in
	 * @param strings the strings to be searched
	 * @return the index of the first unquoted occurrence of the strings
	 */
	public static int indexOfUnquoted(String text, String... strings) {
		return indexOf(text, UNQUOTED, strings);
	}

	/**
	 * Finds the index of the first occurrence of one of the given strings in the given text. If text is null it is
	 * treated as an empty string.
	 *
	 * @param text    the text where we search for the strings
	 * @param strings the strings for which you want the index in the text
	 * @return the first index of any of the strings in the text or -1 if none of the strings is found
	 */
	public static int indexOf(String text, String... strings) {
		return indexOf(text, 0, 0, strings);
	}

	/**
	 * Finds the index of the first occurrence of one of the given strings in the given text. Use the flags for more
	 * options. If text is null it is treated as an empty string.
	 *
	 * @param text    the text where we search for the strings
	 * @param flags   the settings flags to influence the behavior of the method
	 * @param strings the strings for which you want the index in the text
	 * @return the first index of any of the strings in the text or -1 if none of the strings is found
	 */
	public static int indexOf(String text, @IndexOfFlags int flags, String... strings) {
		return indexOf(text, 0, flags, strings);
	}

	/**
	 * When checking for unquoted indices, this flag will consider single quotes instead of normal double quotes as the
	 * quote character.
	 */
	public static final int SINGLE_QUOTED = 0x40;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> Using this flag will skip quoted
	 * strings.
	 */
	public static final int UNQUOTED = 0x01;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> Using this flag will skip comments
	 * (starting with double slash and ending at the end of the line).
	 */
	public static final int SKIP_COMMENTS = 0x02;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> Using this flag will return the last
	 * index instead of the first.
	 */
	public static final int LAST_INDEX = 0x04;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> Using this flag will match strings case
	 * insensitive.
	 */
	public static final int CASE_INSENSITIVE = 0x08;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> If this flag is set, the strings will
	 * only be matched against the start of the line, ignoring white spaces.<p> <b>Example:</b> Consider the following
	 * text: "&nbsp;&nbsp;&nbsp;TEXT, MORE TEXT"<br> Using this flag looking for the indices of TEXT will return index
	 * 3, because there are 3 preceding white spaces. The index for the second occurrence of TEXT will be ignored,
	 * because it is not at the start of the line.
	 */
	public static final int FIRST_IN_LINE = 0x10;

	/**
	 * Flag to be used with {@link Strings#indexOf(String, int, String...)}<p> Using this flag will only return results
	 * outside of braces.
	 */
	public static final int UNBRACED = 0x20;

	private static final Pattern LOCALE_PATTERN = Pattern.compile("([\\w^_]{2,3})(?:[\\-_]([\\w^_]{2,3})(?:[\\-_]#([^\\-_]*))?(?:[\\-_](\\p{Graph}+))?)?");

	private static boolean has(int flags, int flag) {
		return (flags & flag) != 0;
	}

	private static boolean stringsContainChar(String[] strings, char quoteChar) {
		for (String string : strings) {
			if (string.length() == 1 && string.equals(quoteChar + "")) return true;
		}
		return false;
	}

	/**
	 * Returns whether the specified {@link String} is null or only consists of whitespaces.
	 * <p>
	 * The method returns as follows: <ul> <li>Strings.isBlank(null): true <li>Strings.isBlank(""): true
	 * <li>Strings.isBlank(" "): true <li>Strings.isBlank("\n\r"): true <li>Strings.isBlank(" d3web "): false </ul>
	 *
	 * @param text the string to be checked
	 * @return <code>true</code> iff the string has only whitespace character or is empty or null
	 */
	@Contract("null -> true")
	public static boolean isBlank(String text) {
		return text == null || PATTERN_BLANK.matcher(text).matches();
		// matches against "[\\s\\xA0]*"
	}

	/**
	 * Returns whether the specified {@link String} has at least one non-whitespace.
	 * <p>
	 * The method returns as follows: <ul> <li>Strings.isNotBlank(null): false <li>Strings.isNotBlank(""): false
	 * <li>Strings.isNotBlank(" "): false <li>Strings.isNotBlank("\n\r"): false <li>Strings.isNotBlank(" d3web "): true
	 * </ul>
	 *
	 * @param text the string to be checked
	 * @return <code>true</code> iff the string has any non-whitespace characters
	 */
	public static boolean isNotBlank(String text) {
		return !isBlank(text);
	}

	/**
	 * Returns the prefix of the two specified strings that is common in both strings. If any of the strings is null,
	 * null is returned. If both texts are non-null, but have no shared prefix characters, an empty string is returned.
	 *
	 * @param text1 the first text to get the shared prefix from
	 * @param text2 the second text to get the shared prefix from
	 * @return the shared prefix of both texts (left-handed characters)
	 */
	@Contract("!null, !null -> !null; null, _ -> null; _, null -> null")
	public static String getSharedPrefix(String text1, String text2) {
		// if any is null, return null
		if (text1 == null || text2 == null) return null;

		// check the shared character length
		int length = Math.min(text1.length(), text2.length());
		for (int i = 0; i < length; i++) {
			if (text1.charAt(i) != text2.charAt(i)) {
				// if a character differs, return the substring up to this character (excluding)
				return text1.substring(0, i);
			}
		}

		// if the prefix is maximum, return the shorter input text
		return (text1.length() == length) ? text1 : text2;
	}

	/**
	 * Returns the number of unescaped quote characters in this string
	 *
	 * @param text      string to be examined
	 * @param quoteChar quote character, for example '"'
	 * @return the number of unescaped quote characters in this string
	 */
	public static int countUnescapedQuotes(String text, char quoteChar) {
		int count = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == quoteChar && isUnEscapedQuote(text, i)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Returns whether the specified {@link String} is neither null, nor it only consists of whitespaces. It returns
	 * true if the string contains any displayable characters. This is the same result as <code>!isBlank(...)</code>
	 * would return, but might be useful as a stream filter predicate.
	 * <p>
	 * The method returns as follows: <ul> <li>Strings.isBlank(null): false <li>Strings.isBlank(""): false
	 * <li>Strings.isBlank(" "): false <li>Strings.isBlank("\n\r"): false <li>Strings.isBlank(" d3web "): true </ul>
	 *
	 * @param text the string to be checked
	 * @return <code>true</code> iff the string has any non-whitespace character
	 */
	@Contract("null -> false")
	public static boolean nonBlank(String text) {
		return !isBlank(text);
	}

	/**
	 * Returns whether the specified {@link Character} is a whitespace.
	 *
	 * @param c the character to be checked
	 * @return <code>true</code> iff the character is a whitespace character
	 */
	public static boolean isBlank(char c) {
		return Character.isWhitespace(c) || c == '\u00A0';
	}

	/**
	 * Return whether some index in a string is in quotes or not. The indices of the quote characters are considered to
	 * also be in quotes.
	 * <p>
	 * If a index is given which does not fit inside the given text, an {@link IllegalArgumentException} is thrown.
	 *
	 * @param text  the text which may contain quotes
	 * @param index the index or position in the text which will be check if it is in quotes or not
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
	 * Checks whether the given text is correctly and completely quoted with double quotes. This means that it starts
	 * and ends with a quote that is not escaped and the text does not have any other not escaped quotes in
	 * between.<br/> An escaped quote is a quote that is preceded by a backslash -> \"<br/> The escaping backslash
	 * cannot be escaped itself by another backslash.
	 *
	 * @param text the text to be checked
	 * @return whether the given text is quoted
	 * @created 30.05.2012
	 */
	public static boolean isQuoted(String text) {
		return isQuoted(text, QUOTE_DOUBLE);
	}

	/**
	 * Checks whether the given text is correctly and completely quoted with the specified quote char. This means that
	 * it starts and ends with a quote that is not escaped and the text does not have any other not escaped quotes in
	 * between.<br/> An escaped quote is a quote that is preceded by a backslash -> \"<br/> The escaping backslash
	 * cannot be escaped itself by another backslash.
	 *
	 * @param text the text to be checked
	 * @return whether the given text is quoted
	 * @created 30.05.2012
	 */
	public static boolean isQuoted(String text, char quoteChar) {
		if (text.length() < 2) return false;
		if (text.charAt(0) != quoteChar) return false;
		if (!isUnEscapedQuote(text, text.length() - 1, quoteChar)) return false;

		for (int i = 1; i < text.length() - 1; i++) {
			if (isUnEscapedQuote(text, i, quoteChar)) return false;
		}
		return true;
	}

	@MagicConstant(flags = { UNQUOTED, SKIP_COMMENTS, LAST_INDEX, CASE_INSENSITIVE, FIRST_IN_LINE, UNBRACED, SINGLE_QUOTED })
	@interface IndexOfFlags {
	}

	public static boolean isUnEscapedQuote(String text, int i, char quoteChar) {
		return text.length() > i && text.charAt(i) == quoteChar
				&& getNumberOfDirectlyPrecedingBackSlashes(text, i) % 2 == 0;
	}

	public static boolean isUnEscapedQuote(String text, int i, char... quoteChars) {
		for (char quoteChar : quoteChars) {
			if (isUnEscapedQuote(text, i, quoteChar)) return true;
		}
		return false;
	}

	public static boolean isUnEscapedQuote(String text, int i) {
		return isUnEscapedQuote(text, i, QUOTE_DOUBLE);
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
	 * Scans the 'text' for the last occurrence of any of the strings, which are not embraced in quotes ('"') and
	 * returns the start index of the strings.
	 *
	 * @param text    the text to be searched
	 * @param strings the strings to be matched
	 * @return the last start index of the strings in unquoted text
	 */
	public static int lastIndexOfUnquoted(String text, String... strings) {
		return lastIndexOf(text, UNQUOTED, strings);
	}

	/**
	 * Finds the index of the last occurrence of any of the given strings in the given text. Use the flags for more
	 * settings.
	 *
	 * @param text    the text where we search for the strings
	 * @param flags   the settings flags to influence the behavior of the method
	 * @param strings the strings for which you want the index in the text
	 * @return the last index of any of the strings in the text or -1 if none of the strings is found
	 */
	public static int lastIndexOf(String text, @IndexOfFlags int flags, String... strings) {
		return indexOf(text, 0, flags | LAST_INDEX, strings);
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

	/**
	 * Replace all alphanumeric characters, umlauts and spaces of the text.
	 *
	 * @param text the text to replace from
	 * @return empty text, if null or clean text
	 */
	public static String replaceAlphanumericChars(String text) {
		return replaceAlphanumericChars(text, "");
	}

	/**
	 * Replace all alphanumeric characters, umlauts and spaces of the text by the given replacement string.
	 *
	 * @param text        the text to replace from
	 * @param replacement the string to insert instead
	 * @return empty text, if null or clean text
	 */
	public static String replaceAlphanumericChars(String text, String replacement) {
		if (text == null) return "";
		text = replaceUmlaut(text);
		text = text.replaceAll("[^a-zA-Z0-9]", " ");
		return text.replaceAll("\\s+", replacement);
	}

	/**
	 * Splits the specified text by any contained colon or semicolon. Each split item is individually trimmed, so
	 * leading or trailing whitespaces between the split character and the items will be removed. Blank/empty items are
	 * skipped. If the specified text is null or blank, or only contains blank split items, an empty array is returned.
	 * <p>
	 * Note that the list will not contain any empty item, e.g. <code>" , a, , b" --&gt; ["a", "b"]</code>
	 *
	 * @param text the textual list to be split
	 * @return the array of individual trimmed and non-empty items
	 */
	@NotNull
	public static String[] splitColonList(@Nullable String text) {
		if (isBlank(text)) return new String[0];
		return streamColonList(text).toArray(String[]::new);
	}

	/**
	 * Splits the specified text by any contained colon or semicolon. Each split item is individually trimmed, so
	 * leading or trailing whitespaces between the split character and the items will be removed. Blank/empty items are
	 * skipped. If the specified text is null or blank, or only contains blank split items, an empty array is returned.
	 * <p>
	 * Note that the list will not contain any empty item, e.g. <code>" , a, , b" --&gt; ["a", "b"]</code>
	 *
	 * @param text the textual list to be split
	 * @return the array of individual trimmed and non-empty items
	 */
	@NotNull
	public static Stream<String> streamColonList(@Nullable String text) {
		if (isBlank(text)) return Stream.empty();
		return Arrays.stream(trim(text).split("[\\s\\xA0]*[,;][\\s\\xA0]*")).filter(Strings::nonBlank);
	}

	public static String[] splitUnquotedToArray(String text, String splitSymbol) {

		List<StringFragment> stringFragments = splitUnquoted(text, splitSymbol, true, new QuoteSet(QUOTE_DOUBLE));
		String[] result = new String[stringFragments.size()];

		for (int i = 0; i < stringFragments.size(); i++) {
			result[i] = stringFragments.get(i).getContent();
		}
		return result;
	}

	public static List<StringFragment> splitUnquoted(String text, String splitSymbol) {
		return splitUnquoted(text, splitSymbol, true, new QuoteSet(QUOTE_DOUBLE));
	}

	public static List<StringFragment> splitUnquoted(String text, String splitSymbol, boolean includeBlankFragments) {
		return splitUnquoted(text, splitSymbol, includeBlankFragments, new QuoteSet(QUOTE_DOUBLE));
	}

	public static List<StringFragment> splitUnquoted(String text, String splitSymbol, QuoteSet... quoteChars) {
		return splitUnquoted(text, splitSymbol, true, quoteChars);
	}

	public static List<StringFragment> splitUnquoted(String text, String splitSymbol, char... quoteChars) {
		QuoteSet[] quotes = new QuoteSet[quoteChars.length];
		for (int i = 0; i < quotes.length; i++) {
			quotes[i] = new QuoteSet(quoteChars[i]);
		}
		return splitUnquoted(text, splitSymbol, true, quotes);
	}

	/**
	 * Splits the text by the <tt>splitSymbol</tt> disregarding splitSymbols which are quoted.
	 *
	 * @param text        the text to be split
	 * @param splitSymbol the regex to split by
	 * @return the fragments of the text
	 */
	public static List<StringFragment> splitUnquoted(String text, String splitSymbol, boolean includeBlankFragments, QuoteSet... quotes) {
		return splitUnquoted(text, Pattern.compile(Pattern.quote(splitSymbol)), includeBlankFragments, quotes);
	}

	/**
	 * Finds the index of the first occurrence of one of the given strings in the given text after the given offset. Use
	 * the flags for more options. If text is null it is treated as an empty string.
	 *
	 * @param text    the text where we search for the strings
	 * @param offset  the offset from where we start to look for the strings (flags like UNQUOTED or FIRST_IN_LINE also
	 *                consider the text before the offset!)
	 * @param flags   the settings flags to influence the behavior of the method
	 * @param strings the strings for which you want the index in the text
	 * @return the first index of any of the strings in the text or -1 if none of the strings is found
	 */
	public static int indexOf(String text, int offset, @IndexOfFlags int flags, String... strings) {
		if (text == null) text = "";
		boolean unquoted = has(flags, UNQUOTED);
		boolean skipComments = has(flags, SKIP_COMMENTS);
		boolean first = !has(flags, LAST_INDEX);
		boolean caseInsensitive = has(flags, CASE_INSENSITIVE);
		boolean firstInLine = has(flags, FIRST_IN_LINE);
		boolean unbraced = has(flags, UNBRACED);
		char quoteChar = has(flags, SINGLE_QUOTED) ? '\'' : '"';

		boolean quoted = false;
		boolean comment = false;
		boolean atLineStart = true;
		int bracedLevel = 0;

		int lastIndex = -1;

		// scanning the text
		for (int i = 0; i < text.length(); i++) {

			// if we reach a line end we know that we no longer are
			// inside a comment and instead at a line start again
			if (text.charAt(i) == '\n') {
				comment = false;
				atLineStart = true;
			}

			// if we skip comments and are currently in one, nothing further to do here
			if (skipComments && comment) continue;

			if (firstInLine) {
				// we skip if we only look at line starts, but if we are currently in quotes,
				// we first need to find the end of the quotes
				if (!atLineStart && !quoted) {
					continue;
				}
				// we also skip if we are currently in a braced section
				if (!atLineStart && unbraced && bracedLevel != 0) {
					continue;
				}
				if (!isWhitespace(text.charAt(i))) {
					atLineStart = false;
				}
			}

			// if we only want indexes outside quotes, check quote status
			if (unquoted) {
				// toggle quote state
				if (isUnEscapedQuote(text, i, quoteChar)) {

					// special case, we look for quotes!
					if (i >= offset && stringsContainChar(strings, quoteChar)) {
						return i;
					}

					// set quoted status
					quoted = !quoted;
				}
				// ignore indexes in quotes, also ignore comment start and braces
				if (quoted) continue;
			}

			if (skipComments) {
				// check comment status
				if (i + 2 <= text.length()
						&& text.charAt(i) == '/'
						&& text.charAt(i + 1) == '/') {
					comment = true;
				}
				// ignore comment
				if (comment) continue;
			}

			// if we only want indexes outside braced content, check brace depth/status
			if (unbraced) {
				if (text.charAt(i) == '(') {
					bracedLevel++;
				}
				else if (text.charAt(i) == ')' && bracedLevel > 0) {
					bracedLevel--;
				}
				// ignore braced content
				if (bracedLevel > 0) continue;
			}

			// we are before the offset, we don't yet look for the strings
			if (i < offset) continue;

			// when strings discovered return index
			for (String symbol : strings) {
				if (i + symbol.length() <= text.length()) {
					boolean matches;
					if (caseInsensitive) {
						matches = text.substring(i, i + symbol.length()).equalsIgnoreCase(symbol);
					}
					else {
						matches = text.startsWith(symbol, i);
					}
					if (matches) {
						lastIndex = i;
						if (first) return i;
					}
				}
			}
		}
		return lastIndex;
	}

	private static void toggleQuoteState(int[] quoteStates, int q) {
		if (quoteStates[q] == 0) {
			quoteStates[q] = 1;
		}
		else if (quoteStates[q] == 1) {
			quoteStates[q] = 0;
		}
	}

	private static boolean isHiddenByOtherQuote(int[] quoteStates, QuoteSet[] quotes, int q) {
		for (int i = 0; i < quotes.length; i++) {
			if (quoteStates[i] > 0 && quotes[i].hidesOtherQuotes() && q != i) {
				return true;
			}
		}
		return false;
	}

	private static boolean quoted(int[] quoteStates) {
		for (int b : quoteStates) {
			if (b > 0) return true;
		}
		return false;
	}

	/**
	 * Writes the stack trace of a throwable instance into a string.
	 *
	 * @param e the throwable to be printed into the string
	 * @return the stack trace
	 * @created 06.06.2011
	 */
	public static String stackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		e.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	/**
	 * Tests if the specified text string starts with the specified prefix. If any of the specified strings are null,
	 * false is returned.
	 *
	 * @param text   the text string to be checked
	 * @param prefix the prefix to be looked for
	 * @return <code>true</code> if the character sequence represented by the argument is a prefix of the character
	 * sequence represented by the specified text string; <code>false</code> otherwise. Note also that <code>true</code>
	 * will be returned if the argument is an empty string or is equal to this <code>String</code> object as determined
	 * by the {@link #equals(Object)} method.
	 * @created 18.10.2010
	 */
	public static boolean startsWithIgnoreCase(String text, String prefix) {
		if (text == null) return false;
		if (prefix == null) return false;
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
	 * Compares the specified two {@code String}s, ignoring case considerations. Two strings are considered equal if
	 * they are of the same length and corresponding characters in the two strings are equal ignoring case. If any of
	 * the two specified strings is null, it is considered to be the empty string ("").
	 *
	 * @param text1 The first {@code String} to be compared
	 * @param text2 The second {@code String} to be compared
	 * @return {@code true} if the arguments represents an equivalent {@code String} ignoring case; {@code false}
	 * otherwise
	 * @see String#equalsIgnoreCase(String)
	 */
	public static boolean equalsIgnoreCase(String text1, String text2) {
		// if both identical or both == null
		//noinspection StringEquality
		if (text1 == text2) return true;
		// otherwise (at least one is != null)
		// check null against empty string
		if (text1 == null) {
			//noinspection ConstantConditions
			return text2.isEmpty();
		}
		if (text2 == null) return text1.isEmpty();
		// otherwise we check the strings
		return text1.equalsIgnoreCase(text2);
	}

	/**
	 * Compares the specified two {@code String}s. Two strings are considered equal if they are of the same length and
	 * corresponding characters in the two strings are equal. If any of the two specified strings is null, it is
	 * considered to be the empty string ("").
	 *
	 * @param text1 The first {@code String} to be compared
	 * @param text2 The second {@code String} to be compared
	 * @return {@code true} if the arguments represents an equivalent {@code String}; {@code false} otherwise
	 * @see String#equals(Object)
	 */
	public static boolean equals(String text1, String text2) {
		// if both identical or both == null
		//noinspection StringEquality
		if (text1 == text2) return true;
		// otherwise (at least one is != null)
		// check null against empty string
		if (text1 == null) {
			//noinspection ConstantConditions
			return text2.isEmpty();
		}
		if (text2 == null) return text1.isEmpty();
		// otherwise we check the strings
		return text1.equals(text2);
	}

	/**
	 * Returns a sub-sequence of the source character sequence or string. In contrast to
	 * <code></code>state.getSource().substring(state.getSourceIndex)</code> the method does not copy any character or
	 * byte array.
	 *
	 * @return the sub-sequence of the source text
	 */
	@SuppressWarnings("unused")
	public static CharSequence subSequence(CharSequence source, int start, int end) {
		if (start > end || start < 0 || end > source.length()) {
			throw new StringIndexOutOfBoundsException();
		}
		return new CharSequence() {
			@Override
			public int length() {
				return end - start;
			}

			@Override
			public char charAt(int index) {
				int mapped = start + index;
				if (index < 0 || mapped > end) {
					throw new StringIndexOutOfBoundsException();
				}
				return source.charAt(mapped);
			}

			@Override
			public CharSequence subSequence(int s, int e) {
				if (s < 0 || e > length()) {
					throw new StringIndexOutOfBoundsException();
				}
				return Strings.subSequence(source, start + s, start + e);
			}
		};
	}

	/**
	 * Returns the beginning of specified text. If the text does not exceed the specified maxLength, the original text
	 * is returned unmodified. Otherwise the text is truncated, and an ellipsis "..." is appended, so that the total
	 * length is maxLength. The returned string never contains more characters as the specified maxLength.
	 * <p>
	 * If null is specified as text, null is returned.
	 *
	 * @param text      the text to be truncated
	 * @param maxLength the maximum character of the returned text
	 * @return the text, or truncated text with ellipsis
	 * @throws IllegalArgumentException if maxLength is below 3 and the text exceeds the maxLength
	 */
	public static String ellipsis(String text, int maxLength) {
		if (text == null) return null;
		if (text.length() <= maxLength) return text;

		if (maxLength < 3) throw new IllegalArgumentException("maxLength must be at least 3: " + maxLength);
		return text.substring(0, maxLength - 3) + "...";
	}

	/**
	 * Returns a copy of the string, with leading whitespace omitted.
	 * <p>
	 * If this <code>String</code> object represents an empty character sequence, or the first character of character
	 * sequence represented by this <code>String</code> object has a code greater than <code>'&#92;u0020'</code> (the
	 * space character), then a reference to this <code>String</code> object is returned.
	 * <p>
	 * Otherwise, if there is no character with a code greater than <code>'&#92;u0020'</code> in the string, then a new
	 * <code>String</code> object representing an empty string is created and returned.
	 * <p>
	 * Otherwise, let <i>k</i> be the index of the first character in the string whose code is greater than
	 * <code>'&#92;u0020'</code>. A new <code>String</code> object is created, representing the substring of this
	 * string that begins with the character at index <i>k</i>, the result of <code>this.substring(<i>k</i>)</code>.
	 * <p>
	 * This method may be used to trim whitespace (as defined above) from the beginning and end of a string.
	 *
	 * @return A copy of this string with leading white space removed, or this string if it has no leading white space.
	 */
	@Contract("null ->null; !null -> !null")
	public static String trimLeft(String text) {
		if (text == null) return null;
		int pos = trimLeft(text, 0, text.length());
		return (pos == 0) ? text : text.substring(pos);
	}

	@Contract("null -> null; !null -> !null")
	public static String trim(String text) {
		return trimLeft(trimRight(text));
	}

	public static String toLowerCase(String text) {
		if (text == null) return null;
		return text.toLowerCase();
	}

	/**
	 * Returns a collection containing all the strings from the passed collection being trimmed using Strings.trim()
	 *
	 * @param strings the strings to be trimmed
	 * @return the trimmed strings
	 * @created 20.11.2013
	 */
	public static List<String> trim(Collection<String> strings) {
		List<String> result = new ArrayList<>(strings.size());
		for (String string : strings) {
			result.add(trim(string));
		}
		return result;
	}

	/**
	 * Removes all blank lines before or after the specified string. All lines containing non-whitespace characters
	 * remain unchanged, except the last line, where the trailing line break is also removed.
	 *
	 * @param text the text to trim the empty lines from
	 * @return the trimmed text
	 * @created 15.08.2013
	 */
	public static String trimBlankLinesAndTrailingLineBreak(String text) {
		if (text == null) return null;
		return text.replaceFirst("\\A([ \t\u00A0]*\\r?\\n)+", "")
				.replaceFirst("(\\r?\\n[ \t\u00A0]*)+\\z", "");
	}

	/**
	 * Removes all blank lines before or after the specified string. All lines containing non-whitespace characters
	 * remain unchanged.
	 *
	 * @param text the text to trim the empty lines from
	 * @return the trimmed text
	 * @created 30.06.2016
	 */
	public static String trimBlankLines(String text) {
		if (text == null) return null;
		return text.replaceFirst("\\A([ \t\u00A0]*\\r?\\n)+", "")
				.replaceFirst("(?m)(^[ \t\u00A0]*\\r?\\n)+[ \t\u00A0]*\\z", "");
	}

	public static String trimQuotes(String text) {
		return unquote(trim(text));
	}

	/**
	 * Returns a copy of the string, with trailing whitespace omitted.
	 * <p>
	 * If this <code>String</code> object represents an empty character sequence, or the first character of character
	 * sequence represented by this <code>String</code> object has a code greater than <code>'&#92;u0020'</code> (the
	 * space character), then a reference to this <code>String</code> object is returned.
	 * <p>
	 * Otherwise, if there is no character with a code greater than <code>'&#92;u0020'</code> in the string, then a new
	 * <code>String</code> object representing an empty string is created and returned.
	 * <p>
	 * Otherwise, let <i>k</i> be the index of the first character in the string whose code is greater than
	 * <code>'&#92;u0020'</code>. A new <code>String</code> object is created, representing the substring of this
	 * string that begins with the character at index <i>k</i>, the result of <code>this.substring(<i>k</i>)</code>.
	 * <p>
	 * This method may be used to trim whitespace (as defined above) from the beginning and end of a string.
	 *
	 * @return A copy of this string with leading white space removed, or this string if it has no leading white space.
	 */
	@Contract("null -> null; !null -> !null")
	public static String trimRight(String text) {
		if (text == null) return null;
		int pos = trimRight(text, 0, text.length());
		return (pos == text.length()) ? text : text.substring(0, pos);
	}

	/**
	 * Given a text String, a start and a end index, this method will decrement the end index as long as the char before
	 * the end index is a white space and start < end. If the end can no longer be decremented, the end is returned.
	 */
	public static int trimRight(String text, int start, int end) {
		if (end > text.length()) return end;
		while (end > 0
				&& end > start
				&& isWhitespace(text.charAt(end - 1))) {
			end--;
		}
		return end;
	}

	/**
	 * Given a text String, a start and a end index, this method will increment the start index as long as the char at
	 * the start index is a white space and start < end. If the start can no longer be incremented, the start is
	 * returned.
	 */
	public static int trimLeft(String text, int start, int end) {
		while (start >= 0
				&& start < end
				&& start < text.length()
				&& isWhitespace(text.charAt(start))) {
			start++;
		}
		return start;
	}

	public static boolean isWhitespace(char c) {
		return c <= ' ' || isNonBreakingSpace(c);
	}

	/**
	 * Moves the given start and end indices together until they point to the boundaries of a trimmed string inside the
	 * text.
	 *
	 * @return a pair of integers representing start and end of trimmed string inside the given text
	 */
	public static Pair<Integer, Integer> trim(String text, int start, int end) {
		return new Pair<>(trimLeft(text, start, end), trimRight(text, start, end));
	}

	private static boolean isNonBreakingSpace(char c) {
		return c == (char) 160;
	}

	/**
	 * Quotes the given String with ". If the String contains ", it will be escaped with the escape char \.
	 *
	 * @param text the string to be quoted
	 */
	public static String quote(String text) {
		return quote(text, QUOTE_DOUBLE);
	}

	/**
	 * Quotes the given String with '. If the String contains ', it will be escaped with the escape char \.
	 *
	 * @param text the string to be quoted
	 */
	public static String quoteSingle(String text) {
		return quote(text, QUOTE_SINGLE);
	}

	/**
	 * Quotes the given String with a given quote char. If the String contains the quote char, it will be escaped with
	 * the escape char \. Don't use \ as the quote char for this reason.
	 *
	 * @param text      the string to be quoted
	 * @param quoteChar the char used to quote
	 */
	public static String quote(String text, char quoteChar) {
		if (text == null) return null;
		StringBuilder builder = new StringBuilder((text.length()) + 5);
		builder.append(quoteChar);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == quoteChar || c == '\\') {
				builder.append('\\');
			}
			builder.append(c);
		}
		builder.append(quoteChar);
		return builder.toString();
	}

	/**
	 * Unquotes the given String. If the String contains an escaped quote char (\"), it will be unescaped. If the
	 * specified text ist null, null is returned. If the specified text is not quoted the original text is returned.
	 *
	 * @param text the string to be unquoted
	 */
	public static String unquote(String text) {
		return unquote(text, '"');
	}

	/**
	 * Unquotes the given String from the given quote char. If the String contains an escaped quote char (escaped with
	 * \), it will be unescaped.
	 *
	 * @param text      the text to be unquoted
	 * @param quoteChar the char the string was quoted with
	 */
	public static String unquote(String text, char quoteChar) {

		if (text == null) return null;

		if (text.length() == 1 && text.charAt(0) == quoteChar) return "";

		int end = text.length() - 1;
		if (isUnEscapedQuote(text, 0, quoteChar)
				&& isUnEscapedQuote(text, end, quoteChar)) {

			StringBuilder builder = new StringBuilder(text.length() - 2);
			boolean escape = false;
			for (int i = 1; i < end; i++) {
				char c = text.charAt(i);
				if (c == '\\' && !escape && i < end - 1) {
					char next = text.charAt(i + 1);
					if (next == '\\' || next == quoteChar) {
						escape = true;
						continue;
					}
				}
				builder.append(c);
				escape = false;
			}
			text = builder.toString();
		}
		return text;
	}

	/**
	 * Unquotes the given String from any of the given quote characters. If the String contains an escaped quote char
	 * (escaped with \), it will be unescaped.
	 *
	 * @param text       the text to be unquoted
	 * @param quoteChars the characters the string is potentially quoted with
	 */
	public static String unquote(String text, char... quoteChars) {
		if (text != null && !text.isEmpty()) {
			char first = text.charAt(0);
			for (char quoteChar : quoteChars) {
				// if the quote char matches the first char, it will be unquoted with that char
				if (first == quoteChar) {
					return unquote(text, quoteChar);
				}
			}
		}
		// if null, or empty, or not quoted, retrun original text
		return text;
	}

	/**
	 * Safe way to url-encode strings without dealing with {@link UnsupportedEncodingException} of {@link
	 * URLEncoder#encode(String, String)}. If the specified text is null, null is returned.
	 *
	 * @param text the text to be encoded
	 * @return the encoded string
	 * @created 03.05.2012
	 */
	public static String encodeURL(String text) {
		if (text == null) return null;
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	/**
	 * Safe way to modify strings that they can used as path segments in any known file system. All critical characters
	 * will be replaced. If the specified text is null, null is returned.
	 * <p>
	 * Note: This method will potentially return the same string for multiple input strings, so uniqueness of the
	 * filename is not (!) preserved.
	 *
	 * @param text the text to be as a file name
	 * @return the encoded string, potentially clashing with other strings that will produce the same filename
	 * @created 03.05.2012
	 */
	public static String encodeFileName(String text) {
		if (text == null) return null;
		return trim(text.replaceAll("[\u0000-\u001F]+", " ").replaceAll("[^\u0000-\uFFFF]+", ""))
				.replaceAll("[\\\\/|;:<>?*]+", "_")
				.replaceAll("^(CON|PRN|AUX|NUL|(COM\\d)|(LPT\\d))$", "$1_")
				.replaceAll("\\.$", "_")
				.replace('"', '\'');
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
				replace("'", "&apos;").
				replace("<", "&lt;").
				replace(">", "&gt;").
				replace("#", "&#35;").
				replace("%", "&#37;").
				replace("|", "&#124;").
				replace("[", "&#91;").
				replace("]", "&#93;").
				replace("\\", "&#92;");
	}

	private static Pattern ENTITY_PATTERN = null;
	private static Map<String, String> NAMED_ENTITIES = null;

	/**
	 * Decodes the html entities of a given String. Currently the method only supports all known named entities and all
	 * ascii-coded entities. More entities are easy to be added.
	 *
	 * @param text the text to be decoded
	 * @return the decoded result
	 * @created 21.08.2013
	 */
	public static String decodeHtml(String text) {
		if (text == null) return null;

		if (ENTITY_PATTERN == null) {
			ENTITY_PATTERN = Pattern.compile("&(?:#(\\d{1,5})|(\\w{1,8}));");

			NAMED_ENTITIES = new HashMap<>(340);
			NAMED_ENTITIES.put("apos", "'");

			// all entities according to w3c
			// see 'http://www.w3.org/TR/WD-html40-970708/sgml/entities.html'
			NAMED_ENTITIES.put("Aacute", "\u00c1");
			NAMED_ENTITIES.put("aacute", "\u00e1");
			NAMED_ENTITIES.put("Acirc", "\u00c2");
			NAMED_ENTITIES.put("acirc", "\u00e2");
			NAMED_ENTITIES.put("acute", "\u00b4");
			NAMED_ENTITIES.put("AElig", "\u00c6");
			NAMED_ENTITIES.put("aelig", "\u00e6");
			NAMED_ENTITIES.put("Agrave", "\u00c0");
			NAMED_ENTITIES.put("agrave", "\u00e0");
			NAMED_ENTITIES.put("alefsym", "\u2135");
			NAMED_ENTITIES.put("Alpha", "\u0391");
			NAMED_ENTITIES.put("alpha", "\u03B1");
			NAMED_ENTITIES.put("amp", "\u0026");
			NAMED_ENTITIES.put("and", "\u2227");
			NAMED_ENTITIES.put("ang", "\u2220");
			NAMED_ENTITIES.put("Aring", "\u00c5");
			NAMED_ENTITIES.put("aring", "\u00e5");
			NAMED_ENTITIES.put("asymp", "\u2248");
			NAMED_ENTITIES.put("Atilde", "\u00c3");
			NAMED_ENTITIES.put("atilde", "\u00e3");
			NAMED_ENTITIES.put("Auml", "\u00c4");
			NAMED_ENTITIES.put("auml", "\u00e4");
			NAMED_ENTITIES.put("bdquo", "\u201E");
			NAMED_ENTITIES.put("Beta", "\u0392");
			NAMED_ENTITIES.put("beta", "\u03B2");
			NAMED_ENTITIES.put("brvbar", "\u00a6");
			NAMED_ENTITIES.put("bull", "\u2022");
			NAMED_ENTITIES.put("cap", "\u2229");
			NAMED_ENTITIES.put("Ccedil", "\u00c7");
			NAMED_ENTITIES.put("ccedil", "\u00e7");
			NAMED_ENTITIES.put("cedil", "\u00b8");
			NAMED_ENTITIES.put("cent", "\u00a2");
			NAMED_ENTITIES.put("Chi", "\u03A7");
			NAMED_ENTITIES.put("chi", "\u03C7");
			NAMED_ENTITIES.put("circ", "\u02C6");
			NAMED_ENTITIES.put("clubs", "\u2663");
			NAMED_ENTITIES.put("cong", "\u2245");
			NAMED_ENTITIES.put("copy", "\u00a9");
			NAMED_ENTITIES.put("crarr", "\u21B5");
			NAMED_ENTITIES.put("cup", "\u222A");
			NAMED_ENTITIES.put("curren", "\u00a4");
			NAMED_ENTITIES.put("dagger", "\u2020");
			NAMED_ENTITIES.put("Dagger", "\u2021");
			NAMED_ENTITIES.put("darr", "\u2193");
			NAMED_ENTITIES.put("dArr", "\u21D3");
			NAMED_ENTITIES.put("deg", "\u00b0");
			NAMED_ENTITIES.put("Delta", "\u0394");
			NAMED_ENTITIES.put("delta", "\u03B4");
			NAMED_ENTITIES.put("diams", "\u2666");
			NAMED_ENTITIES.put("divide", "\u00f7");
			NAMED_ENTITIES.put("Eacute", "\u00c9");
			NAMED_ENTITIES.put("eacute", "\u00e9");
			NAMED_ENTITIES.put("Ecirc", "\u00ca");
			NAMED_ENTITIES.put("ecirc", "\u00ea");
			NAMED_ENTITIES.put("Egrave", "\u00c8");
			NAMED_ENTITIES.put("egrave", "\u00e8");
			NAMED_ENTITIES.put("empty", "\u2205");
			NAMED_ENTITIES.put("emsp", "\u2003");
			NAMED_ENTITIES.put("ensp", "\u2002");
			NAMED_ENTITIES.put("Epsilon", "\u0395");
			NAMED_ENTITIES.put("epsilon", "\u03B5");
			NAMED_ENTITIES.put("equiv", "\u2261");
			NAMED_ENTITIES.put("Eta", "\u0397");
			NAMED_ENTITIES.put("eta", "\u03B7");
			NAMED_ENTITIES.put("ETH", "\u00d0");
			NAMED_ENTITIES.put("eth", "\u00f0");
			NAMED_ENTITIES.put("Euml", "\u00cb");
			NAMED_ENTITIES.put("euml", "\u00eb");
			NAMED_ENTITIES.put("exist", "\u2203");
			NAMED_ENTITIES.put("fnof", "\u0192");
			NAMED_ENTITIES.put("forall", "\u2200");
			NAMED_ENTITIES.put("frac12", "\u00bd");
			NAMED_ENTITIES.put("frac14", "\u00bc");
			NAMED_ENTITIES.put("frac34", "\u00be");
			NAMED_ENTITIES.put("frasl", "\u2044");
			NAMED_ENTITIES.put("Gamma", "\u0393");
			NAMED_ENTITIES.put("gamma", "\u03B3");
			NAMED_ENTITIES.put("ge", "\u2265");
			NAMED_ENTITIES.put("gt", "\u003E");
			NAMED_ENTITIES.put("harr", "\u2194");
			NAMED_ENTITIES.put("hArr", "\u21D4");
			NAMED_ENTITIES.put("hearts", "\u2665");
			NAMED_ENTITIES.put("hellip", "\u2026");
			NAMED_ENTITIES.put("Iacute", "\u00cd");
			NAMED_ENTITIES.put("iacute", "\u00ed");
			NAMED_ENTITIES.put("Icirc", "\u00ce");
			NAMED_ENTITIES.put("icirc", "\u00ee");
			NAMED_ENTITIES.put("iexcl", "\u00a1");
			NAMED_ENTITIES.put("Igrave", "\u00cc");
			NAMED_ENTITIES.put("igrave", "\u00ec");
			NAMED_ENTITIES.put("image", "\u2111");
			NAMED_ENTITIES.put("infin", "\u221E");
			NAMED_ENTITIES.put("int", "\u222B");
			NAMED_ENTITIES.put("Iota", "\u0399");
			NAMED_ENTITIES.put("iota", "\u03B9");
			NAMED_ENTITIES.put("iquest", "\u00bf");
			NAMED_ENTITIES.put("isin", "\u2208");
			NAMED_ENTITIES.put("Iuml", "\u00cf");
			NAMED_ENTITIES.put("iuml", "\u00ef");
			NAMED_ENTITIES.put("Kappa", "\u039A");
			NAMED_ENTITIES.put("kappa", "\u03BA");
			NAMED_ENTITIES.put("Lambda", "\u039B");
			NAMED_ENTITIES.put("lambda", "\u03BB");
			NAMED_ENTITIES.put("lang", "\u2329");
			NAMED_ENTITIES.put("laquo", "\u00ab");
			NAMED_ENTITIES.put("larr", "\u2190");
			NAMED_ENTITIES.put("lArr", "\u21D0");
			NAMED_ENTITIES.put("lceil", "\u2308");
			NAMED_ENTITIES.put("ldquo", "\u201C");
			NAMED_ENTITIES.put("le", "\u2264");
			NAMED_ENTITIES.put("lfloor", "\u230a");
			NAMED_ENTITIES.put("lowast", "\u2217");
			NAMED_ENTITIES.put("loz", "\u25CA");
			NAMED_ENTITIES.put("lrm", "\u200e");
			NAMED_ENTITIES.put("lsaquo", "\u2039");
			NAMED_ENTITIES.put("lsquo", "\u2018");
			NAMED_ENTITIES.put("lt", "\u003C");
			NAMED_ENTITIES.put("macr", "\u00af");
			NAMED_ENTITIES.put("mdash", "\u2014");
			NAMED_ENTITIES.put("micro", "\u00b5");
			NAMED_ENTITIES.put("middot", "\u00b7");
			NAMED_ENTITIES.put("minus", "\u2212");
			NAMED_ENTITIES.put("Mu", "\u039C");
			NAMED_ENTITIES.put("mu", "\u03BC");
			NAMED_ENTITIES.put("nabla", "\u2207");
			NAMED_ENTITIES.put("nbsp", "\u00a0");
			NAMED_ENTITIES.put("ndash", "\u2013");
			NAMED_ENTITIES.put("ne", "\u2260");
			NAMED_ENTITIES.put("ni", "\u220B");
			NAMED_ENTITIES.put("not", "\u00ac");
			NAMED_ENTITIES.put("notin", "\u2209");
			NAMED_ENTITIES.put("nsub", "\u2284");
			NAMED_ENTITIES.put("Ntilde", "\u00d1");
			NAMED_ENTITIES.put("ntilde", "\u00f1");
			NAMED_ENTITIES.put("Nu", "\u039D");
			NAMED_ENTITIES.put("nu", "\u03BD");
			NAMED_ENTITIES.put("Oacute", "\u00d3");
			NAMED_ENTITIES.put("oacute", "\u00f3");
			NAMED_ENTITIES.put("Ocirc", "\u00d4");
			NAMED_ENTITIES.put("ocirc", "\u00f4");
			NAMED_ENTITIES.put("OElig", "\u0152");
			NAMED_ENTITIES.put("oelig", "\u0153");
			NAMED_ENTITIES.put("Ograve", "\u00d2");
			NAMED_ENTITIES.put("ograve", "\u00f2");
			NAMED_ENTITIES.put("oline", "\u203E");
			NAMED_ENTITIES.put("Omega", "\u03A9");
			NAMED_ENTITIES.put("omega", "\u03C9");
			NAMED_ENTITIES.put("Omicron", "\u039F");
			NAMED_ENTITIES.put("omicron", "\u03BF");
			NAMED_ENTITIES.put("oplus", "\u2295");
			NAMED_ENTITIES.put("or", "\u2228");
			NAMED_ENTITIES.put("ordf", "\u00aa");
			NAMED_ENTITIES.put("ordm", "\u00ba");
			NAMED_ENTITIES.put("Oslash", "\u00d8");
			NAMED_ENTITIES.put("oslash", "\u00f8");
			NAMED_ENTITIES.put("Otilde", "\u00d5");
			NAMED_ENTITIES.put("otilde", "\u00f5");
			NAMED_ENTITIES.put("otimes", "\u2297");
			NAMED_ENTITIES.put("Ouml", "\u00d6");
			NAMED_ENTITIES.put("ouml", "\u00f6");
			NAMED_ENTITIES.put("para", "\u00b6");
			NAMED_ENTITIES.put("part", "\u2202");
			NAMED_ENTITIES.put("permil", "\u2030");
			NAMED_ENTITIES.put("perp", "\u22A5");
			NAMED_ENTITIES.put("Phi", "\u03A6");
			NAMED_ENTITIES.put("phi", "\u03C6");
			NAMED_ENTITIES.put("Pi", "\u03A0");
			NAMED_ENTITIES.put("pi", "\u03C0");
			NAMED_ENTITIES.put("piv", "\u03D6");
			NAMED_ENTITIES.put("plusmn", "\u00b1");
			NAMED_ENTITIES.put("pound", "\u00a3");
			NAMED_ENTITIES.put("prime", "\u2032");
			NAMED_ENTITIES.put("Prime", "\u2033");
			NAMED_ENTITIES.put("prod", "\u220F");
			NAMED_ENTITIES.put("prop", "\u221D");
			NAMED_ENTITIES.put("Psi", "\u03A8");
			NAMED_ENTITIES.put("psi", "\u03C8");
			NAMED_ENTITIES.put("quot", "\"");
			NAMED_ENTITIES.put("radic", "\u221A");
			NAMED_ENTITIES.put("rang", "\u232A");
			NAMED_ENTITIES.put("raquo", "\u00bb");
			NAMED_ENTITIES.put("rarr", "\u2192");
			NAMED_ENTITIES.put("rArr", "\u21D2");
			NAMED_ENTITIES.put("rceil", "\u2309");
			NAMED_ENTITIES.put("rdquo", "\u201D");
			NAMED_ENTITIES.put("real", "\u211C");
			NAMED_ENTITIES.put("reg", "\u00ae");
			NAMED_ENTITIES.put("rfloor", "\u230b");
			NAMED_ENTITIES.put("Rho", "\u03A1");
			NAMED_ENTITIES.put("rho", "\u03C1");
			NAMED_ENTITIES.put("rlm", "\u200f");
			NAMED_ENTITIES.put("rsaquo", "\u203a");
			NAMED_ENTITIES.put("rsquo", "\u2019");
			NAMED_ENTITIES.put("sbquo", "\u201A");
			NAMED_ENTITIES.put("Scaron", "\u0160");
			NAMED_ENTITIES.put("scaron", "\u0161");
			NAMED_ENTITIES.put("sdot", "\u22C5");
			NAMED_ENTITIES.put("sect", "\u00a7");
			NAMED_ENTITIES.put("shy", "\u00ad");
			NAMED_ENTITIES.put("Sigma", "\u03A3");
			NAMED_ENTITIES.put("sigma", "\u03C3");
			NAMED_ENTITIES.put("sigmaf", "\u03C2");
			NAMED_ENTITIES.put("sim", "\u223C");
			NAMED_ENTITIES.put("spades", "\u2660");
			NAMED_ENTITIES.put("sub", "\u2282");
			NAMED_ENTITIES.put("sube", "\u2286");
			NAMED_ENTITIES.put("sum", "\u2211");
			NAMED_ENTITIES.put("sup", "\u2283");
			NAMED_ENTITIES.put("sup1", "\u00b9");
			NAMED_ENTITIES.put("sup2", "\u00b2");
			NAMED_ENTITIES.put("sup3", "\u00b3");
			NAMED_ENTITIES.put("supe", "\u2287");
			NAMED_ENTITIES.put("szlig", "\u00df");
			NAMED_ENTITIES.put("Tau", "\u03A4");
			NAMED_ENTITIES.put("tau", "\u03C4");
			NAMED_ENTITIES.put("there4", "\u2234");
			NAMED_ENTITIES.put("Theta", "\u0398");
			NAMED_ENTITIES.put("theta", "\u03B8");
			NAMED_ENTITIES.put("thetasym", "\u03D1");
			NAMED_ENTITIES.put("thinsp", "\u2009");
			NAMED_ENTITIES.put("THORN", "\u00de");
			NAMED_ENTITIES.put("thorn", "\u00fe");
			NAMED_ENTITIES.put("tilde", "\u02DC");
			NAMED_ENTITIES.put("times", "\u00d7");
			NAMED_ENTITIES.put("trade", "\u2122");
			NAMED_ENTITIES.put("Uacute", "\u00da");
			NAMED_ENTITIES.put("uacute", "\u00fa");
			NAMED_ENTITIES.put("uarr", "\u2191");
			NAMED_ENTITIES.put("uArr", "\u21D1");
			NAMED_ENTITIES.put("Ucirc", "\u00db");
			NAMED_ENTITIES.put("ucirc", "\u00fb");
			NAMED_ENTITIES.put("Ugrave", "\u00d9");
			NAMED_ENTITIES.put("ugrave", "\u00f9");
			NAMED_ENTITIES.put("uml", "\u00a8");
			NAMED_ENTITIES.put("upsih", "\u03D2");
			NAMED_ENTITIES.put("Upsilon", "\u03A5");
			NAMED_ENTITIES.put("upsilon", "\u03C5");
			NAMED_ENTITIES.put("Uuml", "\u00dc");
			NAMED_ENTITIES.put("uuml", "\u00fc");
			NAMED_ENTITIES.put("weierp", "\u2118");
			NAMED_ENTITIES.put("Xi", "\u039E");
			NAMED_ENTITIES.put("xi", "\u03BE");
			NAMED_ENTITIES.put("Yacute", "\u00dd");
			NAMED_ENTITIES.put("yacute", "\u00fd");
			NAMED_ENTITIES.put("yen", "\u00a5");
			NAMED_ENTITIES.put("yuml", "\u00ff");
			NAMED_ENTITIES.put("Yuml", "\u0178");
			NAMED_ENTITIES.put("Zeta", "\u0396");
			NAMED_ENTITIES.put("zeta", "\u03B6");
			NAMED_ENTITIES.put("zwj", "\u200d");
			NAMED_ENTITIES.put("zwnj", "\u200c");
		}

		StringBuilder result = new StringBuilder(text.length());
		int pos = 0;

		Matcher matcher = ENTITY_PATTERN.matcher(text);
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();

			// first append chars to next match
			result.append(text, pos, start);

			// then try to decode
			try {
				// try coded entity
				int code = Integer.parseInt(matcher.group(1));
				result.append((char) code);
			}
			catch (NumberFormatException e) {
				// try named entity
				String decoded = NAMED_ENTITIES.get(matcher.group(2));
				if (decoded != null) {
					result.append(decoded);
				}
				else {
					// otherwise add the match unchanged
					result.append(matcher.group());
				}
			}

			// move to position after match
			pos = end;
		}

		// append rest of string
		result.append(text.substring(pos));
		return result.toString();
	}

	/**
	 * Trims .0 from strings. This is useful in case double values are displayed as strings.
	 *
	 * @param dString is the String to trim
	 * @return trimmed {@link String}
	 * @created 16.05.2011
	 */
	public static String trimTrailingZero(String dString) {
		if (dString.endsWith(".0")) {
			dString = dString.substring(0, dString.length() - 2);
		}
		return dString;
	}

	/**
	 * Util method to get a String from a double, where the decimal point is omitted in case it is zero.
	 *
	 * @param d is the double to trim
	 * @return trimmed {@link String} representation of the {@link Double}
	 * @created 16.05.2011
	 */
	public static String trimTrailingZero(double d) {
		return trimTrailingZero(String.valueOf(d));
	}

	/**
	 * Returns the number of items together with the item name, correctly used in singular or plural case. It assumes
	 * that the item name is in English language.
	 * <pre>
	 *     pluralOf(0, "apple") -> "0 apples"
	 *     pluralOf(1, "user") -> "1 user"
	 *     pluralOf(2, "coin") -> "2 coins"
	 * </pre>
	 *
	 * @param itemCount the number of items
	 * @param itemName  the name of the item in singular case
	 * @return the item count plus the item name
	 */
	public static String pluralOf(int itemCount, String itemName) {
		return pluralOf(itemCount, itemName, true);
	}

	/**
	 * Returns the number of items together with the item name, correctly used in singular or plural case. It assumes
	 * that the item name is in English language.
	 * <pre>
	 *     pluralOf(0, "apple") -> "0 apples"
	 *     pluralOf(1, "user") -> "1 user"
	 *     pluralOf(2, "coin") -> "2 coins"
	 * </pre>
	 *
	 * @param itemCount     the number of items
	 * @param itemName      the name of the item in singular case
	 * @param includeNumber specifies whether the number should be included in the returned string
	 * @return the item count plus the item name
	 */
	public static String pluralOf(int itemCount, String itemName, boolean includeNumber) {
		String name = (itemCount == 1) ? itemName : Inflector.pluralOf(itemName);
		return includeNumber ? (itemCount + " " + name) : name;
	}

	/**
	 * Returns a plain text representation of a html source code. It preserves some basic formatting of html, e.g.
	 * unordered lists or paragraphs. May be improved in the future, e.g. for tables and similar.
	 *
	 * @param source the html source code
	 * @return the converted plain text.
	 */
	@Contract("null -> null; !null -> !null")
	public static String htmlToPlain(String source) {
		if (source == null) return null;
		String result = source;

		// Remove HTML Development formatting
		// Replace line breaks with space
		// because browsers inserts space
		result = result.replaceAll("\r", " ");
		// Replace line breaks with space
		// because browsers inserts space
		result = result.replaceAll("\n", " ");
		// Remove step-formatting
		result = result.replaceAll("\t", " ");

		// Remove the header (prepare first by clearing attributes)
		result = result.replaceAll("(?i)<( )*head([^>])*>", "<head>");
		result = result.replaceAll("(?i)(<( )*(/)( )*head( )*>)", "</head>");
		result = result.replaceAll("(?i)(<head>).*(</head>)", "");

		// remove all scripts (prepare first by clearing attributes)
		result = result.replaceAll("(?i)<( )*script([^>])*>", "<script>");
		result = result.replaceAll("(?i)(<( )*(/)( )*script( )*>)", "</script>");
		result = result.replaceAll("(?i)(<script>).*(</script>)", "");

		// remove all styles (prepare first by clearing attributes)
		result = result.replaceAll("(?i)<( )*style([^>])*>", "<style>");
		result = result.replaceAll("(?i)(<( )*(/)( )*style( )*>)", "</style>");
		result = result.replaceAll("(?i)(<style>).*(</style>)", "");

		// insert tabs in spaces of <td> tags
		result = result.replaceAll("(?i)<( )*td([^>])*>( )*", "\t");

		// insert line breaks in places of <BR> and <LI> tags
		result = result.replaceAll("(?i)<( )*br([^>])*>( )*", "\n");
		result = result.replaceAll("(?i)<( )*li([^>])*>( )*", "\n* ");

		// insert line paragraphs (double line breaks) in place
		// if <P>, <DIV> and <TR> tags, and after end of bullet-lists
		result = result.replaceAll("(?i)<( )*div([^>])*>( )*", "\n\n");
		result = result.replaceAll("(?i)<( )*tr([^>])*>( )*", "\n\n");
		result = result.replaceAll("(?i)<( )*p([^>])*>( )*", "\n\n");
		result = result.replaceAll("(?i)<( )*/(ol|ul)([^>])*> *", "\n\n");

		// Remove remaining tags like <a>, links, images,
		// comments etc - anything that's enclosed inside < >
		result = result.replaceAll("(?i)<[^>]*>", "");

		// replace special characters:
		result = result.replaceAll("(?i) ", " ");

		result = result.replaceAll("(?i)•", " * ");
		result = result.replaceAll("(?i)‹", "<");
		result = result.replaceAll("(?i)›", ">");
		result = result.replaceAll("(?i)™", "(tm)");
		result = result.replaceAll("(?i)⁄", "/");
		result = result.replaceAll("(?i)<", "<");
		result = result.replaceAll("(?i)>", ">");
		result = result.replaceAll("(?i)©", "(c)");
		result = result.replaceAll("(?i)®", "(r)");
		result = result.replaceAll("(?i)&nbsp;", " ");
		// Remove all others. More can be added, see
		// http://hotwired.lycos.com/webmonkey/reference/special_characters/
		result = result.replaceAll("(?i)&(.{2,6});", "");

		// make line breaking consistent
		result = result.replaceAll("\n", "\n");

		// Remove extra line breaks and tabs:
		// replace over 2 breaks with 2 and over 4 tabs with 4.
		// Prepare first to remove any whitespaces in between
		// the escaped characters and remove redundant tabs in between line breaks
		result = result.replaceAll("(?i)(\n)( )+(\n)", "\n\n");
		result = result.replaceAll("(?i)(\t)( )+(\t)", "\t\t");
		result = result.replaceAll("(?i)(\t)( )+(\n)", "\t\n");
		result = result.replaceAll("(?i)(\n)( )+(\t)", "\n\t");

		// Remove multiple spaces and redundant start-of-line / end-of-line spaces
		result = result.replaceAll("( )( )+", " ");
		result = result.replaceAll("[\t ]\n", "\n");
		result = result.replaceAll("\n( )+", "\n");

		// Remove multiple tabs or spaces following a line break with just one tab
		result = result.replaceAll("(?i)(\n)(\t )+", "\n\t");

		// remove multiple line-breaks
		result = result.replaceAll("\n\n\n+", "\n\n");

		// That's it. Trim finally and done.
		return trim(result);
	}

	public static String readStream(InputStream inputStream) {
		return Streams.readStream(inputStream);
	}

	/**
	 * Capitalizes the given string, meaning the first character will be upper case, all following unchanged. If
	 * there are multiple words, still only the first character of the string will be capitalized!
	 *
	 * @param text the text to capitalize (make the first char upper case, rest unchanged)
	 * @return the capitalized version of the text
	 */
	@NotNull
	public static String capitalize(@NotNull String text) {
		if (Strings.isBlank(text)) return text;
		if (Character.isUpperCase(text.charAt(0))) return text;
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	/**
	 * Concatenates an array of Strings in a way, that it can be parsed again without having to worry about the
	 * separator being present in the concatenated Strings, using the method #parseConcat. If necessary, path elements
	 * are set in quotes and quotes inside the strings are escaped properly.
	 *
	 * @param separator the separator used to concatenate
	 * @param strings   the strings to be concatenated
	 * @return a concatenated, properly escaped and again parsable string
	 */
	public static String concatParsable(String separator, String[] strings) {
		return concatParsable(separator, null, strings);
	}

	/**
	 * Concatenates an array of Strings in a way, that it can be parsed again without having to worry about the
	 * separator being present in the concatenated Strings, using the method #parseConcat. If necessary, path elements
	 * are set in quotes and quotes inside the strings are escaped properly.
	 *
	 * @param separator    the separator used to concatenate
	 * @param quotePattern optional pattern which will be used to decide, whether quotes should be added for the
	 *                     different concatenated strings. It should at least contain the separator, quote, and back
	 *                     slash!
	 * @param strings      the strings to be concatenated
	 * @return a concatenated, properly escaped and again parsable string
	 */
	public static String concatParsable(String separator, Pattern quotePattern, String[] strings) {
		StringBuilder concat = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			String element = strings[i];
			if (i > 0) concat.append(separator);
			if ((quotePattern != null && quotePattern.matcher(element).find())
					|| element.contains(separator) || element.contains("\\") || element.contains("\"")) {
				concat.append(quote(element));
			}
			else {
				concat.append(element);
			}
		}
		return concat.toString();
	}

	/**
	 * Parsed a String, that was previously concatenated using the method {@link #concatParsable(String, Pattern,
	 * String[])}. The returned string elements are unescaped and unqouted properly.
	 *
	 * @param separator          the separator used to concat the string
	 * @param concatenatedString the string to parse the elements from
	 * @return the elements of the concatenated string
	 */
	public static String[] parseConcat(String separator, String concatenatedString) {
		List<StringFragment> pathElementFragments = splitUnquoted(concatenatedString, separator, true);
		String[] elements = new String[pathElementFragments.size()];
		for (int i = 0; i < pathElementFragments.size(); i++) {
			StringFragment pathElementFragment = pathElementFragments.get(i);
			elements[i] = unquote(pathElementFragment.getContent());
		}
		return elements;
	}

	public enum Encoding {
		UTF8("UTF-8"), ISO_8859_1("ISO-8859-1");

		private final Charset charset;

		Encoding(String encoding) {
			this.charset = Charset.forName(encoding);
		}

		public String encoding() {
			return charset.name();
		}

		public Charset charset() {
			return charset;
		}
	}

	/**
	 * Safe way to url-decode strings without dealing with {@link UnsupportedEncodingException} of {@link
	 * URLEncoder#encode(String, String)}. The encoding can be specified by this function. In most cases UTF-8 encoding
	 * works best, see method {@link #decodeURL(String)} for this. If the specified text is null, null is returned.
	 *
	 * @param text     the text to be encoded
	 * @param encoding the encoding to be used for decode
	 * @return the encoded string
	 * @created 03.05.2012
	 */
	public static String decodeURL(String text, Encoding encoding) {
		if (text == null) return null;
		try {
			return URLDecoder.decode(text, encoding.encoding());
		}
		catch (UnsupportedEncodingException | IllegalArgumentException e) {
			LOGGER.warn(e.getMessage());
			return text;
		}
	}

	/**
	 * Safe way to url-decode strings without dealing with {@link UnsupportedEncodingException} of {@link
	 * URLEncoder#encode(String, String)}. It used UTF-8 encoding for decode. If this does not work well, try {@link
	 * #decodeURL(String, Encoding)} where you can specify a particular encoding. If the specified text is null, null is
	 * returned.
	 *
	 * @param text the text to be encoded
	 * @return the encoded string
	 * @created 03.05.2012
	 */
	public static String decodeURL(String text) {
		return decodeURL(text, Encoding.UTF8);
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
		return Files.readFile(filePath);
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
		return Files.readFile(file);
	}

	/**
	 * Writes the given string content to a file with the given path.
	 *
	 * @param path    the path to the file to be written
	 * @param content the content of the file to be written
	 * @throws IOException if writing fails
	 */
	public static void writeFile(String path, String content) throws IOException {
		Files.writeFile(path, content);
	}

	/**
	 * Writes the given string content to the given file
	 *
	 * @param file    the file to be written
	 * @param content the content of the file to be written
	 * @throws IOException if writing fails
	 */
	public static void writeFile(File file, String content) throws IOException {
		Files.writeFile(file, content);
	}

	/**
	 * Splits the text by the <tt>splitRegex</tt> disregarding splitSymbols which are quoted.
	 *
	 * @param text         the text to be split
	 * @param splitPattern the regex to split by
	 * @return the fragments of the text
	 */
	public static List<StringFragment> splitUnquoted(String text, Pattern splitPattern, boolean includeBlankFragments, QuoteSet... quotes) {
		List<StringFragment> parts = new ArrayList<>();
		if (text == null) return parts;

		quotes = Arrays.copyOf(quotes, quotes.length);
		// make sure triple quotes are first
		Arrays.sort(quotes, (o1, o2) -> {
			if (o1 == QuoteSet.TRIPLE_QUOTES && o2 != QuoteSet.TRIPLE_QUOTES) return -1;
			if (o1 != QuoteSet.TRIPLE_QUOTES && o2 == QuoteSet.TRIPLE_QUOTES) return 1;
			return 0;
		});

		// init quote state for each quote
		int[] quoteStates = new int[quotes.length];

		StringBuilder actualPart = new StringBuilder();

		Matcher matcher = splitPattern.matcher(text);
		List<Group> candidates = new ArrayList<>();
		while (matcher.find()) {
			candidates.add(new Group(matcher.start(), matcher.end()));
		}
		if (candidates.isEmpty()) {
			// not splitting in this text
			return Collections.singletonList(new StringFragment(text, 0, text));
		}
		Iterator<Group> candidateIterator = candidates.iterator();

		// scanning the text
		Group nextCandidate = candidateIterator.next();
		int startOfNewPart = 0;
		int skipQuoteDetectionUntil = -1;
		for (int i = 0; i < text.length(); i++) {

			// go to next split candidate if possible
			if (i > nextCandidate.start) {
				if (candidateIterator.hasNext()) {
					nextCandidate = candidateIterator.next();
				}
				else {
					// no more candidates, rest of the string is one fragment
					actualPart.append(text.substring(i));
					break;
				}
			}

			// tracking multiple quote states
			for (int q = 0; q < quotes.length; q++) {

				if (i <= skipQuoteDetectionUntil) continue;

				QuoteSet quoteSet = quotes[q];

				// check whether the quote is hidden by another quote, e.g. a
				// bracket in a literal-quote
				if (!isHiddenByOtherQuote(quoteStates, quotes, q)) {

					// first handle unary quotes
					if (quoteSet.isUnary()) {
						// handle special case for triple quotes (""")
						if (quoteSet == QuoteSet.TRIPLE_QUOTES
								// triple quotes cannot be escaped, so just try a match
								&& text.length() >= i + 3
								&& text.startsWith(TRIPLE_QUOTES, i)
								// don't match closing triple quotes at the start, but at the end of
								// a sequence of more than 3 quotes (e.g. """Hi there "stranger"""")
								&& !(quoteStates[q] == 1 && text.length() > i + 3 && text.charAt(i + 3) == TRIPLE_QUOTES
								.charAt(0))) {

							toggleQuoteState(quoteStates, q);

							// triple quotes might also match other quote set, so we skip
							skipQuoteDetectionUntil = i + TRIPLE_QUOTES.length();
							break;
						}
						// just ordinary unary quotes, open() == close()
						else if (isUnEscapedQuote(text, i, quoteSet.open())) {
							toggleQuoteState(quoteStates, q);
						}
					}
					// then handle binary (potentially nested) quotes
					else {

						// check for opening char
						if (isUnEscapedQuote(text, i, quoteSet.open())) {
							// this one is just being opened (once more)
							quoteStates[q]++;
						}
						// check for closing char
						if (isUnEscapedQuote(text, i, quoteSet.close())) {
							// this one is just being closed (once)
							quoteStates[q]--;
						}
					}
				}
			}
			if (quoted(quoteStates)) {
				actualPart.append(text.charAt(i));
				continue;
			}

			if (nextCandidate.start == i) {
				String actualPartString = actualPart.toString();
				if (includeBlankFragments || !isBlank(actualPartString)) {
					parts.add(new StringFragment(actualPartString, startOfNewPart, text));
				}
				actualPart = new StringBuilder();
				i = nextCandidate.end - 1;
				startOfNewPart = nextCandidate.end;
				continue;
			}
			actualPart.append(text.charAt(i));
		}
		String actualPartString = actualPart.toString();
		if (includeBlankFragments || !isBlank(actualPartString)) {
			parts.add(new StringFragment(actualPartString, startOfNewPart, text));
		}
		return parts;
	}

	/**
	 * Parses a locale from a locale string representation. This is the inverse method to {@link
	 * java.util.Locale#toString()}. If the specified text is null or "null" or cannot be parsed, null is returned. If
	 * the specified text is empty or "ROOT", the root locale is returned. Leading or trailing whitespaces will be
	 * ignored by this method.
	 *
	 * @param text the locale's text representation to be parsed
	 * @return the parsed locale
	 */
	public static Locale parseLocale(String text) {
		if (text == null) return null;
		text = text.trim();
		if (text.isEmpty()) return Locale.ROOT;
		if (equalsIgnoreCase(text, "ROOT")) return Locale.ROOT;
		if (equalsIgnoreCase(text, "null")) return null;
		Matcher matcher = LOCALE_PATTERN.matcher(text);
		if (matcher.matches()) {
			String lang = matcher.group(1);
			String country = matcher.group(2);
			String variant = matcher.group(4);
			// TODO: also use script to create locale (use LocaleBuilder) as soon as Java 7 is supported.
			// String script = matcher.group(3);
			return new Locale(
					lang == null ? "" : lang,
					country == null ? "" : country,
					variant == null ? "" : variant);
		}
		return null;
	}

	/**
	 * Do best effort to parse the display name of a locale. Optionally, give languages the display name might possibly
	 * be written in. If not given, the default locale and some other common locales are checked (german, english,
	 * french, italian, chinese, japanese, korean), so no need to specify displayNameLanguagesToCheck, if you expect the
	 * display language in one of these locales.<br>
	 * Be aware that for some rare languages, some rare locales might have the same display name. In these cases, we
	 * return a random locale of all matching locales.
	 *
	 * @param displayName                 the display name to be parsed
	 * @param displayNameLanguagesToCheck the languages the display name might be written in
	 * @return the locale for the given locale display name or null, if no matching locale can be found
	 */
	@Nullable
	public static Locale parseLocalesByDisplayName(String displayName, Locale... displayNameLanguagesToCheck) {
		List<Locale> localesToCheck = displayNameLanguagesToCheck.length == 0
				? DEFAULT_LOCALES
				: Arrays.asList(displayNameLanguagesToCheck);
		for (Locale localeToCheck : localesToCheck) {
			Set<Locale> mappedLocale = DISPLAY_NAME_TO_LOCALE
					.computeIfAbsent(localeToCheck, Strings::getDisplayNameMappingForLocale)
					.get(displayName.toLowerCase(localeToCheck));
			if (mappedLocale != null) {
				return mappedLocale.iterator().next();
			}
		}

		// fallback
		return parseLocale(displayName);
	}

	@NotNull
	private static Map<String, Set<Locale>> getDisplayNameMappingForLocale(Locale localeToCheck) {
		Map<String, Set<Locale>> map = new HashMap<>();
		for (Locale locale : AVAILABLE_LOCALES) {
			map.computeIfAbsent(locale.getDisplayName(localeToCheck)
					.toLowerCase(localeToCheck), k2 -> new MinimizedHashSet<>()).add(locale);
		}
		return map;
	}

	/**
	 * Parses a percentage or a fraction value. It returns the parsed value. If the specified text ends with a % sign,
	 * the parsed value before the % sign is divided by 100, so "95%" will return as 0.95.
	 *
	 * @param percentage string contains a floating point number or a percentage string
	 * @return the value of the floating point number, including % interpretation
	 * @throws NumberFormatException          if it is not a valid number format
	 * @throws java.lang.NullPointerException if the specified argument is null
	 * @see Double#parseDouble(String)
	 */
	public static double parsePercentage(String percentage) throws NumberFormatException {
		String number = Strings.trim(percentage);
		double divisor = 1.0;
		if (number.endsWith("%")) {
			number = Strings.trim(number.substring(0, number.length() - 1));
			divisor = 100.0;
		}
		return Double.parseDouble(number) / divisor;
	}

	/**
	 * Returns the enum constant referenced by the specified enum name. This method is very similar to T.value(name),
	 * despite that it is case insensitive. If the specified name cannot be matched to a enum constant of the specified
	 * enum type, null is returned. This method never throws an exception.
	 *
	 * @param name     the name of the enum constant
	 * @param enumType the type of the enum
	 * @return the enum constant found case insensitive
	 * @created 26.01.2014
	 */
	public static <T extends Enum<T>> T parseEnum(String name, Class<T> enumType) {
		return parseEnum(name, enumType, null);
	}

	/**
	 * Returns the enum constant referenced by the specified enum name. This method is very similar to T.value(name),
	 * except that it is case insensitive and provides the capability to specify a default value. The default value is
	 * used every time the specified name cannot be matched to a enum constant of the specified enum type. Therefore
	 * this method always returns a valid enum constant, even if the name is null.
	 * <p>
	 * Please not that null as a default value is not allowed. In this case use the method {@link #parseEnum(String,
	 * Class)}, because this method is not capable to handle null.
	 *
	 * @param name         the name of the enum constant
	 * @param defaultValue the default enum constant to be used if the name does not match a specific enum constant
	 * @return the enum constant found case insensitive
	 * @throws NullPointerException if the default value is null
	 * @created 26.01.2014
	 */
	public static <T extends Enum<T>> T parseEnum(String name, T defaultValue) {
		return parseEnum(name, defaultValue.getClass(), defaultValue);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Enum<T>> T parseEnum(String name, @SuppressWarnings("rawtypes") Class enumType, T defaultValue) {
		if (isBlank(name)) return defaultValue;
		try {
			return (T) Enum.valueOf(enumType, name);
		}
		catch (Exception e) {
			// as a fallback, try to find name case insensitive
			for (T t : ((T[]) enumType.getEnumConstants())) {
				if (t.name().equalsIgnoreCase(name)) {
					return t;
				}
			}
		}

		// otherwise use default value
		LOGGER.warn("cannot parse value '" + name + "' of enumeration " + enumType);
		return defaultValue;
	}

	/**
	 * Returns the names of the specified enumeration values as an array in the same order as the enums are specified.
	 *
	 * @param enums the enum values for which the names shall be returned
	 * @return the names of the enums
	 * @see Enum#name()
	 */
	public static String[] names(Enum<?>... enums) {
		String[] result = new String[enums.length];
		int index = 0;
		for (Enum<?> e : enums) {
			result[index++] = e.name();
		}
		return result;
	}

	/**
	 * Determines whether the given string ends with the end character being not escaped by backslash.
	 *
	 * @param text the text to be checked
	 * @param end  the expected end character
	 * @return if the expected end character is there and is being escaped
	 * @created 02.12.2013
	 */
	public static boolean endsWithUnescaped(String text, char end) {
		return text.length() >= 2
				&& text.charAt(text.length() - 1) == end
				&& text.charAt(text.length() - 2) != '\\';
	}

	/**
	 * Returns the stack trace of a specified exception as a newly created String object. If the exception is null, null
	 * is returned.
	 *
	 * @param e the exception to get the stack trace for
	 * @return the stack trace of the exception
	 * @created 19.02.2014
	 */
	public static String getStackTrace(Throwable e) {
		return getStackTrace(e, Integer.MAX_VALUE);
	}

	/**
	 * Returns the stack trace of a specified exception as a newly created String object. If the exception is null, null
	 * is returned.
	 *
	 * @param e the exception to get the stack trace for
	 * @return the stack trace of the exception
	 * @created 19.02.2014
	 */
	public static String getStackTrace(Throwable e, int maxDepth) {
		if (e == null) return null;
		StringWriter buffer = new StringWriter();
		PrintWriter print = new PrintWriter(buffer);
		e.printStackTrace(print);
		print.flush();
		String trace = buffer.toString();
		if (maxDepth >= 0 && maxDepth < Integer.MAX_VALUE) {
			int endIndex = ordinalIndexOf(trace, "\n", maxDepth);
			if (endIndex > 0) {
				trace = trace.substring(0, endIndex);
			}
		}
		return trace;
	}

	/**
	 * Finds the ordinal or nth index of the given string in the given text. Note that the first occurrence is found by
	 * <p>
	 * Implementation from user <i>aioobe</i> from <a
	 * href="http://stackoverflow.com/questions/3976616/how-to-find-nth-occurrence-of-character-in-a-string/3976656#3976656">stackoverflow.com</a>
	 *
	 * @param text   the text in which to find the index of the given string
	 * @param string the string to find in the given text
	 * @param n      the ordinal of the occurrence to find
	 * @return the index of the nth occurrence of the given string in the given text
	 */
	public static int ordinalIndexOf(String text, String string, int n) {
		int pos = text.indexOf(string);
		while (n-- > 0 && pos != -1) {
			pos = text.indexOf(string, pos + 1);
		}
		return pos;
	}

	public static String getDurationVerbalization(long timeMillis) {
		return getDurationVerbalization(timeMillis, false);
	}

	public static String getDurationVerbalization(long timeMillis, boolean longVersion) {
		if (timeMillis == 0) return "0" + getTimeUnit(1, longVersion, true);
		String t;
		if (timeMillis < 0) {
			t = "-";
			timeMillis *= -1;
		}
		else {
			t = "";
		}
		for (int i = TIME_FACTORS.length - 1; i >= 0; i--) {
			long factor = TIME_FACTORS[i];
			long amount = (timeMillis / factor);
			if (amount >= 1) {
				if (!t.replace("-", "").isEmpty()) t += " ";
				t += amount + getTimeUnit(i, longVersion, amount > 1);
				timeMillis -= amount * factor;
			}
		}
		return t;
	}

	/**
	 * Serializes a date object into a string using a standardized format. Can be un-serialized using {@link
	 * #readDate(String)}.
	 * <p>
	 * This should be used to write dates for persistence.
	 *
	 * @param date date to serialize
	 */
	public static String writeDate(Date date) {
		synchronized (DATE_FORMAT) {
			return DATE_FORMAT.format(date);
		}
	}

	/**
	 * Un-serializes a date string created by {@link #writeDate(Date)}.
	 *
	 * @param compatibilityFormat allows to provide an additional format which will be applied, if the default one
	 *                            fails. This allows to support persistence files that were written before the date
	 *                            verbalization was standardized.
	 */
	public static Date readDate(String dateString, SimpleDateFormat compatibilityFormat) throws ParseException {
		try {
			synchronized (DATE_FORMAT) {
				return DATE_FORMAT.parse(dateString);
			}
		}
		catch (ParseException e) {
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (compatibilityFormat) {
				return compatibilityFormat.parse(dateString);
			}
		}
	}

	private static Date parseDate(String dateString, SimpleDateFormat... allowedFormats) throws ParseException {
		for (SimpleDateFormat format : allowedFormats) {
			try {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (format) {
					return format.parse(dateString);
				}
			}
			catch (ParseException ignore) {
			}
		}
		throw new ParseException("no date format can used to parse: " + dateString, 0);
	}

	/**
	 * Serializes a date object into a string using a standardized format.
	 */
	public static Date readDate(String dateString) throws ParseException {
		return parseDate(dateString, DATE_FORMAT_COMPATIBILITY, DATE_FORMAT_NO_SECONDS, DATE_FORMAT_NO_TIME);
	}

	private static String getTimeUnit(int i, boolean longVersion, boolean plural) {
		return longVersion ? " " + TIME_UNITS_LONG[i] + (plural ? "s" : "") : TIME_UNITS[i];
	}

	private static class Group {
		private static final Logger LOGGER = LoggerFactory.getLogger(Group.class);
		final int start;
		final int end;

		public Group(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	/**
	 * Converts an array of (Unicode) integer codepoints to a corresponding String representation
	 *
	 * @param codePoints integer codepoints (Unicode)
	 * @return a String derived from the codepoints
	 */
	public static String fromCharCode(int... codePoints) {
		StringBuilder builder = new StringBuilder(codePoints.length);
		for (int codePoint : codePoints) {
			builder.append(Character.toChars(codePoint));
		}
		return builder.toString();
	}

	/**
	 * Checks whether a string contains only of numerals.
	 *
	 * @param string string to check
	 * @return true, iff the given string contains only numerals
	 */
	public static boolean isNumeral(String string) {
		return string.matches("^\\d+$");
	}

	/* Returns a string that contains the specified character "count" times.
	 *
	 * @param character     the char to be replicated
	 * @param count the number of times to replicate
	 * @return the replicated char
	 */
	public static String nTimes(char character, int count) {
		return nTimes(String.valueOf(character), count);
	}

	/**
	 * Returns a string that contains the specified text "count" times. If count is "0" or negative, or the specified
	 * text is null, an empty string is returned.
	 *
	 * @param text  the text to be replicated
	 * @param count the number of times to replicate
	 * @return the replicated text
	 */
	public static String nTimes(String text, int count) {
		if (count <= 0 || text == null) return "";
		if (count == 1) return text;
		return text.repeat(count);
	}

	/**
	 * Returns a string of the specified text, that has been filled the specified char at its left side, unit the
	 * specified target length is reached. If the string already reached or exceeds the specified target length, the
	 * original string is returned.
	 *
	 * @param text         the right-side text to be filled on its left side
	 * @param fillChar     the char to be filled
	 * @param targetLength the target length to be filled
	 * @return the filled string of at least the specified target length
	 */
	public static String fillLeft(String text, char fillChar, int targetLength) {
		return fill(text, fillChar, targetLength, true);
	}

	/**
	 * Returns a string of the specified text, that has been filled the specified char at its right side, unit the
	 * specified target length is reached. If the string already reached or exceeds the specified target length, the
	 * original string is returned.
	 *
	 * @param text         the left-side text to be filled on its right side
	 * @param fillChar     the char to be filled
	 * @param targetLength the target length to be filled
	 * @return the filled string of at least the specified target length
	 */
	public static String fillRight(String text, char fillChar, int targetLength) {
		return fill(text, fillChar, targetLength, false);
	}

	private static String fill(String text, char fillChar, int targetLength, boolean fillLeft) {
		if (text.length() >= targetLength) return text;
		StringBuilder builder = new StringBuilder(targetLength);
		if (!fillLeft) builder.append(text);
		builder.append(String.valueOf(fillChar).repeat(targetLength - text.length()));
		if (fillLeft) builder.append(text);
		return builder.toString();
	}

	/**
	 * Returns the specified integer as a 8-digit-hex representation. The returned hex string is in upper case and of a
	 * fixed with of 8 characters (filled with leading "0" as required). Negative numbers are returned in their
	 * complement representation.
	 *
	 * @param number the number to be converted to hex String
	 * @return the 8-character string of uppercase hex digits
	 */
	public static String toHex8(int number) {
		return String.format("%08X", (0xFFFFFFFFL & number));
	}

	/**
	 * Returns a clean string which can be parsed as a decimal number. Removes all irregular dots and commas without
	 * losing the decimal part.
	 *
	 * @param s the String which should be cleaned
	 * @return the cleaned String, which could be parsed as decimal
	 */
	public static String cleanNumeral(String s) {
		if (s == null || s.length() < 4) {
			return s;
		}
		int commaIndex = s.length() - 3;
		String comma = String.valueOf(s.charAt(commaIndex));
		String decimal = "";
		if (Strings.isNumeral(comma)) {
			commaIndex = s.length();
		}
		else {
			decimal = "." + s.substring(commaIndex + 1);
		}
		String integer = s.substring(0, commaIndex);
		String integerCleaned = integer.replaceAll("[.,]", "");
		return integerCleaned + decimal;
	}

	@NotNull
	public static String getBundleStringAsUTF8(ResourceBundle bundle, String key) throws MissingResourceException {
		String text = bundle.getString(key);
		if (Java.getVersion() < 9) {
			// before Java 9, property files are read with ISO-8859-1, after they are read with UTF-8
			text = new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		}
		return text;
	}
}
