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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import com.denkbares.strings.NumberAwareComparator;

import static org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil.isStringLiteral;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.04.21
 */
public class DefaultValueComparator implements ValueComparator {

	private static final org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator VALUE_COMPARATOR
			= new org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator();

	@Override
	public int compare(Value v1, Value v2) {
		// We could just use RDF4J ValueComparator for everything, but actually,
		// NumberAwareComparator is a bit nicer, so we use that for strings and IRIs
		if ((v1 != null && v2 != null)
			&& ((v1 instanceof IRI && v2 instanceof IRI) || (isStringLiteral(v1) && isStringLiteral(v2)))) {
			return NumberAwareComparator.CASE_INSENSITIVE.compare(v1.stringValue(), v2.stringValue());
		}
		else {
			return VALUE_COMPARATOR.compare(v1, v2);
		}
	}
}
