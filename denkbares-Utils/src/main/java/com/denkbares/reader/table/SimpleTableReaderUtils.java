/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.denkbares.utils.Log;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 30.03.15.
 */
public class SimpleTableReaderUtils {



	/**
	 * Returns a List of TableLines containing the table contained in the specified input file.
	 *
	 * @param input input source for the textual table content to be read
	 * @param encoding encoding to be used to read the file
	 * @param factory the TableLineFactory to be used to create TableLines
	 * @param skipFirstLine whether the top line should be skipped from reading, when being header line not content
	 * @param lineSplitter the splitting sign to be used to separate cells within the text sequence
	 * @return a ordered list of TableLines corresponding to the given input
	 */
	public static List<TableLine> getData(File input, String encoding, TableLineFactory factory,  boolean skipFirstLine, String lineSplitter) {
		List<TableLine> data = null;
		final String[] lines;
		try {
			lines = SimpleTableReaderUtils.getLines(input, encoding, lineSplitter);
			data = new ArrayList<>();
			int index = 0;
			for (String line : lines) {
				if(skipFirstLine && index == 0) {
					index++;
					continue;
				}
				data.add(factory.createTableLine(line));
				index++;
			}
		} catch (IOException e) {
			Log.severe("Could not read table file", e);
		}

		return data;
	}

	private static String[] getLines(File txt, String encoding, String lineSplitter) throws IOException {
		if (!txt.canRead()) {
			Log.severe("ERROR: file not found: " + txt.getAbsolutePath());
		}
		String content = new String(Files.readAllBytes(txt.toPath()), encoding);
		return content.split(lineSplitter);
	}

    public static List<TableLine> getData(File txt, String encoding, TableLineFactory factory,  boolean skipFirstLine) {
       return getData(txt, encoding, factory, skipFirstLine,  "\\r?\\n");
    }

	/**
	 * Checks for lines the do not have the expected number of cells.
	 * Usually in a table all lines do have the same number of cells.
	 *
	 * @param data input data to be checked.
	 * @param expectedNumberOfColumns number of cells/columns to be checked against
	 * @return a collections of lines that do not have the expected number of columns/cells
	 */
	public static List<TableLine> getInvalidLines( List<TableLine> data, int expectedNumberOfColumns) {
		List<TableLine> invalidLines = new ArrayList<>();
		for (TableLine tableLine : data) {
			if(! (tableLine.getCells().length == expectedNumberOfColumns)) {
				invalidLines.add(tableLine);
			}
		}
		return invalidLines;
	}

}
