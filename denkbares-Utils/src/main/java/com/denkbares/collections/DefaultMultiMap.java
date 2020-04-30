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
package com.denkbares.collections;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * This class provides an implementation for a {@link MultiMap} that only uses a single hash map for the underlying
 * representation. Therefore accessing the values by the keys is efficient (O(1)), but accessing the keys for values or
 * the set of values or removing a value for all keys is O(n) in the worst case:
 * <p/>
 * <ul> <li>{@link #containsValue(Object)} <li>{@link #removeValue(Object)} <li>{@link
 * #getKeys(Object)} <li> {@link #valueSet()} </ul>
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 07.01.2014
 */
// we are suppressing warnings when using "Object"s as key and value.
// They are used when accessing entries, because java.util.Map also does it this way.
// only methods that adds items are forcing to have the correct parameter types.
@SuppressWarnings("SuspiciousMethodCalls")
public class DefaultMultiMap<K, V> extends AbstractMultiMap<K, V> implements Serializable {

	private int size = 0;
	final Map<K, Set<V>> k2v;

	private final MultiMaps.CollectionFactory<K> keyFactory;
	private final MultiMaps.CollectionFactory<V> valueFactory;

	/**
	 * Creates a new N2MMap using HashSets and HashMaps for the contained objects.
	 */
	public DefaultMultiMap() {
		this(MultiMaps.hashFactory(), MultiMaps.hashFactory());
	}

	/**
	 * Creates a new N2MMap using the specified collection factories to manage the keys and values to be added. The
	 * {@link MultiMaps.CollectionFactory} can be accessed by the {@link MultiMaps} class through some utility methods
	 * provided.
	 *
	 * @param keyFactory   the collection factory used to manage the keys
	 * @param valueFactory the collection factory used to manage the values
	 */
	public DefaultMultiMap(MultiMaps.CollectionFactory<K> keyFactory, MultiMaps.CollectionFactory<V> valueFactory) {
		this.keyFactory = keyFactory;
		this.valueFactory = valueFactory;
		this.k2v = keyFactory.createMap();
	}

	/**
	 * Convenience constructor method that creates a multi-map with keys and values are linked hash sets.
	 *
	 * @return a newly created, empty multi map that preserves the order of their elements
	 */
	public static <K, V> DefaultMultiMap<K, V> newLinked() {
		return new DefaultMultiMap<>(MultiMaps.linkedFactory(), MultiMaps.linkedFactory());
	}

	/**
	 * Convenience constructor method that creates a multi-map with keys and values are in concurrent hash sets.
	 *
	 * @return a newly created, empty multi map that is "weakly consistent" in concurrency (cannot throw any concurrent
	 * modifications)
	 */
	public static <K, V> DefaultMultiMap<K, V> newConcurrent() {
		return new DefaultMultiMap<>(MultiMaps.concurrentFactory(), MultiMaps.concurrentFactory());
	}

	@Override
	public boolean put(K key, V value) {
		// connect source to term
		Set<V> values = k2v.computeIfAbsent(key, _k -> valueFactory.createSet());
		boolean isNew = values.add(value);
		if (isNew) size++;
		return isNew;
	}

	@Override
	public void clear() {
		k2v.clear();
		size = 0;
	}

	@NotNull
	@Override
	public Set<K> removeValue(Object value) {
		Set<K> keys = getKeys(value);
		for (K key : keys) {
			// for each key remove it from its support list
			Set<V> values = k2v.get(key);
			values.remove(value);
			// is the support list is empty, also remove the term itself
			if (values.isEmpty()) {
				k2v.remove(key);
			}
		}
		size -= keys.size();
		return Collections.unmodifiableSet(keys);
	}

	@NotNull
	@Override
	public Set<V> removeKey(Object key) {
		Set<V> values = k2v.remove(key);
		if (values == null) return Collections.emptySet();
		size -= values.size();
		return Collections.unmodifiableSet(values);
	}

	@Override
	public boolean remove(Object key, Object value) {
		Set<V> values = k2v.get(key);
		if (values == null) return false;
		boolean isRemoved = values.remove(value);
		if (isRemoved) {
			size--;
			// and check if the list has become empty
			// and can be removed completely
			if (values.isEmpty()) {
				k2v.remove(key);
			}
		}
		return isRemoved;
	}

	@NotNull
	@Override
	public Set<K> getKeys(Object value) {
		Set<K> result = keyFactory.createSet();
		for (Entry<K, Set<V>> entry : k2v.entrySet()) {
			if (entry.getValue().contains(value)) {
				result.add(entry.getKey());
			}
		}
		return Collections.unmodifiableSet(result);
	}

	@NotNull
	@Override
	public Set<V> getValues(Object key) {
		Set<V> values = k2v.get(key);
		return (values == null) ? Collections.emptySet() : Collections.unmodifiableSet(values);
	}

	@Override
	public boolean contains(Object key, Object value) {
		Set<V> values = k2v.get(key);
		return (values != null) && values.contains(value);
	}

	@Override
	public boolean containsKey(Object key) {
		return k2v.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (Set<V> values : k2v.values()) {
			if (values.contains(value)) return true;
		}
		return false;
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(k2v.keySet());
	}

	@NotNull
	@Override
	public Set<V> valueSet() {
		Set<V> result = valueFactory.createSet();
		for (Set<V> values : k2v.values()) {
			result.addAll(values);
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public int size() {
		return size;
	}
}
