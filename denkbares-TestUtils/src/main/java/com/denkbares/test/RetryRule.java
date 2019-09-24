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
