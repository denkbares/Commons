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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.denkbares.utils.Pair;

/**
 * Class that decorates a progress and allows to split it into a set of multiple progresses. Each of that multiple
 * progresses may be continued independently. If all of them reaches 100%, the original one is also progressed to 100%.
 *
 * @author volker_belli
 * @created 09.09.2012
 */
public class ParallelProgress implements ReadableProgressListener {

	final ProgressListener delegate;
	PartListener[] partListeners;
	final Map<PartListener, PartListener> activeListeners = new ConcurrentHashMap<>();
	private float current = 0f;
	String lastMessage = "";

	/**
	 * Creates a new {@link ParallelProgress} for a specified delegate listener. If is divided into several sub-tasks
	 * (and a progress listener for each). The number of sub-tasks is specified by the size of partSizes. The percentage
	 * 0%..100% is split according to the specified partSizes.
	 *
	 * @param delegate     the progress listener to delegate the update to
	 * @param subTaskSizes the relative size of the individual sub-tasks
	 */
	public ParallelProgress(ProgressListener delegate, float... subTaskSizes) {
		this.delegate = delegate;
		// calculate total size
		float total = 0f;
		for (float size : subTaskSizes) {
			total += size;
		}
		// create sub-listeners according to their percentage
		final float finalTotal = total;
		createAndAddPartListeners(subTaskSizes.length, i -> subTaskSizes[i] / finalTotal);
	}

	/**
	 * Creates a new {@link ParallelProgress} for a specified delegate listener. If is divided into several sub-tasks
	 * (and a progress listener for each). The number of sub-tasks is specified by the parameter subTaskCount. The
	 * percentage 0%..100% is split into identical parts for each sub-task.
	 *
	 * @param delegate     the progress listener to delegate the update to
	 * @param subTaskCount the number of individual sub-tasks
	 */
	public ParallelProgress(ProgressListener delegate, int subTaskCount) {
		this.delegate = delegate;
		// create sub-listeners according to their percentage
		createAndAddPartListeners(subTaskCount, i -> 1f / subTaskCount);
	}

	void createAndAddPartListeners(int count, Function<Integer, Float> fractionFunction) {
		this.partListeners = new PartListener[count];
		for (int i = 0; i < count; i++) {
			this.partListeners[i] = new PartListener(fractionFunction.apply(i));
		}
	}

	/**
	 * Returns the sub-task with the specified index, 0 for the first sub-task.
	 *
	 * @param index the index of the sub-task
	 * @return the sub-task with the specified index
	 * @throws java.lang.ArrayIndexOutOfBoundsException if the specified sub-task is not one of the created sub-tasks
	 */
	public ReadableProgressListener getSubTaskProgressListener(int index) {
		return this.partListeners[index];
	}

	private void incrementProgress(double increment, String message) {
		this.current += increment;
		if (current > 0.9999f) current = 1f;
		updateProgress(this.current, message);
	}

	@Override
	public void updateProgress(float percent, String message) {
		if (message == null) {
			// just a new percentage, reuse last message
			message = lastMessage;
		}
		else if (activeListeners.size() > 1) {
			// must be multiple, concat them
			Iterator<String> messages = activeListeners.keySet().stream()
					.map(l -> new Pair<>(l.getProgress(), l.getMessage()))
					.sorted(Comparator.comparing(Pair::getA))
					.map(Pair::getB).distinct().iterator();
			StringBuilder messageBuilder = new StringBuilder();
			while (messages.hasNext()) {
				messageBuilder.append(messages.next());
				if (messages.hasNext()) messageBuilder.append(" | ");
			}
			message = messageBuilder.toString();
		}
		lastMessage = message;
		// write through to delegate, but not update current percentage
		this.delegate.updateProgress(percent, message);
	}

	/**
	 * Utility method to easily increment a progress of a special sub-task.
	 *
	 * @param subTaskIndex the index of the sub-task
	 * @param percent      the fraction of the progress of this sub-task (1.0 for 100%)
	 * @param message      the message to be applied
	 * @throws java.lang.ArrayIndexOutOfBoundsException if the specified sub-task is not one of the created sub-tasks
	 * @see #getSubTaskProgressListener(int)
	 */
	public void updateProgress(int subTaskIndex, float percent, String message) {
		getSubTaskProgressListener(subTaskIndex).updateProgress(percent, message);
	}

	/**
	 * Utility method to easily increment a progress of a special sub-task. The previously set message is reused.
	 *
	 * @param subTaskIndex the index of the sub-task
	 * @param percent      the fraction of the progress of this sub-task (1.0 for 100%)
	 * @throws java.lang.ArrayIndexOutOfBoundsException if the specified sub-task is not one of the created sub-tasks
	 * @see #getSubTaskProgressListener(int)
	 */
	public void updateProgress(int subTaskIndex, float percent) {
		getSubTaskProgressListener(subTaskIndex).updateProgress(percent, lastMessage);
	}

	@Override
	public float getProgress() {
		return current;
	}

	@Override
	public String getMessage() {
		return lastMessage;
	}

	class PartListener implements ReadableProgressListener {

		private final float fraction;
		private float current = 0f;
		private String currentMessage = null;
		private float partCurrent = 0f;

		public PartListener(float fraction) {
			this.fraction = fraction;
		}

		@Override
		public void updateProgress(float percent, String message) {
			if (percent > 1f) percent = 1f;
			if (percent < 0f) percent = 0f;
			//noinspection FloatingPointEquality
			if (percent == 1f) {
				activeListeners.remove(this);
			}
			else if (message != null) {
				currentMessage = message;
				activeListeners.put(this, this);
			}
			this.partCurrent = percent;
			float absolute = percent * fraction;
			float delta = absolute - current;
			this.current = absolute;
			incrementProgress(delta, message);
		}

		@Override
		public float getProgress() {
			return partCurrent;
		}

		@Override
		public String getMessage() {
			return currentMessage;
		}
	}
}
