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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple ProgressListener that delegates/broadcasts any progress updates to multiple other progress listeners.
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public class MultiProgressListener implements ProgressListener {

	private final List<ProgressListener> listeners;

	public MultiProgressListener(ProgressListener... listeners) {
		// use concurrent list, to avoid concurrent modifications in multi-threaded environments
		this.listeners = new CopyOnWriteArrayList<>(listeners);
	}

	/**
	 * Adds the specified progress listener to this instance, to broadcast all future updates to.
	 *
	 * @param listener the listener to be added
	 */
	public void addListener(ProgressListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes the specified progress listener from this instance, to no longer broadcast future updates to.
	 *
	 * @param listener the listener to be removed
	 */
	public void removeListener(ProgressListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void updateProgress(float percent, String message) {
		for (ProgressListener listener : listeners) {
			listener.updateProgress(percent, message);
		}
	}
}
