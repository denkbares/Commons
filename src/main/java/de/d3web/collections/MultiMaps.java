/*
 * Copyright (C) 2014 denkbares GmbH
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
package de.d3web.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility class to provide useful methods for implementing and/or using MultiMaps.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 09.01.2014
 */
public class MultiMaps {

	/**
	 * Interface to provide factories to create the individual collection instances used by the
	 * MultiMap implementations to collect the keys and/or values.
	 *
	 * @param <T> the elements to have the collection factory for
	 * @author Volker Belli (denkbares GmbH)
	 * @created 09.01.2014
	 */
	public static interface CollectionFactory<T> {

		/**
		 * Creates a new set used for storing the elements.
		 *
		 * @return a newly created set
		 * @created 09.01.2014
		 */
		Set<T> createSet();

		/**
		 * Creates a new map used for storing objects by keys of this class' elements.
		 *
		 * @return a newly created map
		 * @created 09.01.2014
		 */
		<E> Map<T, E> createMap();
	}

	private static final class TreeFactory<T> implements CollectionFactory<T> {

		@Override
		public Set<T> createSet() {
			return new TreeSet<T>();
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new TreeMap<T, E>();
		}
	}

	private static final class LinkedHashFactory<T> implements CollectionFactory<T> {

		@Override
		public Set<T> createSet() {
			return new LinkedHashSet<T>();
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new LinkedHashMap<T, E>();
		}
	}

	private static final class HashFactory<T> implements CollectionFactory<T> {

		private int capacity;

		public HashFactory(int capacity) {
			this.capacity = capacity;
		}

		@Override
		public Set<T> createSet() {
			return new HashSet<T>(capacity);
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new HashMap<T, E>();
		}
	}

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory HASH = new HashFactory(16);

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory HASH_MINIMIZED = new HashFactory(2);

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory LINKED = new LinkedHashFactory();

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory TREE = new TreeFactory();

