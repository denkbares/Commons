/*
 * Copyright (C) 2013 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;

public class SimpleTableRow implements TableRow {

	private final Map<String, Value> values = new LinkedHashMap<>();

	public void addValue(String variable, Value n) {
		values.put(variable, n);
	}

	@Override
	public Value getValue(String Variable) {
		return values.get(Variable);
	}

	@Override
	public String toString() {
		return values.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (Value value : values.values()) {
			final int valueHashCode = value != null ? value.hashCode() : 0;
			result = prime * result + valueHashCode;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof TableRow)) return false;
		TableRow other = (TableRow) obj;

		if (getVariables().size() != other.getVariables().size()) return false;

		Set<String> variables = values.keySet();
		for (String variable : variables) {
			final Value valueOther = other.getValue(variable);
			final Value value = values.get(variable);
			if (value == null && valueOther == null) {
				return true;
			}
			if (value == null || valueOther == null) {
				return false;
			}
			if (!valueOther.equals(value)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public List<String> getVariables() {
		List<String> result = new ArrayList<>();
		Set<String> keySet = values.keySet();
		result.addAll(keySet);
		return result;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}
}
