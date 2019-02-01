package com.denkbares.semanticcore.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;

import com.denkbares.collections.DefaultMultiMap;
import com.denkbares.collections.MultiMap;

public class ResultTableHierarchy {

	private final ResultTableModel data;
	private final List<TableRow> roots = new LinkedList<>();
	private final MultiMap<TableRow, TableRow> children = new DefaultMultiMap<>();
	private final Comparator<TableRow> comparator;
	public static final String SORT_VALUE = "sortValue";

	public ResultTableHierarchy(ResultTableModel data) {
		this.data = data;
		this.comparator = getComparator(data);
		init();
	}

	public List<TableRow> getRoots() {
		return roots.stream()
				.sorted(comparator)
				.collect(Collectors.toList());
	}

	public List<TableRow> getChildren(TableRow row) {
		return children.getValues(row).stream()
				.sorted(comparator)
				.collect(Collectors.toList());
	}

	private void init() {
		for (TableRow tableRow : data) {
			String parentColumn = data.getVariables().get(1);
			Value parentId = tableRow.getValue(parentColumn);
			Collection<TableRow> parents = data.findRowFor(parentId);
			if (parents.isEmpty()) {
				roots.add(tableRow);
			}
			else {
				for (TableRow parent : parents) {
					children.put(parent, tableRow);
				}
			}
		}
	}

	private static Comparator<TableRow> getComparator(final ResultTableModel result) {
		return (o1, o2) -> {
			// we sort by the column 'sortValue' if existing
			// otherwise we sort by URI
			Value sortValue1 = o1.getValue(SORT_VALUE);
			Value sortValue2 = o2.getValue(SORT_VALUE);
			if (sortValue1 == null) {
				sortValue1 = o1.getValue(result.getVariables().get(0));
			}
			if (sortValue2 == null) {
				sortValue2 = o2.getValue(result.getVariables().get(0));
			}

			// TODO : is there a better way to sort integer literals?
			final String sortString1 = sortValue1.toString();
			final String sortString2 = sortValue2.toString();
			final String xmlInt = "<http://www.w3.org/2001/XMLSchema#integer>";
			final String numRegex = "\"(\\d+)\".*$";
			if (sortString1.endsWith(xmlInt) && sortString2.endsWith(xmlInt)) {
				Pattern p = Pattern.compile(numRegex);
				final Matcher matcher = p.matcher(sortString1);
				matcher.find();
				final String intValueString1 = matcher.group(1);
				final Matcher matcher2 = p.matcher(sortString2);
				matcher2.find();
				final String intValueString2 = matcher2.group(1);
				return Integer.valueOf(intValueString1).compareTo(Integer.valueOf(intValueString2));
			}
			return sortString1.compareTo(sortString2);
		};
	}
}

