/*
 * Copyright (C) 2017 denkbares GmbH. All rights reserved.
 */

package com.denkbares.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.denkbares.utils.Log;
import com.denkbares.utils.Stopwatch;

/**
 * Rule allowing for retries if a test fails.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 03.07.15
 */
public class RetryRule implements TestRule {

	private final int retryCount;

	public RetryRule(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return statement(base, description);
	}

	private Statement statement(final Statement base, final Description description) {

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				Throwable caughtThrowable = null;
				Stopwatch timer = new Stopwatch().reset();
				for (int i = 0; i < retryCount; i++) {
					timer.start();
					try {
						base.evaluate();
						Log.info("Retry number " + i + 1 + ": " + timer.getDisplay());
						timer.reset();
						return;
					}
					catch (Throwable t) {
						timer.reset();
						caughtThrowable = t;
						Log.severe("Run " + (i + 1) + "/" + retryCount + " of '" + description.getDisplayName() + "' failed", t);
					}

				}
				Log.severe("Giving up after " + retryCount + " failures of '" + description.getDisplayName() + "'");
				if (caughtThrowable != null) {
					throw caughtThrowable;
				}
			}
		};
	}
}
