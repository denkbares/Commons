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

package com.denkbares.semanticcore.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MultiMap;
import com.denkbares.semanticcore.CachedTupleQueryResult;
import com.denkbares.semanticcore.ClosableTupleQueryResult;
import com.denkbares.strings.Strings;
import com.denkbares.strings.Text;
import com.denkbares.utils.Log;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 02.04.2015
 */
public class Sparqls {

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a language-tagged text. If no language
	 * is specified, the root language is returned. If the column does not exists or no value is available, null is
	 * returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the text or null
	 */
	public static Text asText(BindingSet bindings, String columnName) {
		return asText(bindings.getBinding(columnName));
	}

	/**
	 * Reads a value from a binding and returns it as a language-tagged text. If no language is specified, the root
	 * language is returned. If the binding is null, null is returned.
	 *
	 * @param binding the binding of the sparql result row
	 * @return the text or null
	 */
	public static Text asText(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		return RDFUtils.create(value);
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a plain string. If the column does not
	 * exists or no value is available, null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the plain string or null
	 */
	@Contract("_, null -> fail")
	public static String asString(BindingSet bindings, String columnName) {
		assert !columnName.startsWith("?");
		return asString(bindings.getBinding(columnName));
	}

	/**
	 * Reads a value from a binding and returns it as a plain string. If the binding is null, null is returned.
	 *
	 * @param binding the binding of the sparql result row
	 * @return the plain string or null
	 */
	public static String asString(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		if (value == null) {
			return null;
		}
		return value.stringValue();
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a float number. If the column does not
	 * exists or no value is available, null is returned. If there is a value, but it cannot be parsed as a number null
	 * is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the number value or null
	 */
	public static Float asFloat(BindingSet bindings, String columnName) {
		return asFloat(bindings.getBinding(columnName));
	}

	/**
	 * Reads a value from a binding and returns it as a float number. If the binding is null, null is returned. If there
	 * is a value, but it cannot be parsed as a number null is returned.
	 *
	 * @param binding the bindings of the sparql result row
	 * @return the number value or null
	 */
	public static Float asFloat(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		return asFloat(value);
	}

	/**
	 * Reads a value from a binding and returns it as a float number. If the value is null, null is returned. If there
	 * is a value, but it cannot be parsed as a number null is returned.
	 *
	 * @param value the bindings of the sparql result row
	 * @return the number value or null
	 */
	public static Float asFloat(Value value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Literal) {
			try {
				return ((Literal) value).floatValue();
			}
			catch (NumberFormatException ignore) {
			}
		}
		try {
			return Float.parseFloat(value.stringValue());
		}
		catch (NumberFormatException e) {
			Log.warning("cannot parse binding of '" + value + "' as float");
			return null;
		}
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a float number. If the column does not
	 * exists or no value is available, the specified default value is returned. If there is a value, but it cannot be
	 * parsed as a number the specified default value is returned.
	 *
	 * @param bindings     the bindings of the sparql result row
	 * @param columnName   the column name to read from
	 * @param defaultValue the default value
	 * @return the number value or null
	 */
	public static float asFloat(BindingSet bindings, String columnName, float defaultValue) {
		Float result = asFloat(bindings, columnName);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding returns it as a float number. If the binding is null, the specified default value is
	 * returned. If there is a value, but it cannot be parsed as a number the specified default value is returned.
	 *
	 * @param binding      the binding of the sparql result row
	 * @param defaultValue the default value
	 * @return the number value or null
	 */
	public static float asFloat(Binding binding, float defaultValue) {
		Float result = asFloat(binding);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a Date if it is a date literal.
	 *
	 * @param binding the value binding
	 * @return the LocalDate or null
	 */
	public static LocalDate asDate(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		if (value == null) {
			return null;
		}
		if (value instanceof Literal) {
			final String valueString = ((Literal) value).getLabel();
			final IRI datatype = ((Literal) value).getDatatype();
			if ("http://www.w3.org/2001/XMLSchema#dateTime".equals(datatype.toString())) {
				return LocalDate.parse(valueString, DateTimeFormatter.ISO_LOCAL_DATE);
			}
			else {
				// we still try to parse...
				return LocalDate.parse(valueString, DateTimeFormatter.ISO_LOCAL_DATE);
			}
		}
		return null;
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as an integer number. If the column does
	 * not exists or no value is available, null is returned. If there is a value, but it cannot be parsed as a number
	 * null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the number value or null
	 */
	public static Integer asInteger(BindingSet bindings, String columnName) {
		Binding binding = bindings.getBinding(columnName);
		return asInteger(binding);
	}

	/**
	 * Reads a value from a binding and returns it as an integer number. If the binding is null, null is returned. If
	 * there is a value, but it cannot be parsed as a number null is returned.
	 *
	 * @param binding the bindings of the sparql result row
	 * @return the number value or null
	 */
	public static Integer asInteger(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		return asInteger(value);
	}

	/**
	 * Reads a value from a binding and returns it as an integer number. If the binding is null, null is returned. If
	 * there is a value, but it cannot be parsed as a number null is returned.
	 *
	 * @param value the bindings of the sparql result row
	 * @return the number value or null
	 */
	public static Integer asInteger(Value value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Literal) {
			try {
				return ((Literal) value).intValue();
			}
			catch (NumberFormatException ignore) {
			}
		}
		try {
			return Integer.parseInt(value.stringValue());
		}
		catch (NumberFormatException e) {
			Log.warning("cannot parse binding of '" + value + "' as float");
			return null;
		}
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a integer number. If the column does
	 * not exists or no value is available, the specified default value is returned. If there is a value, but it cannot
	 * be parsed as a number the specified default value is returned.
	 *
	 * @param bindings     the bindings of the sparql result row
	 * @param columnName   the column name to read from
	 * @param defaultValue the default value
	 * @return the number value or null
	 */
	public static int asInteger(BindingSet bindings, String columnName, int defaultValue) {
		Integer result = asInteger(bindings, columnName);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding returns it as an integer number. If the binding is null, the specified default value
	 * is returned. If there is a value, but it cannot be parsed as a number the specified default value is returned.
	 *
	 * @param binding      the binding of the sparql result row
	 * @param defaultValue the default value
	 * @return the number value or null
	 */
	public static int asInteger(Binding binding, int defaultValue) {
		Integer result = asInteger(binding);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a boolean. If the column does not
	 * exists or no value is available, null is returned. If there is a value, but it cannot be parsed as a boolean null
	 * is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the boolean value or null
	 */
	public static Boolean asBoolean(BindingSet bindings, String columnName) {
		return asBoolean(bindings.getBinding(columnName));
	}

	/**
	 * Reads a value from a binding and returns it as a boolean. If the binding is null, null is returned. If there is a
	 * value, but it cannot be parsed as a boolean null is returned.
	 *
	 * @param binding the bindings of the sparql result row
	 * @return the boolean value or null
	 */
	public static Boolean asBoolean(Binding binding) {
		if (binding == null) {
			return null;
		}
		Value value = binding.getValue();
		return asBoolean(value);
	}

	/**
	 * Reads a value from a binding and returns it as a boolean. If the value is null, null is returned. If there is a
	 * value, but it cannot be parsed as a boolean null is returned.
	 *
	 * @param value the bindings of the sparql result row
	 * @return the boolean value or null
	 */
	public static Boolean asBoolean(Value value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Literal) {
			try {
				return ((Literal) value).booleanValue();
			}
			catch (NumberFormatException ignore) {
			}
		}
		try {
			return Boolean.parseBoolean(value.stringValue());
		}
		catch (NumberFormatException e) {
			Log.warning("cannot parse binding of '" + value + "' as float");
			return null;
		}
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a boolean. If the column does not
	 * exists or no value is available, the specified default value is returned. If there is a value, but it cannot be
	 * parsed as a boolean the specified default value is returned.
	 *
	 * @param bindings     the bindings of the sparql result row
	 * @param columnName   the column name to read from
	 * @param defaultValue the default value
	 * @return the boolean value or null
	 */
	public static boolean asBoolean(BindingSet bindings, String columnName, boolean defaultValue) {
		Boolean result = asBoolean(bindings, columnName);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding returns it as a boolean. If the binding is null, the specified default value is
	 * returned. If there is a value, but it cannot be parsed as a boolean the specified default value is returned.
	 *
	 * @param binding      the binding of the sparql result row
	 * @param defaultValue the default value
	 * @return the boolean value or null
	 */
	public static boolean asBoolean(Binding binding, boolean defaultValue) {
		Boolean result = asBoolean(binding);
		return (result == null) ? defaultValue : result;
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as an uri. If the column does not exists
	 * or no value is available, null is returned. If there is a value, but it cannot be parsed as a, uri, a synthetic
	 * uri is created form the string of the value.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the uri value or null
	 */
	public static URI asURI(BindingSet bindings, String columnName) {
		return toURI(asString(bindings, columnName));
	}

	/**
	 * Reads a value from a binding and returns it as an uri. If the binding is null, null is returned. If there is a
	 * value, but it cannot be parsed as a, uri, a synthetic uri is created form the string of the value.
	 *
	 * @param binding the binding of the sparql result row
	 * @return the uri value or null
	 */
	public static URI asURI(Binding binding) {
		return toURI(asString(binding));
	}

	/**
	 * Converts a value into an uri. If the binding is null, null is returned. If there is a value, but it cannot be
	 * parsed as a, uri, a synthetic uri is created form the string of the value.
	 *
	 * @param value value to create the URI from
	 * @return the uri value or null
	 */
	public static URI asURI(Value value) {
		if (value == null) return null;
		return toURI(value.stringValue());
	}

	private static URI toURI(String string) {
		if (string == null) {
			return null;
		}

		// try to create uri directly
		try {
			return new URI(string);
		}
		catch (URISyntaxException ignored) {
		}

		// try to create synthetic uri
		try {
			return new URI("http://www.denkbares.com/noURI?id=" + Strings.encodeURL(string));
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("unexpected internal error, must not happen", e);
		}
	}

	/**
	 * Iterates the complete query result and collects the bindings as a multi-map. The specified queryResult ist not
	 * (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param keyExtractor   the function to extract the keys
	 * @param valueExtractor the function to extract the values
	 * @param <K>            the key type
	 * @param <V>            the value type
	 * @return a multi-map of the key-value-pairs
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <K, V> MultiMap<K, V> toMultiMap(ClosableTupleQueryResult queryResult, Function<BindingSet, K> keyExtractor, Function<BindingSet, V> valueExtractor) throws QueryEvaluationException {
		MultiMap<K, V> map = new DefaultMultiMap<>();
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			K key = keyExtractor.apply(bindingSet);
			V value = valueExtractor.apply(bindingSet);
			if (value != null) {
				map.put(key, value);
			}
		}
		return map;
	}

	/**
	 * Iterates the complete query result and collects the bindings as a multi-map. The specified queryResult ist not
	 * (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param keyName        the name of the binding to be used for the keys (sparql variable name without the leading
	 *                       "?")
	 * @param keyExtractor   the function to convert the key bindings into a keys
	 * @param valueName      the name of the binding to be used for the values (sparql variable name without the leading
	 *                       "?")
	 * @param valueExtractor the function to convert the value bindings into a values
	 * @param <K>            the key type
	 * @param <V>            the value type
	 * @return a multi-map of the key-value-pairs
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <K, V> MultiMap<K, V> toMultiMap(ClosableTupleQueryResult queryResult, String keyName, Function<Binding, K> keyExtractor, String valueName, Function<Binding, V> valueExtractor) throws QueryEvaluationException {
		return toMultiMap(queryResult,
				b -> keyExtractor.apply(b.getBinding(keyName)),
				b -> valueExtractor.apply(b.getBinding(valueName)));
	}

	/**
	 * Iterates the complete query result and collects a particular value of each binding set (row) into a set. The
	 * specified queryResult ist not (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param valueExtractor the function to extract the value of each binding set
	 * @param <V>            the value type
	 * @return a set of the extracted values
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <V> Set<V> toSet(ClosableTupleQueryResult queryResult, Function<BindingSet, V> valueExtractor) throws QueryEvaluationException {
		Set<V> set = new HashSet<>();
		collect(queryResult, valueExtractor, set);
		return set;
	}

	/**
	 * Iterates the complete query result and collects the binding of the specified variable name as a set. The
	 * specified queryResult ist not (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param variable       the name of the binding to be used (sparql variable name without the leading "?")
	 * @param valueExtractor the function to convert each value binding into a value
	 * @param <V>            the value type
	 * @return a set of the extracted values
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <V> Set<V> toSet(ClosableTupleQueryResult queryResult, String variable, Function<Binding, V> valueExtractor) throws QueryEvaluationException {
		return toSet(queryResult,
				b -> valueExtractor.apply(b.getBinding(variable)));
	}

	/**
	 * Iterates the complete query result and collects a particular value of each binding set (row) into a list. The
	 * specified queryResult ist not (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param valueExtractor the function to extract the value of each binding set
	 * @param <V>            the value type
	 * @return a list of the extracted values
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <V> List<V> toList(ClosableTupleQueryResult queryResult, Function<BindingSet, V> valueExtractor) throws QueryEvaluationException {
		List<V> list = new LinkedList<>();
		collect(queryResult, valueExtractor, list);
		return list;
	}

	/**
	 * Iterates the complete query result and collects the binding of the specified variable name as a set. The
	 * specified queryResult ist not (!) automatically closed.
	 *
	 * @param queryResult    the query result to be iterated
	 * @param variable       the name of the binding to be used (sparql variable name without the leading "?")
	 * @param valueExtractor the function to convert each value binding into a value
	 * @param <V>            the value type
	 * @return a list of the extracted values
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static <V> List<V> toList(ClosableTupleQueryResult queryResult, String variable, Function<Binding, V> valueExtractor) throws QueryEvaluationException {
		return toList(queryResult,
				b -> valueExtractor.apply(b.getBinding(variable)));
	}

	private static <V> void collect(ClosableTupleQueryResult queryResult, Function<BindingSet, V> valueExtractor, Collection<V> result) {
		if (queryResult instanceof CachedTupleQueryResult) {
			((CachedTupleQueryResult) queryResult).resetIterator();
		}
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			V value = valueExtractor.apply(bindingSet);
			if (value != null) {
				result.add(value);
			}
		}
	}

	/**
	 * Iterates the complete query result and collects the binding of the specified variable name. The variable is
	 * expected to contain a literal and the map will contain the literal mapped by locale. If there are multiple
	 * literals for the same locale, the shorter value will be contained, the longer one discarded.
	 *
	 * @param queryResult the query result to be iterated
	 * @param variable    the name of the binding to be used (sparql variable name without the leading "?")
	 * @return the values for the given variable mapped by locale
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	public static Map<@NotNull Locale, String> toMapByLocale(ClosableTupleQueryResult queryResult, String variable) throws QueryEvaluationException {
		Set<Text> textsSet = Sparqls.toSet(queryResult, variable, Sparqls::asText);
		return toMapByLocale(textsSet);
	}

	/**
	 * Transforms the given collection of Texts to a map, where the locale of the text will be the key and the string
	 * will be the label. If there are multiple strings for the same locale, the shorter value will be contained, the
	 * longer one discarded.
	 *
	 * @param texts a collection of texts to be transformed to a map
	 * @return the values for the given variable mapped by locale
	 * @throws QueryEvaluationException if the query could not been iterated correctly
	 */
	@NotNull
	public static Map<@NotNull Locale, String> toMapByLocale(Collection<Text> texts) {
		return texts.stream().collect(
				Collectors.toMap(Text::getLanguage, Text::getString, (a, b) -> a.length() <= b.length() ? a : b));
	}
}
