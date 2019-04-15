/*
 * Copyright (C) 2018 denkbares GmbH. All rights reserved.
 */

package com.denkbares.utils;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jetbrains.annotations.NotNull;

/**
 * A Future implementation that creates a future based on a callable (supplier that may throw any exception), that is
 * created synchronously in the thread calling the {@link #get()} method the first time. All other thread calling {@link
 * #get()} will wait for the result or return immediately if the reuslt object is computed.
 * <p>
 * The class also provides utility methods to unwrap (and rethrow) common exceptions.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 07.08.2018
 */
public class DefaultFuture<V> extends FutureTask<V> {

	/**
	 * Create a new future that uses the specified computational function to create the result object as required.
	 *
	 * @param callable supplier to create the result
	 */
	public DefaultFuture(@NotNull Callable<V> callable) {
		super(callable);
	}

	/**
	 * Create a new future that uses ready prepared result object. Usually you do not require to call this method,
	 * unless you have the result object and want to mimic a future implementation.
	 *
	 * @param result the already prepared result
	 */
	public DefaultFuture(V result) {
		this(() -> result);
	}

	@Override
	public V get() {
		try {
			run();
			return super.get();
		}
		catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			throw new IllegalStateException(e);
		}
	}

	@Override
	public V get(long timeout, @NotNull TimeUnit unit) {
		try {
			startASync();
			return super.get(timeout, unit);
		}
		catch (InterruptedException | TimeoutException e) {
			throw new IllegalStateException(e);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof IOException) throw new IOError(cause);
			throw new IllegalStateException(cause);
		}
	}

	/**
	 * Asynchronously starts the computational task to prepare the result at some time in the future, using the common
	 * ForkJoinPool. The method returns this future to chain multiple calls.
	 * <p>
	 * Note: For optimization reasons, you should not calls this method if you immediately call {@link #get()} or any of
	 * the unwrapXYZ methods afterwards, to avoid threading overhead.
	 *
	 * @return this future
	 */
	public DefaultFuture<V> startASync() {
		ForkJoinPool.commonPool().execute(this);
		return this;
	}

	/**
	 * Waits for the computational task to be completed, and throws IOException if an IOException has occurred.
	 * Otherwise this future is returned to chain multiple calls.
	 *
	 * @return this future
	 * @throws IOException if an IOException prevented the computational task to complete normally
	 */
	public DefaultFuture<V> unwrapIO() throws IOException {
		try {
			run();
			super.get();
		}
		catch (InterruptedException ignore) {
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
		}
		return this;
	}

	/**
	 * Waits for the computational task to be completed, and throws InterruptedException if an InterruptedException has
	 * occurred. Otherwise this future is returned to chain multiple calls.
	 *
	 * @return this future
	 * @throws InterruptedException if an InterruptedException prevented the computational task to complete normally
	 */
	public DefaultFuture<V> unwrapInterrupted() throws InterruptedException {
		try {
			run();
			super.get();
		}
		catch (InterruptedException ignore) {
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof InterruptedException) throw (InterruptedException) cause;
		}
		return this;
	}

	/**
	 * Waits for the computational task to be completed, and throws TimeoutException if an TimeoutException has
	 * occurred. Otherwise this future is returned to chain multiple calls.
	 *
	 * @return this future
	 * @throws TimeoutException if an TimeoutException prevented the computational task to complete normally
	 */
	public DefaultFuture<V> unwrapTimeout() throws TimeoutException {
		try {
			run();
			super.get();
		}
		catch (InterruptedException ignore) {
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof TimeoutException) throw (TimeoutException) cause;
		}
		return this;
	}
}
