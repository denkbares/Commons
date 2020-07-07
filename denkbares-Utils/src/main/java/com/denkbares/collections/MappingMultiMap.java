/*
 * Copyright (C) 2020 denkbares GmbH, Germany
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

/**
 * MultiMap implementation that uses a key-set and a mapping function that maps each key to a set of values. The
 * MultiMap is unmodifiable, as the modifications cannot be mapped to the the mapping function.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 21.03.2020
 */
public class MappingMultiMap<K, V> extends AbstractMultiMap<K, V> {

	private final Set<K> keys;
	private final Function<K, @NotNull Set<V>> mapping;

	/**
	 * Creates a new MultiMap, based on a set of keys and a value provider (as a mapping function), that maps each key
	 * to a set of values. The content of the map dynamically changes, as the underlying keys or mapping function
	 * changes.
	 * <p>
	 * The implementation grants that the value provider is only called for elements of the specified key-set.
	 * <p>
	 * Note that he value provider must not (!) return null for any of the keys. Moreover, the value provider must not
	 * (!) return an empty set for any of the keys (otherwise the key should not been present).
	 *
	 * @param keys          the keys of the multi map
	 * @param valueProvider the value provider to provide a set of values for each key
	 */
	public MappingMultiMap(Set<K> keys, Function<K, Set<V>> valueProvider) {
		this.keys = keys;
		this.mapping = valueProvider;
	}

	@Override
	public int size() {
		return keys.stream().map(mapping).mapToInt(Set::size).sum();
	}

	@Override
	public boolean containsKey(Object key) {
		//noinspection SuspiciousMethodCalls
		return keys.contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		//noinspection SuspiciousMethodCalls
		return keys.stream().anyMatch(key -> mapping.apply(key).contains(value));
	}

	@Override
	public boolean contains(Object key, Object value) {
		//noinspection unchecked,SuspiciousMethodCalls
		return keys.contains(key) && mapping.apply((K) key).contains(value);
	}

	@NotNull
	@Override
	public Set<V> getValues(Object key) {
		//noinspection unchecked
		return mapping.apply((K) key);
	}

	@NotNull
	@Override
	public Set<K> getKeys(Object value) {
		//noinspection SuspiciousMethodCalls
		return keys.stream().filter(key -> mapping.apply((K) key).contains(value)).collect(Collectors.toSet());
	}

	@Override
	public boolean put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@NotNull
	@Override
	public Set<V> removeKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@NotNull
	@Override
	public Set<K> removeValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(keys);
	}

	@NotNull
	@Override
	public Set<V> valueSet() {
		Set<V> result = new LinkedHashSet<>();
		keys.stream().map(mapping).forEach(result::addAll);
		return result;
	}
}
