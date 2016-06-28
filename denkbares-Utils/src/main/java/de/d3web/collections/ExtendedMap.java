/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
 */

package de.d3web.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Map implementation that adds a single key-value-pair to an existing (decorated) map without
 * modifying or copying the map. If the added key is already in the decorated map, the value of the
 * decorated map seems to be overwritten.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 20.05.2016
 */
public class ExtendedMap<K, V> extends AbstractMap<K, V> {

	final Entry<K, V> entry;
	private final Map<K, V> decorated;

	public ExtendedMap(Map<K, V> decorated, K key, V value) {
		this.entry = new SimpleImmutableEntry<>(key, value);
		this.decorated = decorated;
	}

	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		// if we overwrite a key,
		// decorate the key set by replacing the entry with the same key
		if (decorated.containsKey(entry.getKey())) {
			return new AbstractSet<Entry<K, V>>() {
				@NotNull
				@Override
				public Iterator<Entry<K, V>> iterator() {
					return new MappingIterator<>(decorated.entrySet().iterator(),
							e -> Objects.equals(e.getKey(), entry.getKey()) ? entry : e);
				}

				@Override
				public int size() {
					return ExtendedMap.this.decorated.size();
				}
			};
		}

		// otherwise, if the key is additionally,
		// return a key-set extended by the entry
		return new ExtendedSet<>(decorated.entrySet(), entry);
	}

	@Override
	public boolean containsKey(Object key) {
		return Objects.equals(key, entry.getKey()) ||
				decorated.containsKey(key);
	}

	@Override
	public V get(Object key) {
		return Objects.equals(key, entry.getKey()) ? entry.getValue() : decorated.get(key);
	}
}
