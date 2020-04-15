/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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
 * A readable progress listener that simply records the current progress, and provides the current progress vio API
 * interface.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.04.2020
 */
public class ProgressInfo implements ReadableProgressListener {

	private float progress = 0;
	private String message = "";

	@Override
	public float getProgress() {
		return progress;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void updateProgress(float percent, @Nullable String message) {
		this.progress = percent;
		if (message != null) this.message = message;
	}
}
