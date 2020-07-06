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

package com.denkbares.strings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.Nullable;

import static java.util.Spliterator.*;

/**
 * Utility methods to deal with locales.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 08.01.2015
 */
public class Locales {

	public static final Comparator<Locale> ASCENDING = (o1, o2) -> {
		if (o1 == o2) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		// most cases, locale is same for same language, so we take a shortcut here
		int lang = o1.getLanguage().compareTo(o2.getLanguage());
		if (lang != 0) return lang;
		if (o1.equals(o2)) return 0;

		// also check county, if still same use full string representation
		int country = o1.getCountry().compareTo(o2.getCountry());
		return (country == 0) ? String.valueOf(o1).compareTo(String.valueOf(o2)) : country;
	};

	/**
	 * Parses a locale from a locale string representation. This is the inverse method to {@link
	 * java.util.Locale#toString()}. If the specified text is null or cannot be parsed, null is returned. If the
	 * specified text is empty, the root locale is returned. Leading or trailing whitespaces will be ignored by this
	 * method.
	 *
	 * @param text the locale's text representation to be parsed
	 * @return the parsed locale
	 */
	public static Locale parseLocale(String text) {
		return Strings.parseLocale(text);
	}

	/**
	 * Returns the best matching locale out of a collection of available locales. It returns the ROOT locale if no
	 * locales matches the available locales, but the root locales is included. It returns the first locale of the
	 * specified available locales if neither any locale matches the preferred locale, not the ROOT locale is included.
	 * <p/>
	 * If the available locales are null or empty, null is returned.
	 *
	 * @param preferred the preferred locale to be used
	 * @param available the available locales
	 * @return the best matching locale
	 */
	public static Locale findBestLocale(Locale preferred, Locale... available) {
		return findBestLocale(preferred, Arrays.asList(available));
	}

	/**
	 * Returns the best matching locale out of a collection of available locales. It returns the ROOT locale if no
	 * locales matches the available locales, but the root locales is included. It returns the first locale of the
	 * specified available locales if neither any locale matches the preferred locale, nor the ROOT locale is included.
	 * <p/>
	 * If the available locales are null or empty, null is returned. Otherwise the method is guaranteed to return a
	 * locale instance out of the available ones.
	 *
	 * @param preferred the preferred locale to be used
	 * @param available the available locales
	 * @return the best matching locale
	 */
	public static Locale findBestLocale(Locale preferred, Collection<Locale> available) {
		// if no locales contained, return null (we cannot select one)
		if (available == null || available.isEmpty()) return null;

		Locale defaultLocale = available.iterator().next();
		return ((preferred != null) && (available.size() > 1))
				? findBestLocale(preferred, available, 1, defaultLocale)
				: defaultLocale;
	}

	/**
	 * Returns the best matching locale of the same language out of a collection of available locales. It returns the
	 * ROOT locale if no locales matches the available locales with at least the same language.
	 * <p/>
	 * If the available locales are null or empty, null is returned. Otherwise the method is guaranteed to either return
	 * a locale instance out of the available ones with the same language, or return the root locale (even if it is not
	 * in the available locales).
	 *
	 * @param preferred the preferred locale to be used
	 * @param available the available locales
	 * @return the best matching locale, granted to have at least the same language, or the root locale
	 */
	public static Locale findBestLocaleOfLanguage(Locale preferred, Collection<Locale> available) {
		// if no locales contained, return null (we cannot select one)
		if (available == null || available.isEmpty()) return null;
		// if preferred is null, return the default value
		if (preferred == null) return Locale.ROOT;
		// otherwise select best, at least with same language
		return findBestLocale(preferred, available, 100, Locale.ROOT);
	}

	/**
	 * Returns true if the specified locales are both null, or share the same language.
	 *
	 * @param locale1 the first locale to compare the language
	 * @param locale2 the second locale to compare the language
	 * @return if the languages of the two locales are equal
	 */
	public static boolean hasSameLanguage(Locale locale1, Locale locale2) {
		return locale1 == locale2 ||
				(locale1 != null && locale2 != null && Objects.equals(locale1.getLanguage(), locale2.getLanguage()));
	}

	/**
	 * Returns true if the specified locale has the specified language.
	 *
	 * @param locale   the locale to compare the language
	 * @param language the language to check for
	 * @return if the language of the locale matches
	 */
	public static boolean hasLanguage(Locale locale, String language) {
		return (locale != null) && locale.getLanguage().equalsIgnoreCase(language);
	}

	/**
	 * Returns true of the language of the specified locale is the german language, false otherwise.
	 *
	 * @param locale the locale to be tested
	 * @return if the locale is any kind of german
	 */
	public static boolean isGerman(@Nullable Locale locale) {
		return hasSameLanguage(locale, Locale.GERMAN);
	}

	/**
	 * Returns true of the language of the specified locale is the english language, false otherwise.
	 *
	 * @param locale the locale to be tested
	 * @return if the locale is any kind of english
	 */
	public static boolean isEnglish(@Nullable Locale locale) {
		return hasSameLanguage(locale, Locale.ENGLISH);
	}

