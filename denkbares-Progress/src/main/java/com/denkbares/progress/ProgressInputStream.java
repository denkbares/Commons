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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends FilterInputStream {

	private final ProgressListener listener;
	private final String message;
	private long processed = 0;

	public ProgressInputStream(InputStream in, ProgressListener listener) {
		this(in, listener, null);
	}

	public ProgressInputStream(InputStream in, ProgressListener listener, String message) {
		super(in);
		this.listener = listener;
		this.message = message;
	}

	private void updateProgess(long count) throws IOException {
		processed += count;
		int available = available();
		float progress = processed / (float) (available + processed);
		listener.updateProgress(progress, message);
	}

	@Override
	public int read() throws IOException {
		int data = super.read();
		updateProgess(1);
		return data;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = super.read(b, off, len);
		updateProgess(count);
		return count;
	}

	@Override
	public long skip(long n) throws IOException {
		long count = super.skip(n);
		updateProgess(count);
		return count;
	}

	@Override
	public void close() throws IOException {
		super.close();
		listener.updateProgress(1f, message);
	}
}