	/**
	 * Returns a collection factory for hashing the entries, using {@link T#hashCode()} and {@link
	 * T#equals(Object)} method.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> hashFactory() {
		return (CollectionFactory<T>) HASH;
	}

	/**
	 * Returns a collection factory for hashing the entries, using {@link T#hashCode()} and {@link
	 * T#equals(Object)} method. The initial hash tables to be used are kept as minimized as
	 * possible.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> hashMinimizedFactory() {
		return (CollectionFactory<T>) HASH_MINIMIZED;
	}

	/**
	 * Returns a collection factory for handling the entries as a tree, using {@link
	 * T#compareTo(Object)) method.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<? super T>> CollectionFactory<T> treeFactory() {
		return (CollectionFactory<T>) TREE;
	}

	/**
	 * Returns a collection factory for hashing the entries in linked sets/maps, using {@link
	 * T#hashCode()} and {@link T#equals(Object)} method. The order of the contained objects will
	 * remain stable.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> linkedFactory() {
		return (CollectionFactory<T>) LINKED;
	}

	/**
	 * Returns a string representation of this map. The string representation consists of a list of
	 * key-value mappings in the order returned by the map's <tt>entrySet</tt> view's iterator,
	 * enclosed in braces ( <tt>"{}"</tt>). Adjacent mappings are separated by the characters <tt>",
	 * "</tt> (comma and space). Each key-value mapping is rendered as the key followed by an equals
	 * sign (<tt>"="</tt>) followed by the associated value. Keys and values are converted to
	 * strings as by {@link String#valueOf(Object)}.
	 *
	 * @return a string representation of this map
	 */
	static <K, V> String toString(final MultiMap<K, V> map) {
		Iterator<Entry<K, V>> i = map.entrySet().iterator();
		if (!i.hasNext()) return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (; ; ) {
			Entry<K, V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key == map ? "(this Map)" : key);
			sb.append('=');
			sb.append(value == map ? "(this Map)" : value);
			if (!i.hasNext()) return sb.append('}').toString();
			sb.append(", ");
		}
	}

	static <K, V> Map<K, Set<V>> asMap(final MultiMap<K, V> map) {
		return new AbstractMap<K, Set<V>>() {

			@Override
			public Set<Entry<K, Set<V>>> entrySet() {

				return new AbstractSet<Entry<K, Set<V>>>() {

					@Override
					public Iterator<Entry<K, Set<V>>> iterator() {
						final Iterator<K> keyIter = map.keySet().iterator();
						return new Iterator<Entry<K, Set<V>>>() {

							@Override
							public boolean hasNext() {
								return keyIter.hasNext();
							}

							@Override
							public Entry<K, Set<V>> next() {
								K key = keyIter.next();
								Set<V> values = map.getValues(key);
								return new SimpleImmutableEntry<K, Set<V>>(key, values);
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
					}

					@Override
					public int size() {
						return map.keySet().size();
					}
				};
			}
		};
	}

	public static <K, V> MultiMap<K, V> synchronizedMultiMap(MultiMap<K, V> map) {
		return new SynchronizedMultiMap<K, V>(map);
	}

	private static class SynchronizedMultiMap<K, V> implements MultiMap<K, V> {

		private final MultiMap<K, V> map;     // Backing Map
		final Object mutex;        // Object on which to synchronize

		SynchronizedMultiMap(MultiMap<K, V> m) {
			if (m == null) throw new NullPointerException();
			this.map = m;
			mutex = this;
		}

		SynchronizedMultiMap(MultiMap<K, V> m, Object mutex) {
			this.map = m;
			this.mutex = mutex;
		}

		@Override
		public int size() {
			synchronized (mutex) {
				return map.size();
			}
		}

		@Override
		public boolean isEmpty() {
			synchronized (mutex) {
				return map.isEmpty();
			}
		}

		@Override
		public boolean containsKey(Object key) {
			synchronized (mutex) {
				return map.containsKey(key);
			}
		}

		@Override
		public boolean containsValue(Object value) {
			synchronized (mutex) {
				return map.containsValue(value);
			}
		}

		@Override
		public boolean contains(Object key, Object value) {
			synchronized (mutex) {
				return map.contains(key, value);
			}
		}

		@Override
		public Set<V> getValues(Object key) {
			synchronized (mutex) {
				return map.getValues(key);
			}
		}

		@Override
		public Set<K> getKeys(Object value) {
			synchronized (mutex) {
				return map.getKeys(value);
			}
		}

		@Override
		public boolean put(K key, V value) {
			synchronized (mutex) {
				return map.put(key, value);
			}
		}

		@Override
		public Set<V> removeKey(Object key) {
			synchronized (mutex) {
				return map.removeKey(key);
			}
		}

		@Override
		public Set<K> removeValue(Object value) {
			synchronized (mutex) {
				return map.removeValue(value);
			}
		}

		@Override
		public boolean remove(Object key, Object value) {
			synchronized (mutex) {
				return map.remove(key, value);
			}
		}

		@Override
		public boolean putAll(Map<? extends K, ? extends V> m) {
			synchronized (mutex) {
				return this.map.putAll(m);
			}
		}

		@Override
		public boolean putAll(MultiMap<? extends K, ? extends V> m) {
			synchronized (mutex) {
				return this.map.putAll(m);
			}
		}

		@Override
		public void clear() {
			synchronized (mutex) {
				map.clear();
			}
		}

		@Override
		public Set<K> keySet() {
			synchronized (mutex) {
				return map.keySet();
			}
		}

		@Override
		public Set<V> valueSet() {
			synchronized (mutex) {
				return map.valueSet();
			}
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			synchronized (mutex) {
				return map.entrySet();
			}
		}

		@Override
		public Map<K, Set<V>> toMap() {
			synchronized (mutex) {
				return map.toMap();
			}
		}
	}

	public static <K, V> MultiMap<K, V> unmodifiableMultiMap(MultiMap<K, V> map) {
		return new UnmodifiableMultiMap<K, V>(map);
	}

	private static class UnmodifiableMultiMap<K, V> implements MultiMap<K, V> {

		private final MultiMap<K, V> map;

		private UnmodifiableMultiMap(MultiMap<K, V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return map.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return map.containsValue(value);
		}

		@Override
		public boolean contains(Object key, Object value) {
			return map.contains(key, value);
		}

		@Override
		public Set<V> getValues(Object key) {
			return Collections.unmodifiableSet(map.getValues(key));
		}

		@Override
		public Set<K> getKeys(Object value) {
			return Collections.unmodifiableSet(map.getKeys(value));
		}

		@Override
		public boolean put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<V> removeKey(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<K> removeValue(Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean putAll(Map<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean putAll(MultiMap<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<K> keySet() {
			return Collections.unmodifiableSet(map.keySet());
		}

		@Override
		public Set<V> valueSet() {
			return Collections.unmodifiableSet(map.valueSet());
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return Collections.unmodifiableSet(map.entrySet());
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object o) {
			return map.equals(o);
		}

		@Override
		public int hashCode() {
			return map.hashCode();
		}

		@Override
		public String toString() {
			return map.toString();
		}

		@Override
		public Map<K, Set<V>> toMap() {
			return Collections.unmodifiableMap(map.toMap());
		}
	}

}
