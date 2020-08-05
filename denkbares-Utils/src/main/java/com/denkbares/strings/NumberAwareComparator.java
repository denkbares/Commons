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

import java.util.Comparator;

/**
 * Implements a number-aware comparator of strings. Whenever a number within one of the strings
 * occur, the number is treated as a single character to sort for.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 30.05.2016
 */
public class NumberAwareComparator implements Comparator<String> {

	public static final Comparator<String> CASE_SENSITIVE = new NumberAwareComparator(true);
	public static final Comparator<String> CASE_INSENSITIVE = new NumberAwareComparator(false);

	private final boolean caseSensitive;

	private NumberAwareComparator(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	@SuppressWarnings("Duplicates")
	@Override
	public int compare(String s1, String s2) {
		if (s1 == null) s1 = "";
		if (s2 == null) s2 = "";
		int n1 = s1.length(), n2 = s2.length();
		int i1 = 0, i2 = 0;
		for (; i1 < n1 && i2 < n2; i1++, i2++) {

			// get character at string 1, or number (as negative number)
			int c1 = s1.charAt(i1);
			if (Character.isDigit(c1)) {
				c1 = 0;
				for (int c; i1 < n1 && Character.isDigit(c = s1.charAt(i1)); i1++) {
					c1 = c1 * 10 - Character.digit(c, 10);
				}
				i1--;
			}

			// get character at string 2, or number (as negative number)
			int c2 = s2.charAt(i2);
			if (Character.isDigit(c2)) {
				c2 = 0;
				for (int c; i2 < n2 && Character.isDigit(c = s2.charAt(i2)); i2++) {
					c2 = c2 * 10 - Character.digit(c, 10);
				}
				i2--;
			}

			// if both are numbers, prefer the lower one (with negation corrected)
			if (c1 < 0 && c2 < 0) {
				int compare = Integer.compare(-c1, -c2);
				if (compare != 0) return compare;
				continue;
			}

			// if mixed, treat the number as a number in the alphabet
			if (c1 < 0) {
				return Integer.compare('0', c2);
			}
			if (c2 < 0) {
				return Integer.compare(c1, '0');
			}

			// if both are characters, do normally
			if (c1 != c2) {
				if (caseSensitive) return c1 - c2;
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
				if (c1 != c2) {
					c1 = Character.toLowerCase(c1);
					c2 = Character.toLowerCase(c2);
					if (c1 != c2) {
						return c1 - c2;
					}
				}
			}
		}

		// if any of the strings reach the end, the consumed one comes first
		return (n1 - i1) - (n2 - i2);
	}
}
