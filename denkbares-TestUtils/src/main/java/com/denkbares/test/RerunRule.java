/*
 * Copyright (C) 2017 denkbares GmbH. All rights reserved.
 */

package com.denkbares.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.denkbares.utils.Log;

/**
 * Rule allowing for tests to run a defined number of times. Prints failures along the way.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 03.07.15
 */
public class RerunRule implements TestRule {

	private final int rerunCount;
	private int successes;

	public RerunRule(int rerunCount) {
		this.rerunCount = rerunCount;
		this.successes = 0;
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
				for (int i = 0; i < rerunCount; i++) {
					try {
						base.evaluate();
						successes++;
						Log.severe("Run " + (i + 1) + "/" + rerunCount + " of '" + description.getDisplayName() + "' successful");
					}
					catch (Throwable throwable) {
						caughtThrowable = throwable;
						Log.severe("Run " + (i + 1) + "/" + rerunCount + " of '" + description.getDisplayName() + "' failed", throwable);
					}
				}
				Log.severe("Final statistic for " + description.getDisplayName() + ": " + successes + "/" + rerunCount + " successes");
				if (caughtThrowable != null) throw caughtThrowable;
			}
		};
	}
}
