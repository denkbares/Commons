/*
 * Copyright (C) 2021 denkbares GmbH, Germany
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Value;

import static org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil.isStringLiteral;

/**
 * Comparator that tries to parse dates out of string values and sort by that date
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.04.21
 */
public class ValueAsDateComparator implements ValueComparator {

	private static final DefaultValueComparator VALUE_COMPARATOR = new DefaultValueComparator();
	private static final Pattern GERMAN_DATE_PATTERN = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
	private final SimpleDateFormat GERMAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
	private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public int compare(Value v1, Value v2) {
		int result = 0;
		if (isStringLiteral(v1) && isStringLiteral(v2)) {
			Date d1 = extractDate(v1);
			Date d2 = extractDate(v2);
			if (d1 == null && d2 != null) {
				return -1;
			}
			else if (d1 != null && d2 == null) {
				return 1;
			}
			else if (d1 != null) {
				result = d1.compareTo(d2);
			}
		}
		if (result == 0) {
			return VALUE_COMPARATOR.compare(v1, v2);
		}
		else {
			return result;
		}
	}

	private Date extractDate(Value value) {
		String text = value.stringValue();
		Matcher matcher = GERMAN_DATE_PATTERN.matcher(text);
		if (matcher.find()) {
			try {
				return GERMAN_DATE_FORMAT.parse(matcher.group());
			}
			catch (ParseException e) {
				return null;
			}
		}
		matcher = DATE_PATTERN.matcher(text);
		if (matcher.find()) {
			try {
				return DATE_FORMAT.parse(matcher.group());
			}
			catch (ParseException e) {
				return null;
			}
		}
		return null;
	}
}
