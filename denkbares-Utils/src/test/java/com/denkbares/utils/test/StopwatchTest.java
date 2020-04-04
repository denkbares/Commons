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

package com.denkbares.utils.test;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.denkbares.utils.Stopwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 14.01.2015
 */
public class StopwatchTest {

	@Test
	public void basic() throws InterruptedException {
		// basic timing
		Stopwatch time = new Stopwatch();
		Thread.sleep(10);
		assertAtLeast(time, 10);

		// reset and automatically stop
		time.reset();
		assertEquals(0, time.getTime());
		Thread.sleep(1);
		assertEquals(0, time.getTime());

		// resume multiple times
		time.resume();
		Thread.sleep(1);
		assertAtLeast(time, 1);
		time.resume();
		Thread.sleep(1);
		assertAtLeast(time, 2);
		time.resume();
		Thread.sleep(1);
		assertAtLeast(time, 3);

		// check pause
		time.pause();
		assertAtLeast(time, 3);
		long temp = time.getTime();
		Thread.sleep(1);
		assertEquals(time.getTime(), temp);

		// check resume after pause
		time.resume();
		Thread.sleep(1);
		assertAtLeast(time, temp + 1);
	}

	@Test
	public void display() {
		Stopwatch time = new Stopwatch();
		time.reset();
		assertEquals("0ms", time.getDisplay());
		assertEquals("0ms", time.getDisplay(TimeUnit.MILLISECONDS));
		assertEquals("0.000s", time.getDisplay(TimeUnit.SECONDS));
		assertEquals("0:00 min", time.getDisplay(TimeUnit.MINUTES));
		assertEquals("0:00:00 hours", time.getDisplay(TimeUnit.HOURS));
		assertEquals("0 days 0:00:00 hours", time.getDisplay(TimeUnit.DAYS));

		long durM1 = TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("2:01 min", Stopwatch.getDisplay(durM1));

		long durM2 = TimeUnit.MINUTES.toMillis(60) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("60:01 min", Stopwatch.getDisplay(durM2));

		long durM3 = TimeUnit.MINUTES.toMillis(120) + TimeUnit.SECONDS.toMillis(0);
		assertEquals("120:00 min", Stopwatch.getDisplay(durM3));

		long durM4 = TimeUnit.MINUTES.toMillis(120) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("2:00:01 hours", Stopwatch.getDisplay(durM4));

		long durH1 = TimeUnit.HOURS.toMillis(30) + TimeUnit.MINUTES.toMillis(12) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("30:12:01 hours", Stopwatch.getDisplay(durH1));

		long durH2 = TimeUnit.HOURS.toMillis(49) + TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("2 days 1:02:01 hours", Stopwatch.getDisplay(durH2));

		long durD1 = TimeUnit.DAYS.toMillis(49) + TimeUnit.MINUTES.toMillis(2) + TimeUnit.SECONDS.toMillis(1);
		assertEquals("49 days 0:02:01 hours", Stopwatch.getDisplay(durD1));

		assertEquals("Elapsed time: 0ms", time.toString());
	}

	public void assertAtLeast(Stopwatch timer, long minTime) {
		long time = timer.getTime();
		assertTrue("time of " + time + " is not >=" + minTime, time >= minTime);
	}
}
