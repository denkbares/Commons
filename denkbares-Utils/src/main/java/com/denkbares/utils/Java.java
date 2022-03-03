/*
 * Copyright (C) 2022 denkbares GmbH, Germany
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java related util methods
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 19.05.2020
 */
public class Java {
	private static final Logger LOGGER = LoggerFactory.getLogger(Java.class);

	private static final int VERSION = initVersion();

	private static int initVersion() {
		String version = getVersionString();
		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		}
		else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}
		try {
			return Integer.parseInt(version); // this works at least til Java version 14
		}
		catch (NumberFormatException e) { // just to be safe, return an integer
			LOGGER.error("Unable to parse Java version from version string " + version);
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Get Java version as an integer. Java 8 returns 8, Java 9 returns 9 and so forth.
	 * Implementation from https://stackoverflow.com/a/2591122/1819415, but a little safer
	 *
	 * @return the currently used java version as an integer
	 */
	public static int getVersion() {
		return VERSION;
	}

	/**
	 * The text representation of the Java version currently running
	 */
	public static String getVersionString() {
		return System.getProperty("java.version");
	}
}