	private static Locale findBestLocale(Locale preferred, Collection<Locale> available, int minScore, Locale defaultLocale) {
		// get locale if available
		if (available.contains(preferred)) return preferred;

		// otherwise try to find best locale
		Locale bestLocale = null;
		int bestScore = minScore - 1;
		for (Locale locale : available) {
			int score = rateMatch(preferred, locale);
			if (score > bestScore) {
				bestScore = score;
				bestLocale = locale;
			}
		}
		if (bestLocale != null) return bestLocale;

		// otherwise use default value
		return defaultLocale;
	}

	/**
	 * Returns the best matching locale out of a collection of available locales. The best matching locale is the first
	 * locale of the preferenceList that has at least a language match in the available locales; for that locale, the
	 * best matching one is selected out of the availables. The Method returns the ROOT locale if no locales matches the
	 * available locales, but the root locales is included. It returns the first locale of the specified available
	 * locales if neither any locale matches the preferred locale, nor the ROOT locale is included.
	 * <p/>
	 * If the available locales are null or empty, null is returned. Otherwise the method is guaranteed to return a
	 * locale instance out of the available ones. If the preferenceList is empty the ROOT locale is matched against the
	 * available locales.
	 *
	 * @param preferenceList the preferred locales to be used, order by their preference
	 * @param available      the available locales
	 * @return the best matching locale
	 */
	public static Locale findBestLocale(Collection<Locale> preferenceList, Collection<Locale> available) {
		// if no locales contained, return null (we cannot select one)
		if (available == null || available.isEmpty()) return null;
		if (available.size() == 1) return available.iterator().next();

		// search for first locale that has a language match
		// and use the best match for that locale
		for (Locale preferred : preferenceList) {
			Locale match = findBestLocale(preferred, available, 100, null);
			if (match != null) return match;
		}

		// otherwise try normal selection of the first preferred locale
		Locale first = preferenceList.isEmpty() ? Locale.ROOT : preferenceList.iterator().next();
		return findBestLocale(first, available);
	}

	/**
	 * Returns a iterator of the available locales, ordered by their preference as specified in the preference list. If
	 * the available locales are empty or null, the stream will be empty. For the order of the languages in the stream
	 * refer to {@link #findBestLocale(Collection, Collection)}, where the next stream element is always the best one,
	 * if the previous items of the stream where absent.
	 *
	 * @param preferenceList the preferred locales to be used
	 * @param available      the available locales
	 * @return a sequential {@code Stream} of the available languages
	 */
	public static Iterator<Locale> iterateByPreference(Collection<Locale> preferenceList, Collection<Locale> available) {
		if (available == null) return Collections.emptyIterator();
		Set<Locale> remaining = new LinkedHashSet<>(available);
		return new Iterator<Locale>() {
			@Override
			public boolean hasNext() {
				return !remaining.isEmpty();
			}

			@Override
			public Locale next() {
				if (remaining.isEmpty()) throw new NoSuchElementException();
				Locale bestLocale = findBestLocale(preferenceList, remaining);
				remaining.remove(bestLocale);
				return bestLocale;
			}
		};
	}

