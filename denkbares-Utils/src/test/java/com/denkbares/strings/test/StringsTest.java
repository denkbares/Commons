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
package com.denkbares.strings.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.denkbares.strings.QuoteSet;
import com.denkbares.strings.StringFragment;
import com.denkbares.strings.Strings;

import static org.junit.Assert.*;

/**
 * This test does only test methods which are not used very frequently and are therefore not tested
 * by other tests already (like Headless-App-Tests).
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 18.04.2013
 */
public class StringsTest {

	@Test
	public void concat() {
		assertEquals("a#b#c", Strings.concat("#", Arrays.asList("a", "b", "c")));
		assertEquals("a#c", Strings.concat("#", Arrays.asList("a", null, "c")));
		assertEquals("", Strings.concat("#", Arrays.asList(null, null)));
		assertEquals("", Strings.concat("#", (Collection<?>) null));
		assertEquals("", Strings.concat("#", (Object[]) null));
	}

	@Test
	public void endsWithIgnoreCase() {
		assertTrue(Strings.endsWithIgnoreCase("prefixsuffix", "suffix"));
		assertTrue(Strings.endsWithIgnoreCase("prefixsuffix", "suFFix"));
	}

	@Test
	public void getFirstNonEmptyLineContent() {
		assertEquals(" lineone",
				Strings.getFirstNonEmptyLineContent("\n\n  \n lineone\n\nlinetwo\n").getContent());
	}

	@Test
	public void getLineFragmentation() {
		assertEquals(
				Arrays.asList("", "", "  ", " lineone", "", "linetwo").toString(),
				Strings.getLineFragmentation("\n\n  \n lineone\n\nlinetwo\n").toString());
	}

	@Test
	public void splitUnquoted() {
		assertEquals(
				Arrays.asList("", "", "  ", " word1", "\"word.2\"", "word3", "").toString(),
				Strings.splitUnquoted("..  . word1.\"word.2\".word3.", ".").toString());
	}

	@Test
	public void splitUnquotedMulti() {
		QuoteSet[] quotes = {
				new QuoteSet('"'), new QuoteSet('(', ')') };
		assertEquals(
				Arrays.asList("a", "\"literal mit Klammer. (xy\"", "(A.2)").toString(),
				Strings.splitUnquoted("a.\"literal mit Klammer. (xy\".(A.2)", ".",
						quotes).toString());
	}

	@Test
	public void splitUnquotedTripleQuotes() {
		QuoteSet[] quotes = { QuoteSet.TRIPLE_QUOTES };
		assertEquals(Collections.singletonList("sometext \"\"\"moretext; even more\"\"\" what, there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"\"moretext; even more\"\"\" what, there is still more?", ";", quotes)
						.toString());

		assertEquals(Collections.singletonList("\"\"\"sometext moretext; even more, what, there is still more?\"\"\"")
						.toString(),
				Strings.splitUnquoted("\"\"\"sometext moretext; even more, what, there is still more?\"\"\"", ";", quotes)
						.toString());

		assertEquals(Arrays.asList("sometext \"\"moretext", " even more\"\"\" what, there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"moretext; even more\"\"\" what, there is still more?", ";", quotes)
						.toString());

		assertEquals(Arrays.asList("sometext \"\"\"moretext; even more\"\"\" what", "there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"\"moretext; even more\"\"\" what; there is still more?", "; ", quotes)
						.toString());

		quotes = new QuoteSet[] { new QuoteSet('"'), QuoteSet.TRIPLE_QUOTES, };
		assertEquals(Arrays.asList("sometext \"\"\"moretext; even more\"\"\" what", "there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"\"moretext; even more\"\"\" what; there is still more?", "; ", quotes)
						.toString());

		assertEquals(Arrays.asList("sometext \"\"\"moretext; even\" more\"\"\" what", "there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"\"moretext; even\" more\"\"\" what; there is still more?", "; ", quotes)
						.toString());

		assertEquals(Collections.singletonList("sometext \"\"\"moretext; even\" more\"\"\" what\"; \"there is still more?")
						.toString(),
				Strings.splitUnquoted("sometext \"\"\"moretext; even\" more\"\"\" what\"; \"there is still more?", "; ", quotes)
						.toString());
	}

