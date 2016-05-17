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

import java.util.Locale;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;

import de.d3web.strings.Strings;

/**
 * Implementation of some util class that handles string values together with a potential language
 * tag. If no language tag is provided, the root language is assumed.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 01.01.2015
 */
public class Text {
	private final String string;
	private final Locale language;

	private Text(String string, Locale language) {
		this.string = string;
		this.language = language;
	}

	public static Text create(String string, Locale language) {
		return new Text(string, language);
	}

	public static Text create(Value value) {
		if (value == null) return null;
		if (value instanceof Literal) {
			Locale locale = Strings.parseLocale(((Literal) value).getLanguage());
			if (locale != null) {
				return new Text(value.stringValue(), locale);
			}
		}
		return new Text(value.stringValue(), Locale.ROOT);
	}

	public String getString() {
		return string;
	}

	public Locale getLanguage() {
		return language;
	}
}
