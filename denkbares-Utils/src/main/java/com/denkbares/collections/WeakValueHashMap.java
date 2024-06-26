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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a Map that will remove entries when the value in the map has been cleaned from garbage collection –
 * in contrast to WeakHashMap, which will remove the entries if the key is garbage collected. You should not use this
 * implementation for caching, only for canonial mapping, because weak references are removed very frequently.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.12.14.
 */
public class WeakValueHashMap<K, V> extends AbstractMap<K, V> {

	/* Hash table mapping keys to it weak values references */
	private final Map<K, ValueRef<K, V>> hash;

	/* Reference queue for cleared weak values references */
	protected final ReferenceQueue<V> queue = new ReferenceQueue<>();

	/**
	 * This method does the trick. It removes all invalidated entries from the map, by removing those entries whose
	 * values have been discarded. We must be a little careful here, because the key's value might be already
	 * overwritten by some other (newer) value. In this case the key must remain in the hash table.
	 */
	private void processQueue() {
		while (true) {
			ValueRef reference = (ValueRef) queue.poll();
			if (reference == null) break;
			// only remove if the stored value in the hash-table is still the queued one
			@SuppressWarnings("unchecked") K key = (K) reference.getKey();
			if (reference == hash.get(key)) {
				hash.remove(key);
			}
		}
	}

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the given initial capacity and the given load factor.
	 *
	 * @param initialCapacity The initial capacity of the <code>WeakHashMap</code>
	 * @param loadFactor      The load factor of the <code>WeakHashMap</code>
	 * @throws IllegalArgumentException If the initial capacity is less than zero, or if the load factor is nonpositive
	 */
	public WeakValueHashMap(int initialCapacity, float loadFactor) {
		hash = new HashMap<>(initialCapacity, loadFactor);
	}

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the given initial capacity and the default load
	 * factor, which is <code>0.75</code>.
	 *
	 * @param initialCapacity The initial capacity of the <code>WeakHashMap</code>
	 * @throws IllegalArgumentException If the initial capacity is less than zero
	 */
	public WeakValueHashMap(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the default initial capacity and the default load
	 * factor, which is <code>0.75</code>.
	 */
	public WeakValueHashMap() {
		this(16);
	}

	/**
	 * Constructs a new <code>WeakValueHashMap</code> with the same mappings as the specified <tt>Map</tt>.  The
	 * <tt>HashMap</tt> is created with default load factor (0.75) and an initial capacity sufficient to hold the
	 * mappings in the specified <tt>Map</tt>.
	 *
	 * @param map the map whose mappings are to be placed in this map
	 */
	public WeakValueHashMap(Map<? extends K, ? extends V> map) {
		this(Math.max(2 * map.size(), 11), 0.75f);
		putAll(map);
	}

	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		processQueue();
		final Set<Entry<K, ValueRef<K, V>>> entries = hash.entrySet();
		return new AbstractSet<Entry<K, V>>() {
			@NotNull
			@Override
			public Iterator<Entry<K, V>> iterator() {
				final Iterator<Entry<K, ValueRef<K, V>>> iterator = entries.iterator();
				return new Iterator<Entry<K, V>>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public Entry<K, V> next() {
						final Entry<K, ValueRef<K, V>> next = iterator.next();
						return new Entry<K, V>() {
							@Override
							public K getKey() {
								return next.getKey();
							}

							@Override
							public V getValue() {
								return unwrap(next.getValue());
							}

							@Override
							public V setValue(V value) {
								return unwrap(next.setValue(wrap(next.getKey(), value)));
							}
						};
					}

					@Override
					public void remove() {
						iterator.remove();
					}
				};
			}

			@Override
			public int size() {
				return entries.size();
			}
		};
	}

	/**
	 * Returns the number of key-value mappings in this map. <strong>Note:</strong> <em>In contrast with most
	 * implementations of the <code>Map</code> interface, the time required by this operation is linear in the size of
	 * the map.</em>
	 */
	@Override
	public int size() {
		processQueue();
		return hash.size();
	}

	/**
	 * Returns <code>true</code> if this map contains no key-value mappings.
	 */
	@Override
	public boolean isEmpty() {
		processQueue();
		return hash.isEmpty();
	}

	/**
	 * Returns <code>true</code> if this map contains a mapping for the specified key.
	 *
	 * @param key The key whose presence in this map is to be tested
	 */
	@Override
	public boolean containsKey(Object key) {
		processQueue();
		return hash.containsKey(key);
	}

	/**
	 * Returns the value to which this map maps the specified <code>key</code>. If this map does not contain a value for
	 * this key, then return <code>null</code>.
	 *
	 * @param key The key whose associated value, if any, is to be returned
	 */
	@Override
	public V get(Object key) {
		processQueue();
		return unwrap(hash.get(key));
	}

	/**
	 * Updates this map so that the given <code>key</code> maps to the given <code>value</code>.  If the map previously
	 * contained a mapping for <code>key</code> then that mapping is replaced and the previous value is returned.
	 *
	 * @param key   The key that is to be mapped to the given <code>value</code>
	 * @param value The value to which the given <code>key</code> is to be mapped
	 * @return The previous value to which this key was mapped, or <code>null</code> if if there was no mapping for the
	 * key
	 */
	@Override
	public V put(K key, V value) {
		processQueue();
		return unwrap(hash.put(key, wrap(key, value)));
	}

	/**
	 * Removes the mapping for the given <code>key</code> from this map, if present.
	 *
	 * @param key The key whose mapping is to be removed
	 * @return The value to which this key was mapped, or <code>null</code> if there was no mapping for the key
	 */
	@Override
	public V remove(Object key) {
		processQueue();
		return unwrap(hash.remove(key));
	}

	/**
	 * Removes all mappings from this map.
	 */
	@Override
	public void clear() {
		processQueue();
		hash.clear();
	}

	protected interface ValueRef<K, V> {
		K getKey();

		V getValue();
	}

	/**
	 * This enclosing map implementation stores only weak references to the values. Thus the values can be garbage
	 * collected. To be able to also remove the entries from the map after the value has been discarded, we use a trick:
	 * We derive our own weak reference that also knows the key this reference is stored for. By garbage collecting the
	 * value, the garbage collector adds this extended weak references to a queue. When processing the queue (see method
	 * processQueue), we can remove these keys from the map.
	 *
	 * @param <K> the key type stored in the enclosing map
	 * @param <V> the value type stored in the enclosing map
	 * @see #processQueue()
	 */
	private static class WeakValueRef<K, V> extends WeakReference<V> implements ValueRef<K, V> {
		public final K key;

		public WeakValueRef(K key, V val, ReferenceQueue<V> q) {
			super(val, q);
			this.key = key;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return get();
		}
	}

	protected ValueRef<K, V> wrap(K key, V value) {
		if (value == null) return null;
		return new WeakValueRef<>(key, value, queue);
	}

	private V unwrap(ValueRef<K, V> reference) {
		return (reference == null) ? null : reference.getValue();
	}
}