	@Test
	public void isBlank() {
		assertTrue(Strings.isBlank(null));
		assertTrue(Strings.isBlank(""));
		assertTrue(Strings.isBlank(" "));
		assertTrue(Strings.isBlank(" \n \r\n  \n"));
		assertFalse(Strings.isNotBlank(" \n \r\n  \n"));
	}

	@Test
	public void isUnescapedQuoted() {
		String text = "x\\'\\\"'\"";
		assertFalse(Strings.isUnEscapedQuote(text, 0, '"', '\''));
		assertFalse(Strings.isUnEscapedQuote(text, 1, '"', '\''));
		assertFalse(Strings.isUnEscapedQuote(text, 2, '"', '\''));
		assertFalse(Strings.isUnEscapedQuote(text, 3, '"', '\''));
		assertFalse(Strings.isUnEscapedQuote(text, 4, '"', '\''));
		assertTrue(Strings.isUnEscapedQuote(text, 5, '"', '\''));
		assertTrue(Strings.isUnEscapedQuote(text, 6, '"', '\''));
	}

	@Test
	public void isQuotedIndex() {
		String text = "012\"456\"890123\"5678\\\"1234567\"9";
		assertFalse(Strings.isQuoted(text, 0));
		assertFalse(Strings.isQuoted(text, 1));
		assertFalse(Strings.isQuoted(text, 2));
		assertTrue(Strings.isQuoted(text, 3));
		assertTrue(Strings.isQuoted(text, 4));
		assertTrue(Strings.isQuoted(text, 6));
		assertTrue(Strings.isQuoted(text, 7));
		assertFalse(Strings.isQuoted(text, 8));
		assertFalse(Strings.isQuoted(text, 13));
		assertTrue(Strings.isQuoted(text, 14));
		assertTrue(Strings.isQuoted(text, 15));
		assertTrue(Strings.isQuoted(text, 18));
		assertTrue(Strings.isQuoted(text, 19));
		assertTrue(Strings.isQuoted(text, 20));
		assertTrue(Strings.isQuoted(text, 21));
		assertTrue(Strings.isQuoted(text, 27));
		assertTrue(Strings.isQuoted(text, 28));
		assertFalse(Strings.isQuoted(text, 29));
	}

