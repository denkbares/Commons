/*
 * Copyright (C) 2022 denkbares GmbH, Germany
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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.concurrent.TimeUnit.*;

/**
 * Scheduler that can be used if a task should only be run if something happened recently. To indicate, that this
 * "thing" happened, {@link #notifyUsage()} can be called.
 * <p>
 * <b>Attention: Memory-Leak-Potential!</b><br> Since the task will be stored for a while, don't use tasks/runnables that have
 * references to objects with a large memory footprint or that have a very specific live cycle! If you must, at least wrap
 * them into a WeakReference, so they can be cleaned up!
 * </p>
 * Created by Albrecht on 15.06.2017.
 */
public class UsageBasedScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(UsageBasedScheduler.class);

	private final ScheduledExecutorService scheduler;
	private String name;
	private final ExecutorService executorService;
	private final long triggerWithingMillis;
	private final boolean runAtNightAnyway;
	private final Set<Runnable> runnables = Collections.synchronizedSet(new HashSet<>());
	private long lastUsage = Long.MIN_VALUE;

	/**
	 * Creates a new scheduler.
	 *
	 * @param name                 the name of this scheduler, so it can be identified easily when debugging
	 * @param executorService      the ExecutorService running the scheduled runnable asynchronously
	 * @param triggerWithingMillis if a runnable is scheduled within the given duration in milliseconds since the last
	 *                             call to {@link #notifyUsage()}, it is executed immediately
	 * @param runAtNightAnyway     decides whether a scheduled runnable should be run at night, if there wasn't any
	 *                             call to {@link #notifyUsage()} in the mean time
	 */
	public UsageBasedScheduler(String name, ExecutorService executorService, long triggerWithingMillis, boolean runAtNightAnyway) {
		this.name = name;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, name);
			thread.setDaemon(true);
			return thread;
		});
		this.executorService = executorService;
		this.triggerWithingMillis = triggerWithingMillis;
		this.runAtNightAnyway = runAtNightAnyway;
	}

	/**
	 * Schedules the runnable to be executed asynchronously (call do schedule will return immediately).
	 * If {@link #notifyUsage()} was called recently (within duration given in constructor), the runnable will be
	 * executed immediately. If not, execution of the runnable will be delayed either until some random time at night
	 * (if so specified in constructor), or when the methode {@link #notifyUsage()} is called the next time.
	 *
	 * @param runnable the runnable to be scheduled
	 */
	public void schedule(Runnable runnable) throws ExecutionException, InterruptedException {
		// usage not so far back, run immediately
		long durationSinceLastUsage = System.currentTimeMillis() - lastUsage;
		if (lastUsage > Long.MIN_VALUE && durationSinceLastUsage < triggerWithingMillis) {
			executorService.execute(runnable);
		}
		// if desired, make sure that we run some time tonight at the latest
		else if (runAtNightAnyway) {
			int currentHour = Calendar.getInstance().get(HOUR_OF_DAY);
			long delay = HOURS.toMillis(24 - currentHour)  // till after midnight
						 + MINUTES.toMillis((long) (Math.random() * 300)); // another 0 - 5 hours

			scheduler.schedule(() -> {
						// check if the runnable was already executed in #notifyUsage
						if (runnables.remove(runnable)) {
							executorService.execute(runnable);
						}
					},
					delay, MILLISECONDS);

			runnables.add(runnable);
			Date date = new Date(System.currentTimeMillis() + delay);
			if (lastUsage == Long.MIN_VALUE) {
				LOGGER.info(name + " was not yet used, scheduling to run " + date);
			}
			else {
				LOGGER.info("Last notification for " + name + " was " + Stopwatch.getDisplay(durationSinceLastUsage)
							+ " ago, scheduling to run task " + date);
			}
		}
		// or just wait for the next usage
		else {
			runnables.add(runnable);
			LOGGER.info("Last notification for " + name + " was " + Stopwatch.getDisplay(durationSinceLastUsage)
						+ " ago, waiting for next usage do run task.");
		}
	}

	/**
	 * Notifies the scheduler, that a new access/usage occurred.
	 */
	public void notifyUsage() throws ExecutionException, InterruptedException {
		//this.lastUsage = System.currentTimeMillis();
		synchronized (runnables) {
			for (Runnable runnable : runnables) {
				executorService.execute(runnable);
			}
			runnables.clear();
		}
	}
}
