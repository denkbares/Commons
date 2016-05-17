/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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

package com.denkbares.semanticcore.sparql;

import org.openrdf.query.QueryEvaluationException;

import com.denkbares.semanticcore.TupleQueryResult;
import com.denkbares.semanticcore.ClosableTupleQueryResult;

/**
 * @author Alex Legler
 */
public class SPARQLQueryResult implements AutoCloseable {

	private ClosableTupleQueryResult result;

	public SPARQLQueryResult(TupleQueryResult result) {
		this.result = result;
	}

	public ClosableTupleQueryResult getResult() {
		return result;
	}

	/**
	 * Returns a cached and closed instance of this result.
	 *
	 * @see ClosableTupleQueryResult#cachedAndClosed()
	 */
	public SPARQLQueryResult cachedAndClosed() throws QueryEvaluationException {
		this.result = result.cachedAndClosed();
		return this;
	}

	@Override
	public void close() throws QueryEvaluationException {
		result.close();
	}

}