	@Test(expected = IllegalArgumentException.class)
	public void isQuotedIndexException1() {
		assertFalse(Strings.isQuoted("123", -1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void isQuotedIndexException2() {
		assertFalse(Strings.isQuoted("123", 3));
	}

	@Test
	public void isQuoted() {
		assertTrue(Strings.isQuoted("\"abc\""));
		assertFalse(Strings.isQuoted("\"a\"bc\""));
		assertFalse(Strings.isQuoted("\"a\"bc\""));
		assertFalse(Strings.isQuoted("\\\"abc\""));
		assertFalse(Strings.isQuoted("\"abc\\\""));
		assertFalse(Strings.isQuoted("\"abc\"defg\"abc\""));
		assertFalse(Strings.isQuoted(" \"abc\""));
		assertFalse(Strings.isQuoted(" \"abc\""));
	}

	@Test
	public void replaceUmlaut() {
		assertEquals("AEOEUEaeoeuess", Strings.replaceUmlaut("ÄÖÜäöüß"));
	}

	@Test
	public void parseLocale() {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale locale : locales) {
			if (locale.toString().contains("#")) continue;
			assertEquals(locale, Strings.parseLocale(locale.toString()));
		}
	}

	@Test
	public void stackTrace() {
		String expected = "java.lang.NullPointerException: test";
		assertTrue(Strings.stackTrace(new NullPointerException("test")).startsWith(expected));
	}

	@Test
	public void startsWithIgnoreCase() {
		assertTrue(Strings.startsWithIgnoreCase("prefixsuffix", "prefix"));
		assertTrue(Strings.startsWithIgnoreCase("prefixsuffix", "PRefix"));
	}

	@Test
	public void unqoute() {
		assertEquals("ab\\as", Strings.unquote("\"ab\\as\""));
		assertEquals("ab\"c", Strings.unquote("\"ab\\\"c\""));
		assertEquals("ab\\c", Strings.unquote("\"ab\\\\c\""));
		assertEquals("", Strings.unquote("\""));
		assertNull(Strings.unquote(null));

		assertEquals("foo", Strings.unquote("\"foo\"", '"', '\''));
		assertEquals("foo", Strings.unquote("'foo'", '"', '\''));
		assertEquals("'foo", Strings.unquote("'foo", '"', '\''));
		assertEquals("'foo'", Strings.unquote("'foo'", '"', '#'));
	}

	@Test
	public void encodeHTML() {
		assertNull(Strings.encodeHtml(null));
		assertEquals("abc&amp;&quot;&lt;&gt;&#35;&#92;def", Strings.encodeHtml("abc&\"<>#\\def"));
	}

	@Test
	public void encodeURL() throws URISyntaxException {
		assertNull(Strings.encodeURL(null));
		assertEquals("auf+den%2FFu%C3%9F.txt", Strings.encodeURL("auf den/Fuß.txt"));
	}

	@Test
	public void writeReadFile() throws IOException {
		String testString = "abcdefghijklmnopqrstuvwqyzöäüß";
		String filePath = "target/testfile.txt";
		Strings.writeFile(filePath, testString);
		assertEquals(testString, Strings.readFile(filePath));
		assertEquals(testString, Strings.readStream(new FileInputStream(filePath)));
	}

	@Test
	public void writeReadFile2() throws IOException {
		String testString = "abcdefghijklmnopqrstuvwqyzöäüß";
		String filePath = "target/testfile.txt";
		Strings.writeFile(new File(filePath), testString);
		assertEquals(testString, Strings.readFile(new File(filePath)));
	}

	@Test
	public void indexOfQuotes() {
		assertEquals(0, Strings.indexOf("\"test\"", Strings.UNQUOTED, "\""));
		assertEquals(5, Strings.indexOf("\"test\"", 1, Strings.UNQUOTED, "\""));
		assertEquals(7, Strings.indexOf("\"test\\\"\"", 1, Strings.UNQUOTED, "\""));
		assertEquals(7, Strings.indexOf("\"test\\\"\"", 6, Strings.UNQUOTED, "\""));

		int singleQuoteUnQuoted = Strings.UNQUOTED + Strings.SINGLE_QUOTED;
		assertEquals(-1, Strings.indexOf("\"test\"", singleQuoteUnQuoted, "'"));
		assertEquals(0, Strings.indexOf("'test'", singleQuoteUnQuoted, "'"));
		assertEquals(5, Strings.indexOf("'test'", 1, singleQuoteUnQuoted, "'"));
		assertEquals(7, Strings.indexOf("'test\\''", 1, singleQuoteUnQuoted, "'"));
		assertEquals(7, Strings.indexOf("'test\\''", 6, singleQuoteUnQuoted, "'"));
	}

	@Test
	public void indexOf() {
		assertEquals(-1, Strings.indexOf("", "test"));
		assertEquals(0, Strings.indexOf("test", "test"));
		assertEquals(-1, Strings.indexOf("tes", "test"));
		assertEquals(3, Strings.indexOf("as\"test\"das", "test"));
		assertEquals(2, Strings.indexOf("astestdas", "test"));
		assertEquals(4, Strings.indexOf("as\\\"test\"das", "test"));
		assertEquals(2, Strings.indexOf("a\"test\"s\\\"test\"das", "test"));
		assertEquals(0, Strings.indexOf("a\"test\"s\\\"test\"das", "test", "a"));

		int unquoted = Strings.UNQUOTED;
		assertEquals(-1, Strings.indexOf("", unquoted, "test"));
		assertEquals(0, Strings.indexOf("test", unquoted, "test"));
		assertEquals(-1, Strings.indexOf("tes", unquoted, "test"));
		assertEquals(-1, Strings.indexOf("as\"test\"das", unquoted, "test"));
		assertEquals(2, Strings.indexOf("astestdas", unquoted, "test"));
		assertEquals(4, Strings.indexOf("as\\\"test\"das", unquoted, "test"));
		assertEquals(10, Strings.indexOf("a\"test\"s\\\"test\"das", unquoted, "test"));
		assertEquals(0, Strings.indexOf("a\"test\"s\\\"test\"das", unquoted, "test", "a"));

		int skipBraces = Strings.UNBRACED;
		assertEquals(-1, Strings.indexOf("", skipBraces, "test"));
		assertEquals(0, Strings.indexOf("test", skipBraces, "test"));
		assertEquals(-1, Strings.indexOf("tes", skipBraces, "test"));
		assertEquals(-1, Strings.indexOf("as(test)das", skipBraces, "test"));
		assertEquals(2, Strings.indexOf("astestdas", skipBraces, "test"));
		assertEquals(3, Strings.indexOf("as\"test\"das", skipBraces, "test"));
		assertEquals(9, Strings.indexOf("a(test)s)testdas", skipBraces, "test"));
		assertEquals(7, Strings.indexOf("b(test)as\\\"test\"das", skipBraces, "test", "a"));

		int skipComments = Strings.SKIP_COMMENTS;
		assertEquals(-1, Strings.indexOf("", skipComments, "test"));
		assertEquals(0, Strings.indexOf("test", skipComments, "test"));
		assertEquals(-1, Strings.indexOf("tes", skipComments, "test"));
		assertEquals(3, Strings.indexOf("as\"test\"das", skipComments, "test"));
		assertEquals(2, Strings.indexOf("astestdas", skipComments, "test"));
		assertEquals(4, Strings.indexOf("as\\\"test\"das", skipComments, "test"));
		assertEquals(2, Strings.indexOf("a\"test\"s\\\"test\"das", skipComments, "test"));
		assertEquals(0, Strings.indexOf("a\"test\"s\\\"test\"das", skipComments, "test", "a"));

		assertEquals(-1, Strings.indexOf("aste//stdas", skipComments, "test"));
		assertEquals(-1, Strings.indexOf("aste//hitestdas", skipComments, "test"));
		assertEquals(16, Strings.indexOf("aste//hitest\ndastest", skipComments, "test"));
		assertEquals(-1, Strings.indexOf("asas\"das//comm\"entestdas", skipComments, "test"));
		assertEquals(2, Strings.indexOf("a\"test\"sasd//comment\nasd\"test\"asdetesthoho", skipComments, "test"));
		assertEquals(26, Strings.indexOf("asasd//testcomment\nasdasdetesthoho", skipComments, "test"));
		assertEquals(21, Strings.indexOf("asasd//testcomment\nasdasdetesthoho", skipComments, "test", "das"));
		assertEquals(22, Strings.indexOf("asasd//testcomment\na\"sdasdetestho\"ho", skipComments, "test", "das"));

		int skipCommendsAndQuotes = skipComments | unquoted;
		assertEquals(-1, Strings.indexOf("", skipCommendsAndQuotes, "test"));
		assertEquals(0, Strings.indexOf("test", skipCommendsAndQuotes, "test"));
		assertEquals(-1, Strings.indexOf("tes", skipCommendsAndQuotes, "test"));
		assertEquals(-1, Strings.indexOf("as\"test\"das", skipCommendsAndQuotes, "test"));
		assertEquals(2, Strings.indexOf("astestdas", skipCommendsAndQuotes, "test"));
		assertEquals(4, Strings.indexOf("as\\\"test\"das", skipCommendsAndQuotes, "test"));
		assertEquals(10, Strings.indexOf("a\"test\"s\\\"test\"das", skipCommendsAndQuotes, "test"));
		assertEquals(0, Strings.indexOf("a\"test\"s\\\"test\"das", skipCommendsAndQuotes, "test", "a"));

		assertEquals(-1, Strings.indexOf("aste//stdas", skipCommendsAndQuotes, "test"));
		assertEquals(-1, Strings.indexOf("aste//hitestdas", skipCommendsAndQuotes, "test"));
		assertEquals(16, Strings.indexOf("aste//hitest\ndastest", skipCommendsAndQuotes, "test"));
		assertEquals(17, Strings.indexOf("asas\"das//comm\"entestdas", skipCommendsAndQuotes, "test"));
		assertEquals(34, Strings.indexOf("a\"test\"sasd//comment\nasd\"test\"asdetesthoho", skipCommendsAndQuotes, "test"));
		assertEquals(24, Strings.indexOf("a\"testsasd//comment\nasd\"test\"asdetesthoho", skipCommendsAndQuotes, "test"));
		assertEquals(32, Strings.indexOf("a\"test\"sasd//testcomment\nasdasdetesthoho", skipCommendsAndQuotes, "test"));

		assertEquals(27, Strings.lastIndexOf("asasd//testcomment\na\"sdasdetestho\"ho", skipComments, "test", "das"));
		assertEquals(16, Strings.lastIndexOf("a\"test\"s\\\"test\"das", skipComments, "test", "a"));
		assertEquals(2, Strings.lastIndexOf("a\"test\"s//\"test\"das", skipComments, "test", "a"));
		assertEquals(14, Strings.lastIndexOf("atests//test\ndas", skipCommendsAndQuotes, "test", "a"));
		assertEquals(14, Strings.lastIndexOf("atests//test\ndas\"testatest\"", skipCommendsAndQuotes, "test", "a"));
		assertEquals(14, Strings.lastIndexOf("atests//test\ndas//testatest", skipCommendsAndQuotes, "test", "a"));

		int skipCommendQuotesAndBraces = skipComments | unquoted | skipBraces;
		assertEquals(-1, Strings.indexOf("", skipCommendQuotesAndBraces, "test"));
		assertEquals(0, Strings.indexOf("test", skipCommendQuotesAndBraces, "test"));
		assertEquals(-1, Strings.indexOf("tes", skipCommendQuotesAndBraces, "test"));
		assertEquals(-1, Strings.indexOf("as\"test\"das", skipCommendQuotesAndBraces, "test"));
		assertEquals(2, Strings.indexOf("astestdas", skipCommendQuotesAndBraces, "test"));
		assertEquals(4, Strings.indexOf("as\\\"test\"das", skipCommendQuotesAndBraces, "test"));
		assertEquals(10, Strings.indexOf("a\"test\"s\\\"test\"das", skipCommendQuotesAndBraces, "test"));
		assertEquals(0, Strings.indexOf("a\"test\"s\\\"test\"das", skipCommendQuotesAndBraces, "test", "a"));

		assertEquals(-1, Strings.indexOf("aste//stdas", skipCommendQuotesAndBraces, "test"));
		assertEquals(-1, Strings.indexOf("aste//hitestdas", skipCommendQuotesAndBraces, "test"));
		assertEquals(16, Strings.indexOf("aste//hitest\ndastest", skipCommendQuotesAndBraces, "test"));
		assertEquals(17, Strings.indexOf("asas\"d(s//co)m\"entestdas", skipCommendQuotesAndBraces, "test"));
		assertEquals(34, Strings.indexOf("a(test)sasd//comment\nasd\"test\"asdetesthoho", skipCommendQuotesAndBraces, "test"));
		assertEquals(26, Strings.indexOf("a((testsasd//comment\nasd))test\"asdetesthoho", skipCommendQuotesAndBraces, "test"));
		assertEquals(32, Strings.indexOf("a\"test\"sasd//testcomment\nasdasdetesthoho", skipCommendQuotesAndBraces, "test"));
		assertEquals(26, Strings.indexOf("a((testsasd//comment\nasd))test\"asdetesthoho", skipCommendQuotesAndBraces, "test"));
		assertEquals(34, Strings.indexOf("a\"test\"sasd//test(\"comment\nasdasdetesthoho", skipCommendQuotesAndBraces, "test"));

		assertEquals(15, Strings.lastIndexOf("atests//t(est\ndas", skipCommendQuotesAndBraces, "test", "a"));
		assertEquals(16, Strings.lastIndexOf("atests//t\"(est\ndas\"testatest\"", skipCommendQuotesAndBraces, "test", "a"));
		assertEquals(14, Strings.lastIndexOf("atests//test\ndas//testatest", skipCommendQuotesAndBraces, "test", "a"));
	}

	@Test
	public void nestedQuotes() {
		List<StringFragment> fragments = Strings.splitUnquoted("\"\"\"Hi \"there \"stranger\"\"\"\"\";\"how are you\"", ";",
				true, QuoteSet.TRIPLE_QUOTES, new QuoteSet('"'));
		assertEquals(2, fragments.size());
		assertEquals("\"\"\"Hi \"there \"stranger\"\"\"\"\"", fragments.get(0).getContent());
		assertEquals("\"how are you\"", fragments.get(1).getContent());
	}

	@Test
	public void htmlToPlain() {
		String source = "<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"</head>\n" +
				"<body>\n" +
				"\n" +
				"<p>This is a paragraph.</p>\n" +
				"\n" +
				"<ul>\n" +
				"<li>item1<li>item2\n" +
				"</ul>\n" +
				"  And some text after." +
				"\n" +
				"</body>\n" +
				"</html>\n";
		assertEquals("This is a paragraph.\n* item1\n* item2\n\nAnd some text after.",
				Strings.htmlToPlain(source));
	}

	@Test
	public void fromCodePoint() {
		int charCode = 45;
		String stringFromCharCode = Strings.fromCharCode(charCode);
		assertEquals("-", stringFromCharCode);
	}

	@Test
	public void trimBlankLines() {
		String source = "  \n" +
				"  What is up?\n" +
				"\n" +
				"   \n" +
				"All is fine!\n" +
				"  \r\n  ";
		assertEquals("  What is up?\n" +
				"\n" +
				"   \n" +
				"All is fine!\n", Strings.trimBlankLines(source));
		assertEquals("  What is up?\n" +
				"\n" +
				"   \n" +
				"All is fine!", Strings.trimBlankLinesAndTrailingLineBreak(source));
	}

	@Test
	public void getStackTrace() {
		StringWriter buffer = new StringWriter();
		PrintWriter print = new PrintWriter(buffer);
		Exception e = new Exception();
		e.printStackTrace(print);
		print.flush();
		String stackTrace = Strings.getStackTrace(e);
		assertEquals(buffer.toString(), stackTrace);
	}

	@Test
	public void stringFragment() {
		StringFragment stringFragment = new StringFragment("Test", 1, "aTest");
		assertEquals("Test", stringFragment.getContent());
		assertEquals(5, stringFragment.getEnd());
		assertEquals(4, stringFragment.length());
		assertEquals("aTest", stringFragment.getFatherString());
	}

	@Test
	public void hex() {
		assertEquals("00000000", Strings.toHex8(0));
		assertEquals("00000001", Strings.toHex8(1));
		assertEquals("FFFFFFFF", Strings.toHex8(-1));
		assertEquals("FFFFFFFE", Strings.toHex8(-2));
	}

	@Test
	public void cleanNumeralTest() {
		String s = Strings.cleanNumeral("1.012,12");
		assertEquals("1012.12", s);

		String s1 = Strings.cleanNumeral("1,012,12");
		assertEquals("1012.12", s1);

		String s2 = Strings.cleanNumeral("1.012.12");
		assertEquals("1012.12", s2);

		String s3 = Strings.cleanNumeral("101.212");
		assertEquals("101212", s3);

		String s4 = Strings.cleanNumeral("101,212");
		assertEquals("101212", s4);
	}
}
