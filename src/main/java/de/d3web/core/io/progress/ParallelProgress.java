/*
 * Copyright (C) 2012 denkbares GmbH, Germany
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
package de.d3web.core.io.progress;

/**
 * Class that decorates a progress and allows to split it into a set of multiple
 * progresses. Each of that multiple progresses may be continued independently.
 * If all of them reaches 100%, the original one is also progressed to 100%.
 * 
 * @author volker_belli
 * @created 09.09.2012
 */
public class ParallelProgress implements ProgressListener {

	private class PartListener implements ProgressListener {

		private float current = 0f;
		private final float fraction;

		public PartListener(float fraction) {
			this.fraction = fraction;
		}

		@Override
		public void updateProgress(float percent, String message) {
			if (percent > 1f) percent = 1f;
			if (percent < 0f) percent = 0f;
			float absolute = percent * fraction;
			float delta = absolute - current;
			this.current = absolute;
			incrementProgress(delta, message);
		}
	}

	private final ProgressListener delegate;
	private float current = 0f;
	private final PartListener[] partListeners;

	/**
	 * Creates a new {@link ParallelProgress} for a specified delegate listener.
	 * If is divided into several sub-tasks (and a progress listener for each).
	 * The number of sub-tasks is specified by the size of partSizes. The
	 * percentage 0%..100% is split according to the specified partSizes.
	 * 
	 * @param delegate the progress listener to delegate the update to
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
		this.partListeners = new PartListener[subTaskSizes.length];
		for (int i = 0; i < subTaskSizes.length; i++) {
			this.partListeners[i] = new PartListener(subTaskSizes[i] / total);
		}
	}

	/**
	 * Creates a new {@link ParallelProgress} for a specified delegate listener.
	 * If is divided into several sub-tasks (and a progress listener for each).
	 * The number of sub-tasks is specified by the parameter subTaskCount. The
	 * percentage 0%..100% is split into identical parts for each sub-task.
	 * 
	 * @param delegate the progress listener to delegate the update to
	 * @param subTaskCount the number of individual sub-tasks
	 */
	public ParallelProgress(ProgressListener delegate, int subTaskCount) {
		this.delegate = delegate;
		// create sub-listeners according to their percentage
		this.partListeners = new PartListener[subTaskCount];
		for (int i = 0; i < subTaskCount; i++) {
			this.partListeners[i] = new PartListener(1f / subTaskCount);
		}
	}

	public ProgressListener getSubTaskProgressListener(int index) {
		return this.partListeners[index];
	}

	private void incrementProgress(double increment, String message) {
		this.current += increment;
		if (current > 0.9999f) current = 1f;
		updateProgress(this.current, message);
	}

	@Override
	public void updateProgress(float percent, String message) {
		// write through to delegate, but not update current percentage
		this.delegate.updateProgress(percent, message);
	}

}
