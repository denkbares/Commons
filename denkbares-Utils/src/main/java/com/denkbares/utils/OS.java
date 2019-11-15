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

import java.util.regex.Pattern;

public enum OS {
	WINDOWS("^Windows"),
	MAC_OS("^Mac OS"),
	UNIX("^(?:AIX|Digital Unix|Epoc32|FreeBSD|HP UX|IRIX|Linux|NetBSD|OpenBSD|Solaris|SunOS)"),
	OTHER(".*"); // take the rest

	private final Pattern pattern;
	private static final OS currentOS = findOS(getCurrentOriginalName());

	/**
	 * Returns the original operating system name, provided by the operating system itself.
	 */
	public static String getCurrentOriginalName() {
		return System.getProperty("os.name");
	}

	OS(String regex) {
		this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	public static OS findOS(String displayName) {
		OS matchesOS = OTHER;
		for (OS os : OS.values()) {
			if (os.pattern.matcher(displayName).find()) {
				matchesOS = os;
				break;
			}
		}
		return matchesOS;
	}

	public boolean isCurrentOS() {
		return this == getCurrentOS();
	}

	public static OS getCurrentOS() {
		return currentOS;
	}
}
