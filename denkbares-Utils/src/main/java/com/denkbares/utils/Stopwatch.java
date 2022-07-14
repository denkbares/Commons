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

package com.denkbares.utils;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Utility class to measure some time with some more comfort as direct accessing
 * System.currentTimeMillis and formatting it manually.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 14.01.2015
 */
public class Stopwatch {

	private long startTime = System.currentTimeMillis();
	private long pausedTime = 0;

	/**
	 * Creates a new Stopwatch. The created stopwatch will be automatically started. If you do not
	 * want to start it manually later on, ignore that, calling start() will automatically stop and
	 * reset it. Alternatively you can use <code>Stopwatch timer = new Stopwatch().reset()</code> to
	 * create a not started watch.
	 */
	public Stopwatch() {
	}

	/**
	 * Resets the stopwatch to 0. The stopwatch is also automatically stopped (paused). The method
	 * returns this stopwatch afterwards to allow cascading calls.
	 */
	public Stopwatch reset() {
		this.startTime = -1;
		this.pausedTime = 0;
		return this;
	}

	/**
	 * Starts time measurement. Before starting the watch is automatically stopped and resetted. The
	 * method returns this stopwatch afterwards to allow cascading calls.
	 */
	public Stopwatch start() {
		this.startTime = System.currentTimeMillis();
		this.pausedTime = 0;
		return this;
	}

	/**
	 * Pauses the time measurement, remembering the yet elapsed time. The method returns this
	 * stopwatch afterwards to allow cascading calls.
	 */
	public Stopwatch pause() {
		long elapsed = (startTime == -1) ? 0 : (System.currentTimeMillis() - startTime);
		this.pausedTime += elapsed;
		this.startTime = -1;
		return this;
	}

	/**
	 * Resumes the previously paused time measurement. If it was not paused or stopped before, the
	 * method does nothing. The method returns this stopwatch afterwards to allow cascading calls.
	 */
	public Stopwatch resume() {
		if (this.startTime == -1) {
			this.startTime = System.currentTimeMillis();
		}
		return this;
	}

	/**
	 * Shows the currently measured time with some message. The method returns this stopwatch
	 * afterwards to allow cascading calls.
	 *
	 * @param message the message to be printed before the time
	 */
	public Stopwatch show(String message) {
		//noinspection UseOfSystemOutOrSystemErr
		System.out.println(message + ": " + getDisplay());
		return this;
	}

	/**
	 * Shows the currently measured time with some message in the default log with log level info.
	 * The method returns this stopwatch afterwards to allow cascading calls.
	 *
	 * @param message the message to be printed before the time
	 */
	public Stopwatch log(String message) {
		Log.mock(1, Level.INFO, message + ": " + getDisplay());
		return this;
	}

	/**
	 * Shows the currently measured time with some message in the default log with log level info.
	 * The method returns this stopwatch afterwards to allow cascading calls.
	 *
	 * @param message the message to be printed before the time
	 */
	public Stopwatch log(org.slf4j.Logger logger, String message) {
		logger.info(message + ": " + getDisplay());
		return this;
	}

	/**
	 * Returns the currently measured time in milliseconds
	 *
	 * @return the measure time
	 */
	public long getTime() {
		long elapsed = (startTime == -1) ? 0 : (System.currentTimeMillis() - startTime);
		return elapsed + pausedTime;
	}

	/**
	 * Returns the currently measured time in some auto-detected appropriate time unit as a string.
	 *
	 * @return the measure time as a display string
	 */
	public String getDisplay() {
		long time = getTime();
		return getDisplay(time);
	}

	public static String getDisplay(long time) {
		if (time > TimeUnit.DAYS.toMillis(2)) {
			return getDisplay(time, TimeUnit.DAYS);
		}
		else if (time > TimeUnit.HOURS.toMillis(2)) {
			return getDisplay(time, TimeUnit.HOURS);
		}
		else if (time > TimeUnit.MINUTES.toMillis(1)) {
			return getDisplay(time, TimeUnit.MINUTES);
		}
		else if (time > TimeUnit.SECONDS.toMillis(1)) {
			return getDisplay(time, TimeUnit.SECONDS);
		}
		return getDisplay(time, TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns the currently measured time according the specified preferred time unit as a string.
	 *
	 * @return the measure time as a display string
	 */
	public String getDisplay(TimeUnit unit) {
		return getDisplay(getTime(), unit);
	}

	public static String getDisplay(long time, TimeUnit unit) {
		return switch (unit) {
			case NANOSECONDS, MICROSECONDS, MILLISECONDS -> time + "ms";
			case SECONDS -> String.format("%d.%03ds", time / 1000, time % 1000);
			case MINUTES -> String.format("%d:%02d min",
					TimeUnit.MINUTES.convert(time, TimeUnit.MILLISECONDS),
					getRemainingSeconds(time));
			case HOURS -> String.format("%d:%02d:%02d hours",
					TimeUnit.HOURS.convert(time, TimeUnit.MILLISECONDS),
					getRemainingMinutes(time), getRemainingSeconds(time));
			case DAYS -> String.format("%d days %d:%02d:%02d hours",
					TimeUnit.DAYS.convert(time, TimeUnit.MILLISECONDS),
					getRemainingHours(time), getRemainingMinutes(time), getRemainingSeconds(time));
		};
	}

	private static long getRemainingHours(long time) {
		return (time % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
	}

	private static long getRemainingSeconds(long time) {
		return (time % (60 * 1000)) / (1000);
	}

	private static long getRemainingMinutes(long time) {
		return (time % (60 * 60 * 1000)) / (60 * 1000);
	}

	@Override
	public String toString() {
		return "Elapsed time: " + getDisplay();
	}
}
