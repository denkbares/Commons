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

package com.denkbares.semanticcore;

public interface Transformator {

	void transform(String targetFile, SemanticCore core);

	/**
	 * Returns true if the transformator exports exactly one file. If the
	 * transformator exports a couple of files, than this method returns false.
	 * <p>
	 * This information is necessary to determine whether a File- or
	 * Directory-Chosing dialog is presented in the ReviewTool.
	 *
	 * @return true, if the transformator exports exactly one file.
	 */
	boolean isFileTransformator();

	String getTitle();

	default String getFileExtension() {
		return "";
	}

}
