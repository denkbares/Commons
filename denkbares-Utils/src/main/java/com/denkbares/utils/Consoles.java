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

package com.denkbares.utils;

/**
 * Utility class with handy methods for dealing with console.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 25.04.2020
 */
public class Consoles {

	/**
	 * Formats the specified text, by wrapping the text with the specified formats, resetting the format at the end of
	 * the text. if any of the formats are null, they will be ignored.
	 *
	 * @param text    the text to be wrapped
	 * @param formats the formats to be applied to the text
	 * @return the formatted text
	 */
	public static String formatText(String text, Style... formats) {
		if (formats == null) return text;
		StringBuilder result = new StringBuilder();
		for (Style format : formats) if (format != null) result.append(format.getCode());
		result.append(text);
		result.append(RESET.getCode());
		return result.toString();
	}

	/**
	 * Removes al known ascii control sequences form the specified potentially formatted string. Return the plain
	 * character that will be printed to the console.
	 *
	 * @param formattedText the text to remove al formats from
	 * @return the unformatted plain text
	 */
	public static String toPlainText(String formattedText) {
		return formattedText.replaceAll("\\u001b\\[\\d+(?:;\\d+)?[a-zA-Z]", "");
	}

	@FunctionalInterface
	public interface Style {
		String getCode();
	}

	public static final Style RESET = () -> "\u001b[0m";

	public enum Cursor implements Style {

		UP("A"),
		DOWN("B"),
		RIGHT("C"),
		LEFT("D");

		private final String code;

		Cursor(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return getCode(1);
		}

		public String getCode(int times) {
			return "\u001b[" + times + code;
		}
	}

	public enum Delete implements Style {

		CLEAR_SCREEN_AFTER_CURSOR("\u001b[0J"),
		CLEAR_SCREEN_BEFORE_CURSOR("\u001b[1J"),
		CLEAR_SCREEN("\u001b[2J"),

		CLEAR_LINE_AFTER_CURSOR("\u001b[0K"),
		CLEAR_LINE_BEFORE_CURSOR("\u001b[1K"),
		CLEAR_LINE("\u001b[2K");

		private final String code;

		Delete(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}
	}

	public enum Decoration implements Style {

		BOLD("\u001b[1m"),
		UNDERLINE("\u001b[4m"),
		REVERSED("\u001b[7m");

		private final String code;

		Decoration(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}
	}

	public enum Color implements Style {

		BLACK("\u001b[30m"),
		RED("\u001b[31m"),
		GREEN("\u001b[32m"),
		YELLOW("\u001b[33m"),
		BLUE("\u001b[34m"),
		MAGENTA("\u001b[35m"),
		CYAN("\u001b[36m"),
		WHITE("\u001b[37m"),
		BRIGHT_BLACK("\u001b[30;1m"),
		BRIGHT_RED("\u001b[31;1m"),
		BRIGHT_GREEN("\u001b[32;1m"),
		BRIGHT_YELLOW("\u001b[33;1m"),
		BRIGHT_BLUE("\u001b[34;1m"),
		BRIGHT_MAGENTA("\u001b[35;1m"),
		BRIGHT_CYAN("\u001b[36;1m"),
		BRIGHT_WHITE("\u001b[37;1m"),
		RESET("\u001b[0m");

		private final String code;

		Color(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}
	}

	public enum BackColor implements Style {

		BLACK("\u001b[40m"),
		RED("\u001b[41m"),
		GREEN("\u001b[42m"),
		YELLOW("\u001b[43m"),
		BLUE("\u001b[44m"),
		MAGENTA("\u001b[45m"),
		CYAN("\u001b[46m"),
		WHITE("\u001b[47m"),
		BRIGHT_BLACK("\u001b[40;1m"),
		BRIGHT_RED("\u001b[41;1m"),
		BRIGHT_GREEN("\u001b[42;1m"),
		BRIGHT_YELLOW("\u001b[43;1m"),
		BRIGHT_BLUE("\u001b[44;1m"),
		BRIGHT_MAGENTA("\u001b[45;1m"),
		BRIGHT_CYAN("\u001b[46;1m"),
		BRIGHT_WHITE("\u001b[47;1m"),
		RESET("\u001b[0m");

		private final String code;

		BackColor(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}
	}
}
