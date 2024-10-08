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
package com.denkbares.strings;

public class StringFragment {

	private final String content;
	private final int offset;
	private final String fatherString;

	public StringFragment(String content, int offset, String fatherString) {
		super();
		this.content = content;
		this.offset = offset;
		this.fatherString = fatherString;
	}

	public String getContent() {
		return content;
	}

	public int getStart() {
		return offset;
	}

	public int getEnd() {
		return offset + content.length();
	}

	public int length() {
		return content.length();
	}

	public String getContentTrimmed() {
		return Strings.trim(content);
	}

	public int getStartTrimmed() {
		return offset + (content.indexOf(Strings.trim(content)));
	}

	public int getEndTrimmed() {
		return getStartTrimmed() + lengthTrimmed();
	}

	public int lengthTrimmed() {
		return Strings.trim(content).length();
	}

	public String getFatherString() {
		return fatherString;
	}

	@Override
	public String toString() {
		return getContent();
	}

}
