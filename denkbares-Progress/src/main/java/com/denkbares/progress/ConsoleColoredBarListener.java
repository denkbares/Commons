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

import java.util.Locale;

/**
 * A simple ProgressListener that prints the progress to the console:
 * <p>
 * |████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░|  20.1%  still working
 *
 * @author Volker Belli (denkbares GmbH)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ConsoleColoredBarListener extends AbstractConsoleProgressBarListener {

	private static final char CHAR_FOREGROUND = '\u2588';    // █;
	private static final char CHAR_BACKGROUND = '\u2591';    // ░;

	private static final String COLOR_GREEN = "\u001b[32;1m";
	private static final String COLOR_GRAY = "\u001b[37m";
	private static final String COLOR_RESET = "\u001b[0m";

	private final boolean useColors;

	public ConsoleColoredBarListener() {
		this(30, true, false, false);
	}

	public ConsoleColoredBarListener(int totalBarSize, boolean printHeader, boolean useColors, boolean printEveryNewMessage) {
		super(totalBarSize, printHeader, printEveryNewMessage);
		this.useColors = useColors;
	}

	@Override
	protected void printProgress() {
		int barSize = Math.round(percent * SIZE);
		System.out.print("|");

		if (useColors) System.out.print(COLOR_GREEN);
		for (int i = 0; i < barSize; i++) {
			System.out.print(CHAR_FOREGROUND);
		}

		if (useColors) System.out.print(COLOR_GRAY);
		for (int i = barSize; i < SIZE; i++) {
			System.out.print(CHAR_BACKGROUND);
		}

		if (useColors) System.out.print(COLOR_RESET);
		System.out.print("| ");
		System.out.format(Locale.ENGLISH, "%5.1f%%  ", this.percent * 100);
		System.out.print(message);
	}
}
