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
import com.denkbares.utils.ConsoleWriter.LineConsumer;

/**
 * Class for utility methods about executing commands and do other close-system actions.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 08.02.2014
 */
public class Exec {

	private static final Pattern EXE_SPLIT_PATTERN = Pattern.compile("[\\h\\s\\v]+");

	// constructor parameters
	private final String[] commandLine;

	// optional configuration parameters
	private File directory = null;
	private String[] envp = null;
	private boolean pipeToConsole = true;
	private LineConsumer errorHandler = null;
	private LineConsumer outputHandler = null;
	private Consumer<Integer> exitHandler = null;

	// runtime parameters
	private Process process = null;
	private Thread errorReader = null;
	private Thread outputReader = null;

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
		return error(new SimpleConsumer(errorHandler));
	}

	/**
	 * Sets the error handler that is called for each line written to the error stream of the created process. The
	 * method should not be called after the process is started, as the handler may miss already printed lines. The
	 * method returns this (modified) instance, to chain method calls.
	 *
	 * @param errorHandler the error handler to be used
	 * @return this instance
	 */
	public Exec error(LineConsumer errorHandler) {
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
		return output(new SimpleConsumer(outputHandler));
	}

	/**
	 * Sets the output handler that is called for each line written to the output stream of the created process. The
	 * method should not be called after the process is started, as the handler may miss already printed lines. The
	 * method returns this (modified) instance, to chain method calls.
	 *
	 * @param outputHandler the output handler to be used
	 * @return this instance
	 */
	public Exec output(LineConsumer outputHandler) {
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

	/**
	 * Starts the process and waits for the process to be completed.
	 *
	 * @return the exit value of the subprocess represented by this {@code Exec} object.  By convention, the value
	 * {@code 0} indicates normal termination.
	 * @throws IOException          if the process could not been started properly
	 * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another
	 *                              thread while it is waiting
	 */
	public int runAndWait() throws IOException, InterruptedException {
		startProcess();
		waitProcess();
		return process.exitValue();
	}

	/**
	 * Starts the process asynchronously. The method returns after the process has started.
	 *
	 * @throws IOException if the process could not been started properly
	 */
	public void runAsync() throws IOException {
		startProcess();
		new Thread(() -> {
			try {
				waitProcess();
			}
			catch (InterruptedException e) {
				Log.warning("interrupted waiting for external process", e);
			}
		}, "Exec-AsyncRunner: " + getCommand()).start();
	}

	/**
	 * Causes the current thread to wait, if necessary, until the process represented by this {@code Exec} object has
	 * terminated.  This method returns immediately if the subprocess has already terminated.  If the subprocess has not
	 * yet terminated, the calling thread will be blocked until the subprocess exits.
	 *
	 * @return the exit value of the subprocess represented by this {@code Process} object.  By convention, the value
	 * {@code 0} indicates normal termination.
	 * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another
	 *                              thread while it is waiting
	 */
	public int waitFor() throws InterruptedException {
		int exitValue = process.waitFor();
		if (errorReader != null) errorReader.join();
		if (outputReader != null) outputReader.join();
		return exitValue;
	}

	/**
	 * Returns the exit value for the subprocess.
	 *
	 * @return the exit value of the subprocess represented by this {@code Exec} object.  By convention, the value
	 * {@code 0} indicates normal termination.
	 * @throws IllegalThreadStateException if the subprocess represented by this {@code Exec} object has not yet
	 *                                     terminated
	 */
	public int exitValue() {
		return process.exitValue();
	}

	/**
	 * Kills the subprocess. Whether the subprocess represented by this {@code Process} object is forcibly terminated or
	 * not is implementation dependent.
	 */
	public void destroy() {
		process.destroy();
	}

	/**
	 * Kills the subprocess. The subprocess represented by this {@code Exec} object is forcibly terminated.
	 *
	 * <p>Note: The subprocess may not terminate immediately.
	 * i.e. {@code isAlive()} may return true for a brief period after {@code destroyForcibly()} is called. This method
	 * may be chained to {@code waitFor()} if needed.
	 *
	 * @return the {@code Exec} object representing the subprocess to be forcibly destroyed.
	 */
	public Process destroyForcibly() {
		return process.destroyForcibly();
	}

	/**
	 * Tests whether the subprocess represented by this {@code Exec} is alive.
	 *
	 * @return {@code true} if the subprocess represented by this {@code Exec} object has not yet terminated.
	 */
	public boolean isAlive() {
		return process.isAlive();
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	private void startProcess() throws IOException {
		if (Strings.isBlank(getCommand())) throw new IOException("no command specified");
		if (this.process != null) throw new IOException("process already started");
		this.process = Runtime.getRuntime().exec(commandLine, envp, directory);
		this.outputReader = connectStream(process.getInputStream(), System.out, outputHandler);
		this.errorReader = connectStream(process.getErrorStream(), System.err, errorHandler);
	}

	private int waitProcess() throws InterruptedException {
		try {
			// wait for process and wait for readers to complete
			int exitValue = waitFor();

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

	private Thread connectStream(InputStream in, PrintStream delegate, LineConsumer handler) {
		// even if there is no handler, we have to read the stream contents,
		// otherwise the process may block its execution if the buffer is full
		InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
		ConsoleWriter writer = new ConsoleWriter();
		if (pipeToConsole) writer.addListener(new PipeToPrintStream(delegate));
		if (handler != null) writer.addListener(handler);
		Thread streamReader = new Thread(() -> {
			try {
				reader.transferTo(writer);
			}
			catch (IOException ignore) {
				// ignore exception, as it usually happens when the application returns and the stream is closed
			}
		}, "Exec-StreamReader: " + getCommand());
		streamReader.start();
		return streamReader;
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

	/**
	 * Line consumer that sequentially only receives completed lines, and never touches an already completed line
	 */
	private static class SimpleConsumer implements LineConsumer {

		private final Consumer<String> consumer;
		private int completedLineNo = -1;

		private SimpleConsumer(Consumer<String> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void accept(int lineNo, CharSequence line) {
			// special case: if the same line is touched (and not empty), reprint the line
			if (lineNo == completedLineNo && line.length() > 0) consumer.accept(line.toString());

			// if we are remaining in the recently completed line (or before), there is nothing we can do
			if (lineNo <= completedLineNo) return;

			// fill potentially empty lines in between
			for (int i = completedLineNo + 1; i < lineNo; i++) consumer.accept("");

			// and process the recent line
			consumer.accept(line.toString());
			completedLineNo = lineNo;
		}
	}

	private static class PipeToPrintStream implements LineConsumer {

		private final PrintStream out;
		private int currentLineNo = -1;

		private PipeToPrintStream(PrintStream out) {
			this.out = out;
		}

		@Override
		public void accept(int lineNo, CharSequence line) {
			// if we are remaining in the current line (or before), clear line and append
			if (lineNo <= currentLineNo) {
				out.print(Consoles.Delete.CLEAR_LINE.getCode());
				out.print("\r");
				out.print(line);
			}
			// otherwise proceed to the next line, and append there
			else {
				while (currentLineNo < lineNo) {
					out.print("\n");
					currentLineNo++;
				}
				out.print(line);
			}
		}
	}
}
