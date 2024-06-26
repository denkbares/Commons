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

package com.denkbares.utils.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.strings.Locales;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 08.01.2015
 */
public class LocalesTest {

	@Test
	public void bestMatch() {
		// match existing one
		Assert.assertEquals(Locale.GERMAN, Locales.findBestLocale(Locale.GERMAN,
				Locale.GERMAN, Locale.ENGLISH, Locale.ROOT));
		Assert.assertEquals(Locale.ENGLISH, Locales.findBestLocale(Locale.ENGLISH,
				Locale.GERMANY, Locale.ENGLISH, Locale.ROOT));
		Assert.assertEquals(Locale.ROOT, Locales.findBestLocale(Locale.ROOT,
				Locale.GERMANY, Locale.ENGLISH, Locale.ROOT));
		Locale platt = new Locale("de", "DE", "platt");
		Assert.assertEquals(Locale.GERMAN, Locales.findBestLocale(Locale.GERMAN,
				Locale.GERMAN, Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT));
		Assert.assertEquals(Locale.GERMANY, Locales.findBestLocale(Locale.GERMANY,
				Locale.GERMAN, Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT));

		// match same language
		Assert.assertEquals(Locale.GERMAN, Locales.findBestLocale(Locale.GERMANY,
				Locale.GERMAN, Locale.ENGLISH, Locale.ROOT));
		Assert.assertEquals(Locale.GERMANY, Locales.findBestLocale(Locale.GERMAN,
				Locale.GERMANY, Locale.ENGLISH, Locale.ROOT));

		// match root one if preferred is not contained, but root is
		Assert.assertEquals(Locale.ROOT, Locales.findBestLocale(Locale.CHINESE,
				Locale.GERMANY, Locale.ENGLISH, Locale.ROOT));

		// match first one if preferred is not contained, and root is also not
		Assert.assertEquals(Locale.GERMANY, Locales.findBestLocale(Locale.CHINESE,
				Locale.GERMANY, Locale.ENGLISH));

		// match best of language
		Assert.assertEquals(Locale.GERMAN, Locales.findBestLocaleOfLanguage(Locale.GERMAN,
				Arrays.asList(Locale.GERMAN, Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(Locale.GERMANY, Locales.findBestLocaleOfLanguage(Locale.GERMAN,
				Arrays.asList(Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(platt, Locales.findBestLocaleOfLanguage(Locale.GERMAN,
				Arrays.asList(platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(Locale.ROOT, Locales.findBestLocaleOfLanguage(Locale.GERMAN,
				Arrays.asList(Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(Locale.ROOT, Locales.findBestLocaleOfLanguage(Locale.GERMAN,
				Collections.singletonList(Locale.ENGLISH)));
		Assert.assertEquals(null, Locales.findBestLocaleOfLanguage(Locale.GERMAN, Collections.emptyList()));
		Assert.assertEquals(null, Locales.findBestLocaleOfLanguage(Locale.GERMAN, null));
		Assert.assertEquals(null, Locales.findBestLocaleOfLanguage(null, null));

		// match best
		Assert.assertEquals(Locale.GERMAN, Locales.findBestLocale(
				Arrays.asList(Locale.GERMAN, Locale.ENGLISH),
				Arrays.asList(Locale.GERMAN, Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(Locale.GERMANY, Locales.findBestLocale(
				Arrays.asList(Locale.GERMAN, Locale.ENGLISH),
				Arrays.asList(Locale.GERMANY, platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(platt, Locales.findBestLocale(
				Arrays.asList(Locale.GERMAN, Locale.ENGLISH),
				Arrays.asList(platt, Locale.ENGLISH, Locale.ROOT)));
		Assert.assertEquals(Locale.ENGLISH, Locales.findBestLocale(
				Arrays.asList(Locale.GERMAN, Locale.ENGLISH),
				Collections.singletonList(Locale.ENGLISH)));
		Assert.assertEquals(null, Locales.findBestLocale(Locale.GERMAN, Collections.emptyList()));
		Assert.assertEquals(null, Locales.findBestLocale(Locale.GERMAN, (Collection<Locale>) null));
		Assert.assertEquals(null, Locales.findBestLocale((List<Locale>) null, (Collection<Locale>) null));

		// iterate
		Iterator<Locale> localeIterator = Locales.iterateByPreference(Arrays.asList(Locale.GERMAN, Locale.ENGLISH),
				Arrays.asList(platt, Locale.ENGLISH, Locale.ROOT));
		assertTrue(localeIterator.hasNext());
		assertEquals(platt, localeIterator.next());
		assertTrue(localeIterator.hasNext());
		assertEquals(Locale.ENGLISH, localeIterator.next());
		assertTrue(localeIterator.hasNext());
		assertEquals(Locale.ROOT, localeIterator.next());
		assertFalse(localeIterator.hasNext());

	}

	@Test
	public void parseLocale() {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale locale : locales) {
			// TODO: java 6 does not support scripts, so Strings also does not. Remove this skip operation when migrate to java 7
			if (locale.toString().contains("#")) continue;
			assertEquals(locale, Locales.parseLocale(locale.toString()));
			assertEquals(locale, Locales.parseLocale(Locales.toParsableLocale(locale)));
		}
	}

	@Test
	public void parseLocaleList() {
		Locale[] locales = { Locale.GERMANY, Locale.CHINESE, Locale.ROOT, null,
				Locale.GERMAN, Locale.GERMANY, Locale.ENGLISH };

		// try coding / encoding cycle
		String coded = Locales.toParsableList(locales);
		Iterator<Locale> iterator = Locales.parseList(coded).iterator();
		assertEquals(Locale.GERMANY, iterator.next());
		assertEquals(Locale.CHINESE, iterator.next());
		assertEquals(Locale.ROOT, iterator.next());
		assertEquals(null, iterator.next());
		assertEquals(Locale.GERMAN, iterator.next());
		assertEquals(Locale.ENGLISH, iterator.next());
		assertFalse(iterator.hasNext());

		// try coding / encoding cycle with sorting
		coded = Locales.toParsableList(true, locales);
		iterator = Locales.parseList(coded).iterator();
		assertEquals(null, iterator.next());
		assertEquals(Locale.ROOT, iterator.next());
		assertEquals(Locale.GERMAN, iterator.next());
		assertEquals(Locale.GERMANY, iterator.next());
		assertEquals(Locale.ENGLISH, iterator.next());
		assertEquals(Locale.CHINESE, iterator.next());
		assertFalse(iterator.hasNext());
	}
}
