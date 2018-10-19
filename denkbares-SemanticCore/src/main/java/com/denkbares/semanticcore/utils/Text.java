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
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.denkbares.strings.Strings;

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
		this.language = (language == null) ? Locale.ROOT : language;
	}

	public static Text create(String string, Locale language) {
		return new Text(string, language);
	}

	public static Text create(Value value) {
		if (value == null) return null;
		Locale locale = Locale.ROOT;
		if (value instanceof Literal) {
			locale = ((Literal) value).getLanguage().map(Strings::parseLocale).orElse(Locale.ROOT);
		}
		return new Text(value.stringValue(), locale);
	}

	public String getString() {
		return string;
	}

	@NotNull
	public Locale getLanguage() {
		return language;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Text)) return false;
		Text text = (Text) o;
		return Objects.equals(string, text.string) &&
				Objects.equals(language, text.language);
	}

	@Override
	public int hashCode() {
		return Objects.hash(string, language);
	}
}
