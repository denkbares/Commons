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

import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public final class Architecture {
	public static final Architecture X86_64 = new Architecture("x86_64");
	public static final Architecture AARCH64 = new Architecture("aarch64");

	private final String name;

	private static String getCurrentArchName() {
		return System.getProperty("os.arch");
	}

	Architecture(String name) {
		this.name = name;
	}

	public @NotNull String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Architecture that = (Architecture) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public static @NotNull Architecture findArch(String archName) {
		return Stream.of(X86_64,AARCH64).filter(arch -> arch.name.equals(archName)).findAny().orElse(new Architecture(archName));
	}

	public static @NotNull Architecture getSystemArch() {
		return  findArch(getCurrentArchName());
	}
}
