/*
 * Copyright (C) 2018 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * Implementation of a Map that will remove entries when the value in the map has been cleaned from garbage collection –
 * in contrast to WeakHashMap, which will remove the entries if the key is garbage collected. The values are store as
 * soft references, make the map usable as in-memory cache.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.12.14.
 */
public class SoftValueHashMap<K, V> extends WeakValueHashMap<K, V> {

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the given initial capacity and the given load factor.
	 *
	 * @param initialCapacity The initial capacity of the <code>WeakHashMap</code>
	 * @param loadFactor      The load factor of the <code>WeakHashMap</code>
	 * @throws IllegalArgumentException If the initial capacity is less than zero, or if the load factor is nonpositive
	 */
	public SoftValueHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the given initial capacity and the default load
	 * factor, which is <code>0.75</code>.
	 *
	 * @param initialCapacity The initial capacity of the <code>WeakHashMap</code>
	 * @throws IllegalArgumentException If the initial capacity is less than zero
	 */
	public SoftValueHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs a new, empty <code>WeakValueHashMap</code> with the default initial capacity and the default load
	 * factor, which is <code>0.75</code>.
	 */
	public SoftValueHashMap() {
		super();
	}

	/**
	 * Constructs a new <code>WeakValueHashMap</code> with the same mappings as the specified <tt>Map</tt>.  The
	 * <tt>HashMap</tt> is created with default load factor (0.75) and an initial capacity sufficient to hold the
	 * mappings in the specified <tt>Map</tt>.
	 *
	 * @param map the map whose mappings are to be placed in this map
	 */
	public SoftValueHashMap(Map<? extends K, ? extends V> map) {
		super(map);
	}

	/**
	 * This enclosing map implementation stores only soft references to the values. Thus the values can be garbage
	 * collected. To be able to also remove the entries from the map after the value has been discarded, we use a trick:
	 * We derive our own weak reference that also knows the key this reference is stored for. By garbage collecting the
	 * value, the garbage collector adds this extended weak references to a queue. When processing the queue (see method
	 * processQueue), we can remove these keys from the map.
	 *
	 * @param <K> the key type stored in the enclosing map
	 * @param <V> the value type stored in the enclosing map
	 * @see #processQueue()
	 */
	private static class SoftValueRef<K, V> extends SoftReference<V> implements ValueRef<K, V> {
		public final K key;

		public SoftValueRef(K key, V val, ReferenceQueue<V> q) {
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

	@Override
	protected ValueRef<K, V> wrap(K key, V value) {
		if (value == null) return null;
		return new SoftValueRef<>(key, value, queue);
	}
}