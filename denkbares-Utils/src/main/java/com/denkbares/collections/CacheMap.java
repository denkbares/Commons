/*
 * Copyright (C) 2026 denkbares GmbH, Germany
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A size-limited {@link LinkedHashMap} that automatically evicts the eldest entry when the maximum size is reached.
 * Entries are ordered from "eldest" (head of the list, first to be removed) to "newest" (tail of the list).
 *
 * <p>The map supports two different caching strategies determined by the {@code accessOrder} flag:</p>
 *
 * <ul>
 *     <li><b>Full Least Recently Used (LRU) Mode (accessOrder = true):</b>
 *     Any interaction with an entry - both reading ({@code get}) and writing ({@code put}) - moves that entry to the
 *     end of the list, marking it as the "newest". Entries that are neither read nor updated will gradually move toward
 *     the head and eventually be evicted.
 *     <br><i>Example:</i> A database query cache. Data that is frequently requested remains in memory, even if it is
 *     never updated.</li>
 *     <li><b>Update-based LRU Mode (accessOrder = false):</b>
 *     Only adding or updating an entry ({@code put}) moves it to the end of the list. Pure read access ({@code get})
 *     does not change the order.
 *     <br><i>Example:</i> A configuration cache where entries are only considered "fresh" if they have actually been
 *     modified. An entry that is read often but was last changed a long time ago will eventually be evicted to make
 *     room for truly newer data.</li>
 * </ul>
 *
 * @author Friedrich Fell (Service Mate GmbH)
 * @created 21.01.22
 */
public class CacheMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1L;

	private final int maxEntries;
	private final boolean accessOrder;

	public CacheMap(int cacheSize) {
		this(cacheSize, true);
	}

	public CacheMap(int cacheSize, boolean accessOrder) {
		super(cacheSize, .75f, accessOrder);
		this.maxEntries = cacheSize;
		this.accessOrder = accessOrder;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxEntries;
	}

	@Override
	public V put(K key, V value) {
		if (accessOrder) {
			return super.put(key, value);
		}
		V oldValue = remove(key);
		super.put(key, value);
		return oldValue;
	}
}
