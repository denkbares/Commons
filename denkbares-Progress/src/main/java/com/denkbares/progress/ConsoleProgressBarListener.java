/*
 * Copyright (C) 2009 denkbares GmbH
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

import java.util.Locale;

/**
 * A simple ProgressListener that prints the progress to the console
 *
 * @author Volker Belli (denkbares GmbH)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ConsoleProgressBarListener implements ProgressListener {

	private static final int SIZE = 50;
	private static final float UPDATE_DELTA = 0.001f;

	private float percent = Float.NaN;
	private String message = null;

	@Override
	public void updateProgress(float percent, String message) {
		if (Float.isNaN(this.percent)) {
			// the progress bar is not yet initialized, so print initialization
			System.out.println();
			System.out.println(message);
			System.out.print("| 0 % ");
			for (int i = 0; i < (SIZE - 18) / 2; i++) {
				System.out.print("\u00AF");
			}
			System.out.print(" 50 % ");
			for (int i = 0; i < (SIZE - 18) / 2; i++) {
				System.out.print("\u00AF");
			}
			System.out.println(" 100 % |");
			message = null;
		}
		else if (percent < 1f && (percent <= this.percent + UPDATE_DELTA) && (message == null || message.equals(this.message))) {
			// no update required
			return;
		}
		else if (this.percent >= 1f) {
			// we already completed the progress bar (with return) and cannot update any longer
			return;
		}

		this.percent = Math.min(Math.max(0f, percent), 1f);
		int barSize = Math.round(percent * SIZE);
		System.out.print("|");
		for (int i = 1; i <= SIZE; i++) {
			System.out.print(i <= barSize ? "=" : " ");
		}
		System.out.print("| ");
		System.out.format(Locale.ENGLISH, "%5.1f%% ", this.percent * 100);
		if (message != null) {
			System.out.print(message);
			this.message = message;
		}
		if (this.percent >= 1f) {
			if (message == null || message.isEmpty()) System.out.print("done");
			System.out.println();
		}
		else {
			System.out.print("\r");
		}
	}
}
