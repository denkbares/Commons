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
