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

/**
 * A simple ProgressListener that prints the progress to the console, in steps of 5% without appending new lines or
 * message texts.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 10.01.2018
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ConsoleSingleLineProgressListener implements ProgressListener {

	private int previousStep = -1;
	private static final int STEPS = 20;

	@Override
	public void updateProgress(float percent, String message) {
		// determine current step and return if not changed
		percent += 0.0001; // slightly round up: x.99% is sufficient for next percentage
		int step = (int) (percent * STEPS);
		if (previousStep == step) return;

		// print the newly reached step
		if (step == 0 && message != null) System.out.println(message);
		if (previousStep >= 0) System.out.print(" .. ");
		System.out.print((int) (percent * 100) + "%");
		previousStep = step;

		// and add line break if the process has finished
		if (step == STEPS) {
			System.out.println();
			if (message != null) System.out.println(message);
		}
	}
}
