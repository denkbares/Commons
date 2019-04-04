/*
 * Copyright (C) 2013 denkbares GmbH
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

package com.denkbares.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;

import com.denkbares.strings.Strings;
import com.denkbares.utils.Pair;

/**
 * Implementation of generalized suffix tree that is capable to take any element types to be stored for any strings
 * (String --> E). It is implemented like an MultiMap and allows to store multiple objects for the same strings. It also
 * allows to receive the whole set of elements added that matches a specified substring.
 * <p/>
 * In contrast to existing generalized suffix trees, our implementation allows to easily remove key-value-pairs from the
 * tree, keeping track if the specified combination has been inserted or not.
 * <p/>
 * The implementation is based of a DefaultMultiMap mapping each key to the values. Additionally for each key there is a
 * tree-map of all suffixes to the keys.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 13.03.2014
 */
public class GeneralizedSuffixTree<E> extends DefaultMultiMap<String, E> {

	/**
	 * Here we store for each suffix all the key strings that have been used to generate the suffix. This map uses a
	 * tree-map to easily allow to access for any suffix all suffixes that are stored, because they are a sub-map of the
	 * tree-map. Using this, searching an infix is looking for the node in the suffix-tree, getting a sub-map of all
	 * suffixes starting with the infix and for them get the values that have been added.
	 * <p/>
	 * We use the tree-set also for the values, so we get a natural ordering of the keys.
	 */
	@SuppressWarnings("RedundantTypeArguments") // Seems to need some help here with type inference
	private final DefaultMultiMap<String, String> suffixTree = new DefaultMultiMap<>(
			MultiMaps.<String>treeFactory(), MultiMaps.<String>treeFactory());

	@Override
	public boolean put(String key, E value) {
		boolean isNewKey = !containsKey(key);
		boolean added = super.put(key, value);
		if (isNewKey) {
			addSuffixes(key);
		}
		return added;
	}

	@NotNull
	@Override
	public Set<E> removeKey(Object key) {
		Set<E> values = super.removeKey(key);
		removeSuffixes(key);
		return values;
	}

	@NotNull
	@Override
	public Set<String> removeValue(Object value) {
		Set<String> keys = super.removeValue(value);
		for (String key : keys) {
			if (!containsKey(key)) {
				removeSuffixes(key);
			}
		}
		return keys;
	}

	@Override
	public boolean remove(Object key, Object value) {
		boolean removed = super.remove(key, value);
		boolean isKeyGone = removed && !containsKey(key);
		if (isKeyGone) {
			removeSuffixes(key);
		}
		return removed;
	}

	@Override
	public void clear() {
		super.clear();
		suffixTree.clear();
	}

	/**
	 * Case-insensitively searches the generalized suffix tree for the values, that are stored with keys, that contains
	 * all of the specified sub-strings of the search phrase.
	 * <p>
	 * If a particular value has been added with multiple different keys, the phrase is matched against the union-set of
	 * all keys together.
	 *
	 * @param phrase the phrase to search for
	 * @return the matched items
	 */
	public Iterable<E> search(String phrase) {
		return search(phrase, true);
	}

	/**
	 * Case-insensitively searches the generalized suffix tree for the values, that are stored with keys, that contains
	 * all of the specified sub-strings of the search phrase.
	 * <p>
	 * If matchJoinedKeys is specified as true, and if a particular value has been added with multiple different keys,
	 * the phrase is matched against the union-set of all keys together. Otherwise there must be one key for each
	 * returned value, that matches all the tokens of the search phrase.
	 *
	 * @param phrase          the phrase to search for
	 * @param matchJoinedKeys if the phrase should be matched to the joined keys of each item together; if not, each key
	 *                        is matched individually, reducing the number of matches
	 * @return the matched items
	 */
	public Iterable<E> search(String phrase, boolean matchJoinedKeys) {
		if (Strings.isBlank(phrase)) return Collections.emptyList();
		phrase = phrase.toLowerCase();
		final String[] infixes = phrase.split("\\s+");
		return matchJoinedKeys ? searchInAnyLabel(infixes) : searchInOneLabel(infixes);
	}

