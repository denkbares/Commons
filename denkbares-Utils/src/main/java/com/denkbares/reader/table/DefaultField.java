/*
 * Copyright (C) 2021 denkbares GmbH. All rights reserved.
 */

package com.denkbares.reader.table;

public class DefaultField implements Field {

	private final int column;

	public DefaultField(int column) {
		this.column = column;
	}

	@Override
	public int getVal() {
		return column;
	}
}
