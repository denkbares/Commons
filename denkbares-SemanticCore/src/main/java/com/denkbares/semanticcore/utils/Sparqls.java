/*
 * Copyright (C) 2015 denkbares GmbH
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

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

import com.denkbares.strings.Strings;
import com.denkbares.utils.Log;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 02.04.2015
 */
public class Sparqls {

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a language-tagged
	 * text. If no language is specified, the root language is returned. If the column does not
	 * exists or no value is available, null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the text or null
	 */
	public static Text asText(BindingSet bindings, String columnName) {
		// be careful to get the string/language, because it is not sure that the items exists
		// Note: it would be easier and faster to simply do all calls directly and catch the
		// NullPointerException, but this is a mess when debugging for other NullPointerExceptions
		Binding binding = bindings.getBinding(columnName);
		if (binding == null) return null;
		Value value = binding.getValue();
		return Text.create(value);
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a plain string. If
	 * the column does not exists or no value is available, null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the plain string or null
	 */
	public static String asString(BindingSet bindings, String columnName) {
		// be careful to get the uri, because it is not sure that the items exists
		// Note: it would be easier and faster to simply do all calls directly and catch the
		// NullPointerException, but this is a mess when debugging for other NullPointerExceptions
		Binding binding = bindings.getBinding(columnName);
		if (binding == null) return null;
		Value value = binding.getValue();
		if (value == null) return null;
		return value.stringValue();
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a float number. If
	 * the column does not exists or no value is available, null is returned. If there is a value,
	 * but it cannot be parsed as a number null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the number value or null
	 */
	public static Float asFloat(BindingSet bindings, String columnName) {
		// be careful to get the uri, because it is not sure that the items exists
		// Note: it would be easier and faster to simply do all calls directly and catch the
		// NullPointerException, but this is a mess when debugging for other NullPointerExceptions
		Binding binding = bindings.getBinding(columnName);
		if (binding == null) return null;
		Value value = binding.getValue();
		if (value == null) return null;
		if (binding instanceof Literal) {
			try {
				return ((Literal) binding).floatValue();
			}
			catch (NumberFormatException ignore) {
			}
		}
		try {
			return Float.parseFloat(value.stringValue());
		}
		catch (NumberFormatException e) {
			Log.warning("cannot parse binding of '" + binding + "' as float");
			return null;
		}
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a float number. If
	 * the column does not exists or no value is available, the specified default value is returned.
	 * If there is a value, but it cannot be parsed as a number the specified default value is
	 * returned.
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
	 * Reads a value from a binding set of a sparql result row and returns it as a integer number.
	 * If the column does not exists or no value is available, null is returned. If there is a
	 * value, but it cannot be parsed as a number null is returned.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the number value or null
	 */
	public static Integer asInteger(BindingSet bindings, String columnName) {
		// be careful to get the uri, because it is not sure that the items exists
		// Note: it would be easier and faster to simply do all calls directly and catch the
		// NullPointerException, but this is a mess when debugging for other NullPointerExceptions
		Binding binding = bindings.getBinding(columnName);
		if (binding == null) return null;
		Value value = binding.getValue();
		if (value == null) return null;
		if (binding instanceof Literal) {
			try {
				return ((Literal) binding).intValue();
			}
			catch (NumberFormatException ignore) {
			}
		}
		try {
			return Integer.parseInt(value.stringValue());
		}
		catch (NumberFormatException e) {
			Log.warning("cannot parse binding of '" + binding + "' as float");
			return null;
		}
	}

	/**
	 * Reads a value from a binding set of a sparql result row and returns it as a integer number.
	 * If the column does not exists or no value is available, the specified default value is
	 * returned. If there is a value, but it cannot be parsed as a number the specified default
	 * value is returned.
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
	 * Reads a value from a binding set of a sparql result row and returns it as a uri. If the
	 * column does not exists or no value is available, null is returned. If there is a value, but
	 * it cannot be parsed as a, uri, a synthetic uri is created form the string of the value.
	 *
	 * @param bindings   the bindings of the sparql result row
	 * @param columnName the column name to read from
	 * @return the uri value or null
	 */
	public static URI asURI(BindingSet bindings, String columnName) {
		String string = asString(bindings, columnName);
		if (string == null) return null;

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
}