	@NotNull
	private Iterable<E> searchInOneLabel(String[] infixes) {
		// use the first infix to search for the items
		// and all other ones to filter the found matches
		final Iterable<String> keys = findKeys(infixes[0]);
		return () -> new FilterDuplicateIterator<>(new FlattingIterator<>(keys, key -> {
			// filter all keys that are not matching all other infixes
			for (int i = 1; i < infixes.length; i++) {
				if (!Strings.containsIgnoreCase(key, infixes[i])) {
					return Collections.<E>emptyList().iterator();
				}
			}
			// filter duplicate items from the values
			return getValues(key).iterator();
		}));
	}

	@NotNull
	private Iterable<E> searchInAnyLabel(String[] infixes) {
		// if no infix is found, we are done
		if (infixes.length == 0) return Collections.emptyList();

		// for each infix, we get a set of keys to look for objects
		// for each set of keys, we create a union-set, because all these keys are valid (have the infix)
		// finally the particular union
		List<Pair<List<Set<E>>, Integer>> unionsWithEstimatedSize = new ArrayList<>(infixes.length);
		for (String infix : infixes) {
			int estimatedSize = 0;
			List<Set<E>> union = new ArrayList<>();
			for (String key : findKeys(infix)) {
				Set<E> values = getValues(key);
				estimatedSize += values.size();
				union.add(values);
			}
			// if any union is empty, no item can be contained in the conjunction of the unions, so stop immediately
			if (estimatedSize == 0) return Collections.emptyList();
			unionsWithEstimatedSize.add(new Pair<>(union, estimatedSize));
		}

		// we only return elements that are in each (!) union, as they must match each infix in any way
		// to we sort the unions by their (estimated) size, to have the smallest set first
		// and then filter each of the elements by all other unions
		unionsWithEstimatedSize.sort(Comparator.comparing(Pair::getB));
		Iterator<E> baseSet = unionsWithEstimatedSize.get(0).getA().stream().flatMap(Set::stream).distinct().iterator();
		return () -> FilterIterator.filter(baseSet, item -> {
			// accept only those items, that are in all other unions
			// (so fail if it is not contained in any parts of one union)
			for (int i = 1; i < unionsWithEstimatedSize.size(); i++) {
				List<Set<E>> union = unionsWithEstimatedSize.get(i).getA();
				if (union.stream().noneMatch(part -> part.contains(item))) {
					// item is in no part of the union, so not in the union, so fail for teh item
					return false;
				}
			}
			// each union has contained the part
			return true;
		});
	}

	private Iterable<String> findKeys(String infix) {
		final NavigableMap<String, Set<String>> range = getSuffixSubTree(infix);
		if (range == null) return Collections.emptyList();
		final Collection<Set<String>> keysOfSuffixes = range.values();
		return () -> new FlattingIterator<>(keysOfSuffixes);
	}

	private NavigableMap<String, Set<String>> getSuffixSubTree(String infix) {
		// get sub-map of prefixes that start with the infix
		TreeMap<String, Set<String>> tree = (TreeMap<String, Set<String>>) suffixTree.k2v;
		// get the first suffix that starts with infix
		String start = tree.ceilingKey(infix);
		if (start == null) return null;
		// get the last suffix that starts with infix
		// (we accept that the following will only work well if \uffff is not
		// used)
		String end = tree.floorKey(infix + '\uffff');
		if (end == null) return null;
		if (start.compareTo(end) > 0) return null;
		return tree.subMap(start, true, end, true);
	}

	private void addSuffixes(String key) {
		for (String suffix : suffixes(key)) {
			suffixTree.put(suffix, key);
		}
	}

	private void removeSuffixes(Object key) {
		if (key instanceof String) {
			for (String suffix : suffixes((String) key)) {
				suffixTree.remove(suffix, key);
			}
		}
	}

	/**
	 * Creates all suffixes for a given phrase. The phrase is split by whitespaces and each resulting token is indexed
	 * separately.
	 *
	 * @param phrase the phrase to get the suffixes for
	 * @return all suffixes of all contained words
	 */
	private Set<String> suffixes(String phrase) {
		phrase = phrase.toLowerCase();
		Set<String> result = new HashSet<>();
		for (String word : phrase.split("\\s+")) {
			for (int i = 0; i < word.length(); i++) {
				result.add(word.substring(i));
			}
		}
		return result;
	}
}
