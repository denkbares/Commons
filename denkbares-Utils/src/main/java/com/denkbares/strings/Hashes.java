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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Util class for creating various hashes from String or byte[] source data.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.11.2018
 */
public class Hashes {

	/**
	 * Creates a MD5 hash from the specified source data. Note that md5 ist not save for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value
	 */
	public static byte[] md5(byte[] source) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			return digest.digest(source);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 missing", e);
		}
	}

	/**
	 * Creates a MD5 hash from the specified source data. Note that md5 ist not save for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value
	 */
	public static byte[] md5(String source) {
		return md5(source.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Creates a MD5 hash from the specified source data, and returns the result as a formatted (block-wise) hex-string.
	 * Note that md5 ist not save for cryptography.
	 *
	 * @param source the source data to create the hash from
	 * @return the md5 hash value as a block-wise string
	 */
	public static String md5String(String source) {
		return toHexString(md5(source));
	}

	private static String toHexString(byte[] data) {
		// create blocks of 4-byte values
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0 && i % 4 == 0) result.append('-');
			result.append(String.format("%02X", 0xFF & data[i]));
		}
		return result.toString();
	}
}
