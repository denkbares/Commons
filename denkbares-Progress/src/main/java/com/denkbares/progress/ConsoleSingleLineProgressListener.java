/*
 * Copyright (C) 2018 denkbares GmbH. All rights reserved.
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
