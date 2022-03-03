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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rule allowing for tests to run a defined number of times. Prints failures along the way.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 03.07.15
 */
public class RerunRule implements TestRule {
	private static final Logger LOGGER = LoggerFactory.getLogger(RerunRule.class);

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
						LOGGER.error("Run " + (i + 1) + "/" + rerunCount + " of '" + description.getDisplayName() + "' successful");
					}
					catch (Throwable throwable) {
						caughtThrowable = throwable;
						LOGGER.error("Run " + (i + 1) + "/" + rerunCount + " of '" + description.getDisplayName() + "' failed", throwable);
					}
				}
				LOGGER.error("Final statistic for " + description.getDisplayName() + ": " + successes + "/" + rerunCount + " successes");
				if (caughtThrowable != null) throw caughtThrowable;
			}
		};
	}
}
