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

import java.util.function.Function;

import com.denkbares.strings.Strings;
import com.denkbares.utils.Log;

/**
 * A simple TableLine holding the text/cells of a textual table line.
 * It uses the specified split sign passed to the constructor to separate columns/entries within the text content.
 * If the split sign may occur within the cell entry contents but the cell entries are quoted, the
 * constructor with the boolean flag for unquoting can be used.
 *
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 30.03.15.
 */
public class TableLine {

	private final String text;

	private final String[] cells;

	private TableLine(String text, String[] cells) {
		this.text = text;
		this.cells = cells;
	}

	/**
	 * Returns the cell entries of this line as an array.
	 *
	 * @return the cell entries of this line as an array
	 */
	public String[] getCells() {
		return cells;
	}

	public static TableLine createLine(String text, String[] cells) {
		return new TableLine(text, cells);
	}

	public TableLine(String text, String splitSign) {
		this(text, splitSign, true);
	}

	public TableLine(String text, String splitSign, boolean unquoted) {
		this.text = text;
		if (unquoted) {
			cells = Strings.splitUnquotedToArray(text, splitSign);
		}
		else {
			// TODO: fix treatment of trailing empty cells
			cells = text.split(splitSign);
		}
	}

	/**
	 * Returns the cell entry value of this line for the given field.
	 *
	 * @param field the queried field
	 * @return the cell entry value of this line for the given field.
	 */
	public String getValue(Field field) {
		if (field.getVal() >= cells.length) return "";
		String content = cells[field.getVal()];
		if (content == null) return null;
		return Strings.unquote(Strings.unquote(content.trim()), '\'').trim();
	}

	/**
	 * Returns the cell entry value of this line for the given field as an integer.
	 *
	 * @param field the queried field
	 * @return the cell entry value of this line for the given field as an integer.
	 */
	public int getValueInteger(Field field) {
		String numberDeliveredString = getValue(field);
		int result = 0;
		try {
			result = (int) Double.parseDouble(numberDeliveredString);
		}
		catch (NumberFormatException e) {
			Log.warning("Not a valid integer value: "+numberDeliveredString+ " "+e.getMessage());
		}
		return result;
	}

	/**
	 * Returns the cell entry value of this line for the given field as a double value.
	 *
	 * @param field the queried field
	 * @return the cell entry value of this line for the given field as a double value.
	 */
	public double getValueDouble(Field field, Function<String, String> preformatter) {
		String numberDeliveredString = getValue(field);
		double result = 0;
		String cleanDoubleString = preformatter.apply(numberDeliveredString).replace("-", "").trim();
		if (Strings.isBlank(cleanDoubleString)) {
			return Double.NaN;
		}
		try {
			result = Double.parseDouble(cleanDoubleString);
		}
		catch (NumberFormatException e) {
			Log.warning("Could not parse value to Double: " + numberDeliveredString+ " "+e.getMessage());
		}
		return result;
	}
	/**
	 * Returns the cell entry value of this line for the given column number.
	 *
	 * @param index the column number
	 * @return the cell entry value of this line for the given column number.
	 */
	public String getValue(int index) {
		if (index >= cells.length) return "";
		return Strings.unquote(Strings.unquote(cells[index].trim()).trim(), '\'');
	}

	@Override
	public String toString() {
		return Strings.concat("; ", cells);
	}
}
