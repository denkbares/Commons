/*
 * Copyright (C) 2025 denkbares GmbH, Germany
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

package com.denkbares.util.nio;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Class the allows to define a filter predicate on paths, allowing {@link java.io.IOException}s during evaluation.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 20.08.2025
 */
public interface PathFilter {
	/**
	 * Returns true, if the specified path is accepted by the filter.
	 *
	 * @param path the path to be tested
	 * @return true if the file or directory should be accepted
	 */
	boolean accept(Path path) throws IOException;
}
