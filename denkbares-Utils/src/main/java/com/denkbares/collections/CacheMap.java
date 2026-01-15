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
 * A LinkedHashMap which has a max size. When max size is reached, the eldest element will be removed. When adding a
 * value which already exists in the map, the old value will be deleted and the new one added as the first element.
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

	/**
	 * Inserts the specified key-value pair into the map. If a mapping for the key previously exists, the old value is
	 * replaced. If the map is in accessOrder mode, the key is inserted in a manner maintaining access order. Otherwise,
	 * the key is manually repositioned to the newest entry.
	 */
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
