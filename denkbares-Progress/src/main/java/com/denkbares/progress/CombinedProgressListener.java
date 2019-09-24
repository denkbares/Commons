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

import org.jetbrains.annotations.Nullable;

/**
 * Combines multiple progresses in one ProgressListener.
 * <p>
 * The progress is divided into a set of certain steps with a specified size each. If the progress is updated (by 0% to
 * 100%) only the part of the current step is progressed.
 * <p>
 * Example: you created a CombinedProgressListener of total size 100. The you finished the first step with step size 20
 * and proceeding with the next step of size 30. Updating the progress to 0% will lead to a percentage of 20% in the
 * decorated ProgressListener, while updating the progress to 100% will lead to a percentage of 50%.
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public class CombinedProgressListener implements ProgressListener {

	private final long totalSize;

	private long currentStepStart = 0;
	private long currentStepSize = 0;
	private final ProgressListener decoratedListener;
	private String prefix = null;

	public CombinedProgressListener(long totalSize, ProgressListener decoratedListener) {
		this.totalSize = totalSize;
		this.decoratedListener = decoratedListener;
	}

	@Override
	public void updateProgress(float percent, String message) {
		if (totalSize < 0) {
			throw new IllegalArgumentException("totalSize has not been set before update");
		}
		decoratedListener.updateProgress(
				(percent * currentStepSize + currentStepStart) / totalSize,
				(message == null || prefix == null) ? message : prefix + message);
	}

	/**
	 * Indicates that the next step of this progress has begun. You need to specify what amount of the total size this
	 * step will take.
	 *
	 * @param size size of this step (part of the total size)
	 */
	public void next(long size) {
		currentStepStart += currentStepSize;
		currentStepSize = size;
	}

	/**
	 * Indicates that the last step of this progress has begun. No need to specify the size of the last step, we will
	 * just use the rest.
	 */
	public void last() {
		currentStepStart += currentStepSize;
		currentStepSize = totalSize - currentStepStart;
	}

	/**
	 * Sets a prefix message that is prepended before each updated message. The prefix must also contain any desired
	 * punctuation and spacing that should occur between the message and prefix, so the prefix usually looks like
	 * <code>"...: "</code>.
	 *
	 * @param prefix the prefix message to be prepended, or null for no prefix
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = prefix;
	}
}
