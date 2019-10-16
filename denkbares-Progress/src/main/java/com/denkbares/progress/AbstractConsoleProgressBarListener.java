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
package com.denkbares.progress;

import com.denkbares.utils.Stopwatch;

/**
 * A simple ProgressListener that prints the progress to the console
 *
 * @author Volker Belli (denkbares GmbH)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public abstract class AbstractConsoleProgressBarListener implements ProgressListener {

	protected static final float UPDATE_DELTA = 0.01f;
	protected final int SIZE;

	protected final boolean printHeader;
	protected final Stopwatch stopwatch = new Stopwatch();

	protected float percent = Float.NaN;
	protected String message = "";

	public AbstractConsoleProgressBarListener() {
		this(50, true);
	}

	public AbstractConsoleProgressBarListener(int totalBarSize, boolean printHeader) {
		this.SIZE = totalBarSize;
		this.printHeader = printHeader;
	}

	@Override
	public void updateProgress(float percent, String message) {
		// avoid conflicting error outputs
		System.err.flush();

		// update the message
		if (percent >= 1f) message = "completed in " + stopwatch.getDisplay();
		boolean newMessage = (message != null) && !message.equals(this.message);
		if (newMessage) this.message = message;

		// check for state, (de-)initialize only once
		if (Float.isNaN(this.percent)) {
			stopwatch.start();
			// the progress bar is not yet initialized, so print initialization
			printHeader();
			System.out.flush();
		}
		else if (percent < 1f && (percent <= this.percent + UPDATE_DELTA) && !newMessage) {
			// no update required
			return;
		}
		else if (this.percent >= 1f) {
			// we already completed the progress bar (with return) and cannot update any longer
			return;
		}

		this.percent = Math.min(Math.max(0f, percent), 1f);
		printProgress();

		if (this.percent >= 1f) {
			System.out.println();
		}
		else {
			System.out.print("\r");
		}
		System.out.flush();
	}

	protected void printHeader() {
		// print message if requested
		if (printHeader) {
			System.out.println();
			System.out.println(message);
			// do not print the init message behind the progress bar, only if explicitly repeated
			message = "";
		}
	}

	protected abstract void printProgress();
}
