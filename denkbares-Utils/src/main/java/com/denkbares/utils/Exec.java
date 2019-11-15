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
package com.denkbares.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.strings.QuoteSet;
import com.denkbares.strings.StringFragment;
import com.denkbares.strings.Strings;

/**
 * Class for utility methods about executing commands and do other close-system actions.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 08.02.2014
 */
public class Exec {

	private static final Pattern EXE_SPLIT_PATTERN = Pattern.compile("[\\h\\s\\v]+");

	private final String[] commandLine;

	private File directory = null;
	private String[] envp = null;
	private boolean pipeToConsole = true;
	private Consumer<String> errorHandler = null;
	private Consumer<String> outputHandler = null;
	private Consumer<Integer> exitHandler = null;

	public Exec(String command, String... arguments) {
		this.commandLine = new String[arguments.length + 1];
		this.commandLine[0] = command;
		System.arraycopy(arguments, 0, this.commandLine, 1, arguments.length);
	}

	private Exec(String[] commandLine) {
		this.commandLine = commandLine;
	}

	@NotNull
	public static Exec parse(String commandLine) {
		if (commandLine == null) return new Exec("");
		return new Exec(Strings
				.splitUnquoted(commandLine, EXE_SPLIT_PATTERN, false, new QuoteSet(Strings.QUOTE_DOUBLE))
				.stream().map(StringFragment::getContentTrimmed).map(Strings::unquote).toArray(String[]::new));
	}

	/**
	 * Returns the command that is executed by this instance. This is the first item of the command line. It returns an
	 * empty string if the command line was empty.
	 *
	 * @return the command file
	 */
	@NotNull
	public String getCommand() {
		return (commandLine.length == 0) ? "" : commandLine[0];
	}

	/**
	 * Returns the command line arguments that are passed to the command. Each argument is a separate String, unquoted,
	 * allowed to include special characters, e.g. whitespaces.
	 *
	 * @return the command line arguments
	 */
	@NotNull
	public String[] getArguments() {
		return Arrays.copyOfRange(commandLine, 1, commandLine.length);
	}

	/**
	 * Modifies this instance to use the specified working directory. By default, or if null is specified, the main
	 * process's directory is used. The method returns this (modified) instance, to chain method calls.
	 *
	 * @param workingDirectory the new working directory
	 * @return this instance
	 */
	public Exec directory(@Nullable File workingDirectory) {
		this.directory = workingDirectory;
		return this;
	}

