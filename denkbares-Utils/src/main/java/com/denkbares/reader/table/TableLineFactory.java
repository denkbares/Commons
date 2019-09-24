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

package com.denkbares.reader.table;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 13.05.16.
 */
@FunctionalInterface
public interface TableLineFactory {

	/**
	 * Creates a new TableLine for the given line of text content.
	 * For the separation of the cell entries within the text sequence
	 * a split sign is used as specified for this TableLineFactory.
	 *
	 * @param lineText the text sequence for this table line
	 * @return a TableLine object containing the cells for the given text sequence.
	 */
	TableLine createTableLine(String lineText);
}
