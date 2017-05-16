package com.denkbares.reader.table;

/**
 * A simple TableLine factory creating a TableLine for a given line of text.
 *
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 13.05.16.
 */
public class DefaultTableLineFactory implements TableLineFactory{

	protected String splitter =  "\\t";
	private boolean unquote = false;

	public DefaultTableLineFactory() {
		// use default splitter
	}

	public DefaultTableLineFactory(String splitter) {
		this.splitter = splitter;
	}

	public DefaultTableLineFactory(String splitter, boolean unquote) {
		this.splitter = splitter;
		this.unquote = unquote;
	}

	@Override
	public TableLine createTableLine(String lineText) {
		return new TableLine(lineText, splitter, unquote);
	}
}