	/**
	 * Returns a sequential {@code Stream} of the available locales, ordered by their preference as specified in the
	 * preference list. If the available locales are empty or null, the stream will be empty. For the order of the
	 * languages in the stream refer to {@link #findBestLocale(Collection, Collection)}, where the next stream element
	 * is always the best one, if the previous items of the stream where absent.
	 *
	 * @param preferenceList the preferred locales to be used
	 * @param available      the available locales
	 * @return a sequential {@code Stream} of the available languages
	 */
	public static Stream<Locale> streamByPreference(Collection<Locale> preferenceList, Collection<Locale> available) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
				iterateByPreference(preferenceList, available),
				ORDERED | DISTINCT | NONNULL | IMMUTABLE), false);
	}

	/**
	 * Rates the similarity between the two specified locales with a number between 0 and 170.
	 */
	private static int rateMatch(Locale preferred, Locale available) {
		int score = 0;

		// score if same language or both empty language or available is more common (empty)
		String p1 = preferred.getLanguage();
		String a1 = available.getLanguage();
		if (p1.equals(a1)) {
			score += 100;
		}
		else if (a1.isEmpty()) {
			score += 10;
		}
		else {
			return score;
		}

		// score if same country or available is more common (empty country)
		String p2 = preferred.getCountry();
		String a2 = available.getCountry();
		if (!p2.isEmpty() && p2.equals(a2)) {
			score += 50;
		}
		else if (a2.isEmpty()) {
			score += 5;
		}
		else {
			return score;
		}

		// score if same variant or available is more common (empty variant)
		String p3 = preferred.getVariant();
		String a3 = available.getVariant();
		if (p3.equals(a3)) {
			score += 20;
		}
		else if (a3.isEmpty()) score += 2;

		return score;
	}

	/**
	 * Creates a String representation for a specified locale that can later on be parsed by the #parseLocale method.
	 * <p/>
	 * It is very similar to the {@link Locale#toString()} method, but does not produce an empty string for the default
	 * locale, instead "ROOT" is returned. Additionally, null is returned as "null".
	 *
	 * @param locale if the specified languages shall be sorted
	 * @return the parsable string
	 */
	public static String toParsableLocale(Locale locale) {
		String name = (locale == null) ? "null" : String.valueOf(locale);
		return Strings.isBlank(name) ? "ROOT" : name;
	}

	/**
	 * Creates a list of the languages that can later on be parsed by the #parseList method.
	 * <p/>
	 * It is very similar to the {@link Locale#toString()} method, but does not produce an empty string for the default
	 * locale, instead "ROOT" is returned. Additionally, null locales in the collection are returned as "null".
	 *
	 * @param languages the languages to represent as a parsable string
	 * @return the parsable string
	 */
	public static String toParsableList(Locale... languages) {
		return toParsableList(Arrays.asList(languages));
	}

	/**
	 * Creates a list of the languages that can later on be parsed by the #parseList method.
	 * <p/>
	 * It is very similar to the {@link Locale#toString()} method, but does not produce an empty string for the default
	 * locale, instead "ROOT" is returned. Additionally, null locales in the collection are returned as "null".
	 *
	 * @param languages the languages to represent as a parsable string
	 * @return the parsable string
	 */
	public static String toParsableList(Collection<Locale> languages) {
		if (languages == null) return "";
		StringBuilder result = new StringBuilder();
		for (Locale language : languages) {
			if (result.length() > 0) result.append(";");
			result.append(toParsableLocale(language));
		}
		return result.toString();
	}

	/**
	 * Creates a list of the languages that can later on be parsed by the #parseList method.
	 * <p/>
	 * It is very similar to the {@link Locale#toString()} method, but does not produce an empty string for the default
	 * locale, instead "ROOT" is returned. Additionally, null locales in the collection are returned as "null".
	 *
	 * @param sorted    if the specified languages shall be sorted
	 * @param languages the languages to represent as a parsable string
	 * @return the parsable string
	 */
	public static String toParsableList(boolean sorted, Collection<Locale> languages) {
		if (languages == null) return "";
		if (sorted) {
			languages = languages.stream().sorted(ASCENDING).collect(Collectors.toList());
		}
		return toParsableList(languages);
	}

	/**
	 * Creates a list of the languages that can later on be parsed by the #parseList method.
	 * <p/>
	 * It is very similar to the {@link Locale#toString()} method, but does not produce an empty string for the default
	 * locale, instead "ROOT" is returned. Additionally, null locales in the collection are returned as "null".
	 *
	 * @param sorted    if the specified languages shall be sorted
	 * @param languages the languages to represent as a parsable string
	 * @return the parsable string
	 */
	public static String toParsableList(boolean sorted, Locale... languages) {
		return toParsableList(sorted, Arrays.asList(languages));
	}

	/**
	 * Reads a representation of a set/list of languages. Each language is separated by ',' or ';'. The root language
	 * shall be represented by "ROOT". The returned set preserves the order of the entries read from the string.
	 *
	 * @param languages the string representation ot be read
	 * @return the languages read from the string representation
	 */
	public static Set<Locale> parseList(String languages) {
		if (Strings.isBlank(languages)) return Collections.emptySet();
		LinkedHashSet<Locale> result = new LinkedHashSet<>();
		for (String lang : languages.split("[;,]")) {
			result.add(parseLocale(lang));
		}
		return result;
	}

	/**
	 * Returns if the specified locale is null or the empty ROOT locale. If the locale specifies a well-defined language
	 * (with optional country/variant), false is returned.
	 *
	 * @param locale the locale to be checked
	 * @return if the locale does not denote a specific language
	 */
	public static boolean isEmpty(Locale locale) {
		return locale == null || Strings.isBlank(locale.getLanguage());
	}

	/**
	 * Searches the best matching language within the specified map, and returns the associated value. If the map is
	 * empty, null is returned. If the map is not empty, it is granted that any of the map's values is returned (which
	 * may be null, if there are any null values in the map).
	 *
	 * @param map      the map of possible values
	 * @param language the preferred language(s) to get the value for
	 * @return the value of the best matching language
	 */
	public static <E> E getBestValue(Map<Locale, ? extends E> map, Locale... language) {
		if (map == null) return null;
		return map.get(findBestLocale(Arrays.asList(language), map.keySet()));
	}

	/**
	 * Searches the best matching language within the specified map, and returns the associated value. If the map is
	 * empty or null, the specified default value is returned. If the map is not empty, it is granted that any of the
	 * map's values is returned (which may be null, if there are any null values in the map).
	 *
	 * @param map          the map of possible values
	 * @param language     the preferred language to get the value for
	 * @param defaultValue the default value to be returned if no item could be detected
	 * @return the value of the best matching language
	 */
	public static <E> E getBestValue(Map<Locale, E> map, Locale language, E defaultValue) {
		if (map == null) return defaultValue;
		return map.getOrDefault(findBestLocale(language, map.keySet()), defaultValue);
	}
}
