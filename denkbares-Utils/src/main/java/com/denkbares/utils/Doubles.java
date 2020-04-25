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

/**
 * Utility class to deal with floating point numbers and precisions.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 25.04.2020
 */
public class Doubles {

	private static final double DOUBLE_IMPRECISION = 0x0.0000000000001P-0; // 52 bit mantissa -> 13 hex digits
	private static final float FLOAT_IMPRECISION = 0x0.000002P-0f; // 23 bit mantissa -> 5.75 hex digits

	/**
	 * Returns the relative difference of <code>a - b</code>, normalized to the larger absolute value of a and b. The
	 * relative difference is <code>&lt;0</code> if a < b, <code>==0</code>, if a == b, and <code>&gt;0</code>, if a >
	 * b.
	 *
	 * @return normalized difference
	 */
	public static double getRelativeDifference(double a, double b) {
		return (a - b) / Math.max(Math.abs(a), Math.abs(b));
	}

	/**
	 * Returns the relative difference of <code>a - b</code>, normalized to the larger absolute value of a and b. The
	 * relative difference is <code>&lt;0</code> if a < b, <code>==0</code>, if a == b, and <code>&gt;0</code>, if a >
	 * b.
	 *
	 * @return normalized difference
	 */
	public static float getRelativeDifference(float a, float b) {
		return (a - b) / Math.max(Math.abs(a), Math.abs(b));
	}

	/**
	 * Returns the relative distance between <code>a</code> and <code>b</code>, normalized to the larger absolute value
	 * of a and b. This is the absolute value of the relative difference, so it is always <code>&ge;0</code>.
	 *
	 * @return normalized delta
	 */
	public static double getRelativeDelta(double a, double b) {
		return Math.abs(getRelativeDifference(a, b));
	}

	/**
	 * Returns the relative distance between <code>a</code> and <code>b</code>, normalized to the larger absolute value
	 * of a and b. This is the absolute value of the relative difference, so it is always <code>&ge;0</code>.
	 *
	 * @return normalized delta
	 */
	public static float getRelativeDelta(float a, float b) {
		return Math.abs(getRelativeDifference(a, b));
	}

	/**
	 * Returns true, if the value is potentially equal, after a assumed number of atomic operations which each
	 * potentially creates a minimal imprecision failure. The number of maxAtomicFailures is usually the number of
	 * operations performed on a and b that may create any imprecision.
	 *
	 * @param a                 left operand to compare
	 * @param b                 right operand to compare
	 * @param maxAtomicFailures number of atomic imprecision failures, a and b may differ from
	 * @return if the two values are equal within the precision of the maxAtomicFailures
	 */
	public static boolean isEqual(double a, double b, int maxAtomicFailures) {
		//noinspection FloatingPointEquality
		return a == b || getRelativeDelta(a, b) < DOUBLE_IMPRECISION * (maxAtomicFailures + 1);
	}

	/**
	 * Returns true, if the value is potentially equal, after a assumed number of atomic operations which each
	 * potentially creates a minimal imprecision failure. The number of maxAtomicFailures is usually the number of
	 * operations performed on a and b that may create any imprecision.
	 *
	 * @param a                 left operand to compare
	 * @param b                 right operand to compare
	 * @param maxAtomicFailures number of atomic imprecision failures, a and b may differ from
	 * @return if the two values are equal within the precision of the maxAtomicFailures
	 */
	public static boolean isEqual(float a, float b, int maxAtomicFailures) {
		//noinspection FloatingPointEquality
		return a == b || getRelativeDelta(a, b) < FLOAT_IMPRECISION * (maxAtomicFailures + 1);
	}
}
