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

package com.denkbares.semanticcore;

import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * TupleQueryResult that is also auto closable and can be cached. Always use the following way: <p/> try
 * (TupleQueryResult result = getResult()) {<br/> // iterate over result <br/> }
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 29.01.16
 */
public interface ClosableTupleQueryResult extends org.eclipse.rdf4j.query.TupleQueryResult, AutoCloseable {

	/**
	 * Will cause the underlying result to be retrieved completely into a cache. After the caching, the underlying
	 * result is also closed. This way, after calling this method, it is no longer necessary to close this instance.
	 * This is particularly useful if the repository only provides a limited amount of concurrent queries and you want
	 * to evaluate additional queries while iterating over this result.
	 *
	 * @return a cached and closed version of this result, without preserving the namespaces
	 * @throws QueryEvaluationException if there is a exception while evaluating
	 */
	CachedTupleQueryResult cachedAndClosed() throws QueryEvaluationException;
}
