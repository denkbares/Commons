/*
 * Copyright (C) 2026 denkbares GmbH, Germany
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
package com.denkbares.semanticcore.events;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.denkbares.events.Event;

/**
 * Event fired when a new connection to a SemanticCore has been closed.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @since 2026-02-13
 */
public class SemanticCoreConnectionClosedEvent implements Event {

	private final RepositoryConnection connection;

	public SemanticCoreConnectionClosedEvent(RepositoryConnection connection) {
		this.connection = connection;
	}

	public RepositoryConnection getConnection() {
		return connection;
	}
}
