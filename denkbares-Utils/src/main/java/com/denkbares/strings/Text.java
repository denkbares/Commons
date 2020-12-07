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

import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of some util class that handles string values together with a potential language tag. If no language
 * tag is provided, the root language is assumed.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 01.01.2015
 */
public class Text {
	private final String string;
	private final Locale language;

	/**
	 * Creates a new text instance if the specified string in the specified language. If the specified language is null,
	 * the root locale is applied.
	 *
	 * @param string   the string to create the text for
	 * @param language the language of the text
	 */
	public Text(String string, Locale language) {
		this.string = string;
		this.language = (language == null) ? Locale.ROOT : language;
	}

	/**
	 * Creates a new text instance if the specified string in the specified language, if the string not null, otherwise
	 * the method returns null. If the specified language is null, the root locale is applied.
	 *
	 * @param string   the string to create the text for
	 * @param language the language of the text
	 * @return the created text instance
	 */
	@Contract("null, _ -> null; !null, _ -> !null")
	public static Text createNonNUll(String string, Locale language) {
		return (string == null) ? null : new Text(string, language);
	}

	/**
	 * Retruns the plain textual content of this Text instance.
	 *
	 * @return the plain text
	 */
	public String getString() {
		return string;
	}

	/**
	 * Returns the language this Text instance is tagged with. It returns {@link Locale#ROOT} if no specific language is
	 * tagged.
	 *
	 * @return the language of the text
	 */
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
