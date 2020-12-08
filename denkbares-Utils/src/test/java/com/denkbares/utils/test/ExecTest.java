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

package com.denkbares.utils.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import com.denkbares.utils.Exec;
import com.denkbares.utils.Log;
import com.denkbares.utils.OS;

import static org.junit.Assert.*;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 15.11.2019
 */
public class ExecTest {
	@Test
	public void parseEmpty() {
		assertEquals("", Exec.parse(null).getCommand());
		assertEquals("", Exec.parse("").getCommand());
	}

	@Test(expected = IOException.class)
	public void failEmpty() throws IOException, InterruptedException {
		Exec.parse("").runAndWait();
	}

	@Test
	public void parsePlain() {
		Exec exec = Exec.parse("ls -al");
		assertEquals("ls", exec.getCommand());
		assertArrayEquals(new String[] { "-al" }, exec.getArguments());
	}

	@Test
	public void parseQuoted() {
		Exec exec = Exec.parse("ls \"/usr/my home/foo\" -a -l");
		assertEquals("ls", exec.getCommand());
		assertArrayEquals(new String[] { "/usr/my home/foo", "-a", "-l" }, exec.getArguments());

		exec = Exec.parse("\"ls -al\"");
		assertEquals("ls -al", exec.getCommand());
		assertEquals(0, exec.getArguments().length);
	}

	@Test
	public void listFiles() throws IOException, InterruptedException {
		String cmdLine;
		switch (OS.getCurrentOS()) {
			case WINDOWS:
				cmdLine = "dir .";
				break;
			case MAC_OS:
			case UNIX:
				cmdLine = "ls -al";
				break;
			default:
				Log.warning("unknown operating system, skip test: " + OS.getCurrentOriginalName());
				return;
		}

		// execute command line and check if the pom.xml of the working directory has been found
		List<String> files = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		Exec.parse(cmdLine).console(false)
				.error((Consumer<String>) errors::add)
				.output((Consumer<String>) files::add)
				.runAndWait();
		assertTrue(errors.isEmpty());
		assertTrue(files.stream().anyMatch(file -> file.contains("pom.xml")));
	}
}
