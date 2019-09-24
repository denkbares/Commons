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

package com.denkbares.semanticcore.changelog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.denkbares.semanticcore.SemanticCore;
import com.denkbares.utils.Log;

public class ChangeLog {

	private static Queue<Change> committedChanges;
	private static PrintWriter logWriter;
	private static ExecutorService es;

	private ChangeLog() {

	}

	public static void loadAndCommitChanges(File rdfFile, SemanticCore core) {
		Queue<Change> changes = loadChanges(rdfFile);
		commitChanges(core, changes);
	}

	public static Queue<Change> loadChanges(File rdfFile) {

		// clear previous content
		committedChanges = new LinkedBlockingQueue<>();
		es = Executors.newSingleThreadExecutor();

		String fileName = rdfFile.getAbsolutePath();
		fileName = fileName.substring(0, fileName.length() - 3);
		fileName = fileName + "changes";
		Queue<Change> changes = new LinkedList<>();
		try {
			File changeFile = new File(fileName);
			if (changeFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String changeLine = reader.readLine();
				while (changeLine != null) {
					Change change = Change.parseCSV(changeLine);
					committedChanges.add(change);
					changes.add(change);
					changeLine = reader.readLine();
				}
				reader.close();
			}

			logWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
		}
		catch (IOException | ParseException e) {
			Log.severe("Exception while loading file " + rdfFile, e);
		}
		return changes;
	}

	public static void commitChanges(SemanticCore core, Queue<Change> changes) {
		try (RepositoryConnection connection = core.getConnection()) {
			ValueFactory vf = core.getValueFactory();
			connection.begin();
			for (Change change : changes) {
				Statement s = toStatement(vf, change);
				if (change.getAction() == Change.Action.ADD) {
					connection.add(s);
				}
				else {
					connection.remove(s);
				}
			}
			connection.commit();
		}
		catch (RepositoryException e) {
			Log.severe("Exception while committing changes", e);
		}
	}

	private static Statement toStatement(ValueFactory vf, Change change) {
		Resource subject = toResource(vf, change.getSubject());
		URI predicate = vf.createURI(change.getPredicate());
		Value object = toValue(vf, change.getObject());
		return vf.createStatement(subject, predicate, object);
	}

	private static Value toValue(ValueFactory vf, String object) {
		if (object.startsWith("http://")) {
			return vf.createURI(object);
		}
		else if (object.matches("node\\d+")) {
			return vf.createBNode(object);
		}
		return vf.createLiteral(object);
	}

	private static Resource toResource(ValueFactory vf, String subject) {
		if (subject.startsWith("http://")) {
			return vf.createURI(subject);
		}
		return vf.createBNode(subject);
	}

	public static void saveLog() {
		logWriter.flush();
		logWriter.close();
	}

	public static void addEntry(Change change) {
		if (change != null) {
			// enqueue ChangeLogWriter in ExecutorService (I/O is done sequentially in background)
			es.submit(new ChangeLogWriter(change));
		}
	}

	static class ChangeLogWriter extends Thread {

		private final Change change;

		public ChangeLogWriter(Change change) {
			this.change = change;
		}

		@Override
		public void run() {
			logWriter.println(change.toCSVString());
			logWriter.flush();
			committedChanges.add(change);
		}
	}
}