	/**
	 * Modifies this instance to use the specified environment variables. By default, or if null is specified, the main
	 * process's environment variables are used. The method returns this (modified) instance, to chain method calls.
	 *
	 * @param keyValuePairs an even number of keys and values for the environment variables
	 * @return this instance
	 */
	public Exec environment(String... keyValuePairs) {
		if (keyValuePairs == null) {
			this.envp = null;
		}
		else if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("odd number of parameters as key-value-pairs: " + Strings.concat(", ", keyValuePairs));
		}
		else {
			this.envp = new String[keyValuePairs.length / 2];
			for (int i = 0; i < envp.length; i++) {
				envp[i] = keyValuePairs[i * 2] + "=" + keyValuePairs[i * 2 + 1];
			}
		}
		return this;
	}

	/**
	 * Sets the error handler that is called for each line written to the error stream of the created process. The
	 * method should not be called after the process is started, as the handler may miss already printed lines. The
	 * method returns this (modified) instance, to chain method calls.
	 *
	 * @param errorHandler the error handler to be used
	 * @return this instance
	 */
	public Exec error(Consumer<String> errorHandler) {
		if (this.errorHandler != null) throw new IllegalStateException("cannot set multiple error handlers");
		this.errorHandler = errorHandler;
		return this;
	}

	/**
	 * Sets the output handler that is called for each line written to the output stream of the created process. The
	 * method should not be called after the process is started, as the handler may miss already printed lines. The
	 * method returns this (modified) instance, to chain method calls.
	 *
	 * @param outputHandler the output handler to be used
	 * @return this instance
	 */
	public Exec output(Consumer<String> outputHandler) {
		if (this.outputHandler != null) throw new IllegalStateException("cannot set multiple output handlers");
		this.outputHandler = outputHandler;
		return this;
	}

	/**
	 * Modifies this instance to tell them if the console output and error should be piped to the actual console of the
	 * calling process. By default all outputs are piped. The method returns this (modified) instance, to chain method
	 * calls.
	 *
	 * @param pipeToConsole false to prevent outputs and errors to be written to the console
	 * @return this instance
	 */
	public Exec console(boolean pipeToConsole) {
		this.pipeToConsole = pipeToConsole;
		return this;
	}

	/**
	 * Sets the exit handler that is called when the process completes. The handler then gets the exit value of the
	 * process. The method should not be called after the process is started, as the handler may miss an already
	 * completed process. The method returns this (modified) instance, to chain method calls.
	 *
	 * @param exitHandler the exit handler to be used
	 * @return this instance
	 */
	public Exec exit(Consumer<Integer> exitHandler) {
		if (this.exitHandler != null) throw new IllegalStateException("cannot set multiple exit handlers");
		this.exitHandler = exitHandler;
		return this;
	}

	public int runAndWait() throws IOException, InterruptedException {
		Process process = startProcess();
		waitProcess(process);
		return process.exitValue();
	}

	public void runAsync() throws IOException {
		Process process = startProcess();
		new Thread(() -> {
			try {
				waitProcess(process);
			}
			catch (InterruptedException e) {
				Log.warning("interrupted waiting for external process", e);
			}
		}, "Exec: " + getCommand()).start();
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	private Process startProcess() throws IOException {
		if (Strings.isBlank(getCommand())) throw new IOException("no command specified");
		Process process = Runtime.getRuntime().exec(commandLine, envp, directory);
		connectStream(process.getInputStream(), System.out, outputHandler);
		connectStream(process.getErrorStream(), System.err, errorHandler);
		return process;
	}

	private int waitProcess(Process process) throws InterruptedException {
		try {
			process.waitFor();
			int exitValue = process.exitValue();
			// inform exit handler
			if (exitHandler != null) {
				exitHandler.accept(exitValue);
			}
			return exitValue;
		}
		finally {
			// we must close all by exec(..) opened streams:
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
			Streams.closeQuietly(process.getInputStream());
			Streams.closeQuietly(process.getOutputStream());
			Streams.closeQuietly(process.getErrorStream());
		}
	}

	private void connectStream(InputStream in, PrintStream delegate, Consumer<String> handler) {
		// even if there is no handler, we have to read the stream contents,
		// otherwise the process may block its execution if the buffer is full
		new Thread(() -> {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			try {
				while (true) {
					String line = reader.readLine();
					if (line == null) break;
					if (pipeToConsole) delegate.println(line);
					if (handler != null) handler.accept(line);
				}
			}
			catch (IOException ignore) {
				// ignore exception, as it usually happens when the application returns and the stream is closed
			}
		}, "Exec-StreamReader: " + getCommand()).start();
	}

	/**
	 * Runs a simple command. The environment and working directory is inherited from the parent process (e.g. the one
	 * in which this Java VM runs).
	 *
	 * @param command The command to run
	 * @return Standard output from the command.
	 * @throws IOException          If the command failed
	 * @throws InterruptedException If the command was halted
	 */
	public static String runSimpleCommand(String command) throws IOException, InterruptedException {
		return runSimpleCommand(command, null);
	}

	/**
	 * Runs a simple command in given directory. The environment is inherited from the parent process (e.g. the one in
	 * which this Java VM runs).
	 *
	 * @param command   The command to run
	 * @param directory The working directory to run the command in
	 * @return Standard output from the command.
	 * @throws IOException          If the command failed
	 * @throws InterruptedException If the command was halted
	 */
	public static String runSimpleCommand(String command, File directory) throws IOException, InterruptedException {
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		int exitValue = parse(command).directory(directory)
				.error(line -> error.append("\n").append(line))
				.output(line -> output.append(line).append("\n"))
				.runAndWait();

		if (exitValue > 0 || error.length() > 0) {
			throw new IOException("Command failed with exit code: " + exitValue + error);
		}
		return output.toString();
	}
}
