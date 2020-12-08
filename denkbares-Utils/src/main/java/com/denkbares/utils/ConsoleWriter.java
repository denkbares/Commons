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

package com.denkbares.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.denkbares.strings.Strings;

/**
 * Utility class that is a writer, but interprets all written characters as VT220 terminal codes and collects the total
 * output similar to a real shell console. It e.g. allows to overwrite existing text by moving the cursor other than
 * strictly monotonic increasing.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 07.12.2020
 */
public class ConsoleWriter extends Writer {

	// the console screen
	private final List<LineBuilder> lines = new ArrayList<>();

	// the cursor position
	private int x = 0;
	private int y = 0;

	// notification on changed lines
	private final Set<Integer> changes = new ConcurrentSkipListSet<>(); // we require the ordering of this set type!!!
	private final List<LineConsumer> listeners = new ArrayList<>();

	// if empty no escape sequence started yet
	private final StringBuilder escape = new StringBuilder();
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\u001b(?:\\[(\\d*))?(?:;(\\d+))?([a-zA-Z])");

	/**
	 * Adds a listener for line changes. The listener will get informed every time a line changes. It receives the line
	 * number (starting from zero) and the content of the line. Depending on the implemented notification strategy, the
	 * consumer may be informed more or less often. Usually it will get informed on carriage return or vertical cursor
	 * movements, but not on every changed character.
	 *
	 * @param listener the listener to get informed on all future changes
	 */
	public void addListener(LineConsumer listener) {
		listeners.add(listener);
	}

	private void sendChanges() {
		Iterator<Integer> iterator = changes.iterator();
		while (iterator.hasNext()) {
			// consume the next item, before we call the consumer, to be potentially added again (asynchronously)
			int lineNo = iterator.next();
			iterator.remove();

			// fetch the text of the line
			CharSequence line = (lines.size() > lineNo) ? lines.get(lineNo).line : "";
			for (LineConsumer listener : listeners) {
				listener.accept(lineNo, line);
			}
		}
	}

	private void notifyChange(LineBuilder line) {
		changes.add(line.lineNo);
	}

	private void interpret(char c) {
		// build already started escape sequence
		if (escape.length() > 0) {
			escape.append(c);
			// if the escape sequence is terminated (non-inner-escape character: '[' or ';' or digit), handle it
			if (c != '[' && c != ';' && !Character.isDigit(c)) consumeEscape();
			return;
		}

		// otherwise interpret the character
		switch (c) {
			// start escape sequence
			case '\u00b1':
				escape.append(c);
				break;

			// handle carriage return and line feed
			case '\n':
				y++;
				x = 0;
				sendChanges();
				break;
			case '\r':
				x = 0;
				sendChanges();
				break;

			// on tab, add spaces until x reaches a multiple of 4
			case '\t':
				do {
					consumeRaw(' ');
				} while (x % 4 != 0);
				break;

			default:
				consumeRaw(c);
		}

		// fix coordinates
		if (y < 0) y = 0;
		if (x < 0) x = 0;
	}

	private void consumeRaw(char c) {
		// and append char at the current line and cursor position
		LineBuilder line = getCurrentLine();
		if (line.setChar(x++, c)) notifyChange(line);
	}

	private void consumeEscape() {
		Matcher matcher = ESCAPE_PATTERN.matcher(escape);
		if (matcher.find()) {
			// parse valid escape sequence
			int val1 = Strings.isBlank(matcher.group(1)) ? 0 : Integer.parseInt(matcher.group(1));
			int val2 = Strings.isBlank(matcher.group(2)) ? 0 : Integer.parseInt(matcher.group(2));
			char control = matcher.group(3).charAt(0);

			// and execute command if known (otherwise skip command as it is usually coloring)
			switch (control) {
				case 'A': // UP
					y -= val1;
					sendChanges();
					break;
				case 'B': // DOWN
					y += val1;
					sendChanges();
					break;
				case 'C': // RIGHT
					x += val1;
					break;
				case 'D': // LEFT
					x -= val1;
					break;

				// clear line
				case 'K':
					LineBuilder line = getCurrentLine();
					boolean modified = false;
					if (val1 == 0) {
						modified = line.clear(x, Integer.MAX_VALUE);
					}
					else if (val1 == 1) {
						modified = line.clear(0, x);
					}
					else if (val1 == 2) {
						modified = line.clear();
					}
					if (modified) notifyChange(line);
					break;

				// clear lines
				case 'J':
					int yn = Math.min(y, lines.size());
					int start, end;
					if (val1 == 0) {
						clearLines(y, Integer.MAX_VALUE);
					}
					else if (val1 == 1) {
						clearLines(0, y);
					}
					else if (val1 == 2) {
						clearLines(0, Integer.MAX_VALUE);
					}
					break;

				// cursor positioning
				case 'H':
				case 'f':
					y = val1;
					x = val2;
					break;

				// otherwise ignore the escape sequences
			}
		}
		else {
			// append invalid escape sequence at the current line and cursor position
			for (int i = 0; i < escape.length(); i++) consumeRaw(escape.charAt(i));
		}

		// fix coordinates and remove consumed escape sequence
		if (y < 0) y = 0;
		if (x < 0) x = 0;
		escape.setLength(0);
	}

	private LineBuilder getCurrentLine() {
		// ensure line count
		while (y >= lines.size()) lines.add(new LineBuilder(lines.size()));
		return lines.get(y);
	}

	private void clearLines(int start, int end) {
		// check valid input values
		int count = lines.size();
		if (start < 0) start = 0;
		if (end > count) end = count;

		// clear the affected lines
		for (int i = end - 1; i >= start; i--) {
			LineBuilder line = lines.get(i);
			if (line.clear()) notifyChange(line);
		}
		sendChanges();
	}

	@Override
	public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			interpret(cbuf[off + i]);
		}
	}

	@Override
	public void flush() {
		sendChanges();
	}

	@Override
	public void close() {
		sendChanges();
	}

	private static class LineBuilder {

		private final int lineNo;
		private final StringBuilder line = new StringBuilder();

		public LineBuilder(int lineNo) {
			this.lineNo = lineNo;
		}

		/**
		 * Set the specified char at the specified column, filling required columns with whitespaces.
		 *
		 * @return if the line has been changed
		 */
		public boolean setChar(int column, char c) {
			// and set/append the char
			int len = line.length();
			if (column < len) {
				char old = line.charAt(column);
				line.setCharAt(column, c);
				return c != old;
			}
			else {
				// fill line for the required column, so that column == line.length(), adn we append the char afterwards
				if (column > len) {
					line.ensureCapacity(column + 1);
					//noinspection StringRepeatCanBeUsed
					for (int i = column - len; i > 0; i--) line.append(' ');
				}
				// and append to the line
				line.append(c);
				return true;
			}
		}

		/**
		 * Clears the while line.
		 *
		 * @return if the line has been changed
		 */
		public boolean clear() {
			if (line.length() == 0) return false;
			line.setLength(0);
			return true;
		}

		/**
		 * Clears the specified column range, from 'start' (inclusively) to 'end' (exclusively).
		 *
		 * @return if the line has been changed
		 */
		public boolean clear(int start, int end) {
			// check valid input values
			if (start < 0) start = 0;

			// if the range if outside the existing characters, do nothing
			int length = line.length();
			if (start >= length) return false;
			if (start >= end) return false;

			// if the complete tail is cleared, simply reduce the line's length
			if (end >= length) {
				line.setLength(start);
				return true;
			}

			// overwrite range with spaces
			boolean modified = false;
			for (int column = start; column < end; column++) {
				modified |= setChar(column, ' ');
			}
			return modified;
		}
	}

	@FunctionalInterface
	public interface LineConsumer {
		void accept(int lineNo, CharSequence line);
	}
}
