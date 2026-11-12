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

package com.denkbares.semanticcore.test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import com.denkbares.semanticcore.SemanticCore;
import com.denkbares.semanticcore.config.RdfConfig;

import static org.junit.Assert.assertTrue;

public class SemanticCoreNamespacesTest {

	/**
	 * The namespace store of the memory sail is not synchronized, so reading the namespaces while another thread
	 * changes them must not fail with a ConcurrentModificationException.
	 */
	@Test
	public void readNamespacesWhileChangingThem() throws Exception {
		SemanticCore core = SemanticCore.createInstance("SemanticCoreNamespacesTest", new RdfConfig(),
				new File("target/SemanticCoreNamespacesTest"));
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			try (RepositoryConnection connection = core.getConnection()) {
				for (int i = 0; i < 20; i++) {
					connection.setNamespace("ns" + i, "http://example.org/ns" + i + "#");
				}
			}
			AtomicBoolean running = new AtomicBoolean(true);
			// change namespaces in bursts, like a compilation adding the namespaces of an article
			Future<?> writer = executor.submit(() -> {
				while (running.get()) {
					try (RepositoryConnection connection = core.getConnection()) {
						for (int i = 0; i < 5; i++) {
							String prefix = "changing" + i;
							connection.setNamespace(prefix, "http://example.org/" + prefix + "#");
							connection.removeNamespace(prefix);
						}
					}
					LockSupport.parkNanos(1_000_000L);
				}
			});
			try {
				long end = System.currentTimeMillis() + 2000;
				while (System.currentTimeMillis() < end) {
					assertTrue(core.getNamespaces().size() >= 20);
				}
			}
			finally {
				running.set(false);
				writer.get();
			}
		}
		finally {
			executor.shutdownNow();
			core.close();
		}
	}
}
