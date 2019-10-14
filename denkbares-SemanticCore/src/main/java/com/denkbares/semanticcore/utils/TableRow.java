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
package com.denkbares.semanticcore.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.Value;
import org.jetbrains.annotations.NotNull;

public final class TableRow {
	private final Map<String, Value> values;
	private final List<String> variables;

	public TableRow(@NotNull Map<String, Value> values, @NotNull List<String> variables) {
		this.values = values;
		this.variables = variables;
	}

	public void addValue(String variable, Value n) {
		this.values.put(variable, n);
	}

	public Value getValue(String Variable) {
		return this.values.get(Variable);
	}

	@Override
	public String toString() {
		return this.values.toString();
	}

	public boolean deepEquals(TableRow other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}

		if (getVariables().size() != other.getVariables().size()) {
			return false;
		}

		for (Map.Entry<String, Value> valueEntry : this.values.entrySet()) {
			final String variable = valueEntry.getKey();
			final Value valueOther = other.getValue(variable);
			final Value value = valueEntry.getValue();

			if (!Objects.equals(value, valueOther)) {
				return false;
			}
		}

		return true;
	}

	public List<String> getVariables() {
		return this.variables;
	}

	public boolean isEmpty() {
		return this.values.isEmpty();
	}
}
