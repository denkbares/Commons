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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

import com.denkbares.strings.Strings;

/**
 * A stream that is capable to replace detect occurrence of <code>${...}</code> while the stream is written and replace
 * the content between the brackets by the result of a defined replace function.
 */
public class ReplacingOutputStream extends OutputStream {

	private OutputStream delegate;
	private final Charset charset;
	private final Function<String, String> replace;

	// while buffer is null, we are not in a potential replacement
	private ByteArrayOutputStream buffer = null;
	private boolean startedDollar = false;
	private boolean startedBracket = false;

	/**
	 * Creates a new replacing stream for the specified stream and the specified replace function. The function will be
	 * called with the contents in between the <code>${...}</code> of each occurrence. If the same expression occurs
	 * multiple time in the stream, the function is called on each occurrence (any may potentially return different
	 * values). If the function returns an empty string, (naturally) the variable is removed from the stream. If the
	 * function returns null, the variable declaration is skipped and not replaced at all.
	 * <p>
	 * The stream is assumed to be UTF-8 encoded.
	 *
	 * @param delegate the stream to replace the variables on
	 * @param replace  the function to calculate the replacement string
	 */
	public ReplacingOutputStream(OutputStream delegate, Function<String, String> replace) {
		this(delegate, Strings.Encoding.UTF8.charset(), replace);
	}

	/**
	 * Creates a new replacing stream for the specified stream and the specified replace function. The function will be
	 * called with the contents in between the <code>${...}</code> of each occurrence. If the same expression occurs
	 * multiple time in the stream, the function is called on each occurrence (any may potentially return different
	 * values). If the function returns an empty string, (naturally) the variable is removed from the stream. If the
	 * function returns null, the variable declaration is skipped and not replaced at all.
	 *
	 * @param delegate the stream to replace the variables on
	 * @param charset  the charset to use to convert between the stream's bytes and the replace function
	 * @param replace  the function to calculate the replacement string
	 */
	public ReplacingOutputStream(OutputStream delegate, Charset charset, Function<String, String> replace) {
		this.delegate = delegate;
		this.charset = charset;
		this.replace = replace;
	}

	public @Override
	void write(int b) throws IOException {
		if (delegate == null) return;

		if (!startedDollar && b == '$') {
			startedDollar = true;
		}
		else if (startedDollar && !startedBracket && b == '{') {
			startedBracket = true;
			buffer = new ByteArrayOutputStream();
		}
		else if (startedDollar && startedBracket && b == '}') {
			executeBuffer();
		}
		else if (buffer != null) {
			buffer.write(b);
		}
		else {
			if (startedDollar) writeBuffer();
			delegate.write(b);
		}
	}

	private void executeBuffer() throws IOException {
		try {
			String buffered = new String(buffer.toByteArray(), charset);
			String result = replace.apply(buffered);
			if (result == null) {
				// if the replace function returns null, it was not handled, and should be not replaced
				writeBuffer();
				// do not forget the consumed closing '}'
				delegate.write('}');
			}
			else {
				// otherwise write the replaced string instead of the buffer
				delegate.write(result.getBytes(charset));
			}
		}
		finally {
			resetBuffer();
		}
	}

	private void writeBuffer() throws IOException {
		try {
			if (startedDollar) {
				delegate.write((int) '$');
			}
			if (startedBracket) {
				delegate.write((int) '{');
			}
			if (buffer != null) {
				buffer.writeTo(delegate);
			}
		}
		finally {
			resetBuffer();
		}
	}

	private void resetBuffer() {
		startedDollar = false;
		startedBracket = false;
		buffer = null;
	}

	@Override
	public void flush() throws IOException {
		if (delegate != null) {
			writeBuffer();
			delegate.flush();
		}
		super.flush();
	}

	@Override
	public void close() throws IOException {
		if (delegate != null) {
			writeBuffer();
			delegate.close();
		}
		super.close();
	}

	/**
	 * Disconnects the replacement stream from the underlying stream. It makes sure that all buffered information of
	 * this stream is written to the underlying stream, but no futher write, close or flush actions will be applied from
	 * this stream to the underlying one.
	 *
	 * @throws IOException if the buffered bytes could not been written before disconnecting
	 */
	public void disconnect() throws IOException {
		if (delegate != null) {
			writeBuffer();
			delegate = null;
		}
	}
}
