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
package com.denkbares.collections;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class to provide useful methods for implementing and/or using MultiMaps.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 09.01.2014
 */
public class MultiMaps {

	/**
	 * Interface to provide factories to create the individual collection instances used by the MultiMap implementations
	 * to collect the keys and/or values.
	 *
	 * @param <T> the elements to have the collection factory for
	 * @author Volker Belli (denkbares GmbH)
	 * @created 09.01.2014
	 */
	public interface CollectionFactory<T> {

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

		private final Comparator<T> comparator;

		public TreeFactory() {
			this(null);
		}

		public TreeFactory(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public Set<T> createSet() {
			return new TreeSet<>(comparator);
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new TreeMap<>(comparator);
		}
	}

	private static final class LinkedHashFactory<T> implements CollectionFactory<T> {

		private final int capacity;

		public LinkedHashFactory(int capacity) {
			this.capacity = capacity;
		}

		@Override
		public Set<T> createSet() {
			return new LinkedHashSet<>(capacity);
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new LinkedHashMap<>(capacity);
		}
	}

	private static final class HashFactory<T> implements CollectionFactory<T> {

		private final int capacity;

		public HashFactory(int capacity) {
			this.capacity = capacity;
		}

		@Override
		public Set<T> createSet() {
			return new HashSet<>(capacity);
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new HashMap<>();
		}
	}

	private static final class MinimizedHashFactory<T> implements CollectionFactory<T> {

		@Override
		public Set<T> createSet() {
			return new MinimizedHashSet<>();
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new HashMap<>();
		}
	}

	private static final class IdentityFactory<T> implements CollectionFactory<T> {

		@Override
		public Set<T> createSet() {
			return Collections.newSetFromMap(new IdentityHashMap<>());
		}

		@Override
		public <E> Map<T, E> createMap() {
			return new IdentityHashMap<>();
		}
	}

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory HASH = new HashFactory(16);

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory HASH_MINIMIZED = new MinimizedHashFactory();

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory LINKED = new LinkedHashFactory(16);

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory LINKED_MINIMIZED = new LinkedHashFactory(4);

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory TREE = new TreeFactory();

	@SuppressWarnings("rawtypes")
	private static final CollectionFactory IDENTITY = new IdentityFactory();

	/**
	 * Returns a collection factory for hashing the entries, using {@link T#hashCode()} and {@link T#equals(Object)}
	 * method.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> hashFactory() {
		return (CollectionFactory<T>) HASH;
	}

	/**
	 * Returns a collection factory for hashing the entries, using {@link T#hashCode()} and {@link T#equals(Object)}
	 * method. The initial hash tables to be used are kept as minimized as possible.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> minimizedFactory() {
		return (CollectionFactory<T>) HASH_MINIMIZED;
	}

	/**
	 * Returns a collection factory for handling the entries as a tree, using {@link T#compareTo(Object)) method.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<? super T>> CollectionFactory<T> treeFactory() {
		return (CollectionFactory<T>) TREE;
	}

	/**
	 * Returns a collection factory for handling the entries with identity hashing. This means only the same object
	 * instances are treated to be equal. No {@link Object#equals(Object)} or {@link Object#hashCode()} of the contained
	 * objects will be used.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> identityFactory() {
		return (CollectionFactory<T>) IDENTITY;
	}

	/**
	 * Returns a collection factory for handling the entries as a tree, using {@link Comparator} to sort.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> treeFactory(Comparator<T> comparator) {
		return new TreeFactory<>(comparator);
	}

	/**
	 * Returns a collection factory for hashing the entries in linked sets/maps, using {@link T#hashCode()} and {@link
	 * T#equals(Object)} method. The order of the contained objects will remain stable.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> linkedFactory() {
		return (CollectionFactory<T>) LINKED;
	}

	/**
	 * Returns a collection factory for hashing the entries in linked sets/maps of minimized size, using {@link
	 * T#hashCode()} and {@link T#equals(Object)} method. The order of the contained objects will remain stable.
	 *
	 * @return the collection factory
	 * @created 09.01.2014
	 */
	@SuppressWarnings("unchecked")
	public static <T> CollectionFactory<T> linkedMinimizedFactory() {
		return (CollectionFactory<T>) LINKED_MINIMIZED;
	}

	public static <K, V> MultiMap<K, V> synchronizedMultiMap(MultiMap<K, V> map) {
		return new SynchronizedMultiMap<>(map);
	}

	private static class SynchronizedMultiMap<K, V> extends AbstractMultiMap<K, V> {

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

		@NotNull
		@Override
		public Set<V> getValues(Object key) {
			synchronized (mutex) {
				return map.getValues(key);
			}
		}

		@NotNull
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

		@NotNull
		@Override
		public Set<V> removeKey(Object key) {
			synchronized (mutex) {
				return map.removeKey(key);
			}
		}

		@NotNull
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

		@NotNull
		@Override
		public Set<K> keySet() {
			synchronized (mutex) {
				return map.keySet();
			}
		}

		@NotNull
		@Override
		public Set<V> valueSet() {
			synchronized (mutex) {
				return map.valueSet();
			}
		}

		@NotNull
		@Override
		public Set<Entry<K, V>> entrySet() {
			synchronized (mutex) {
				return map.entrySet();
			}
		}

		@NotNull
		@Override
		public Map<K, Set<V>> toMap() {
			synchronized (mutex) {
				return map.toMap();
			}
		}

		@NotNull
		@Override
		public Map<K, V> toAnyMap() {
			synchronized (mutex) {
				return map.toAnyMap();
			}
		}
	}

	public static <K, V> MultiMap<K, V> unmodifiableMultiMap(MultiMap<K, V> map) {
		return new UnmodifiableMultiMap<>(map);
	}

	private static final class UnmodifiableMultiMap<K, V> extends AbstractMultiMap<K, V> {

		private final MultiMap<K, V> map;

		private UnmodifiableMultiMap(MultiMap<K, V> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size();
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

		@NotNull
		@Override
		public Set<V> getValues(Object key) {
			return Collections.unmodifiableSet(map.getValues(key));
		}

		@NotNull
		@Override
		public Set<K> getKeys(Object value) {
			return Collections.unmodifiableSet(map.getKeys(value));
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

		@NotNull
		@Override
		public Set<K> keySet() {
			return Collections.unmodifiableSet(map.keySet());
		}

		@NotNull
		@Override
		public Set<V> valueSet() {
			return Collections.unmodifiableSet(map.valueSet());
		}

		@NotNull
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

		@NotNull
		@Override
		public Map<K, Set<V>> toMap() {
			return Collections.unmodifiableMap(map.toMap());
		}

		@NotNull
		@Override
		public Map<K, V> toAnyMap() {
			return Collections.unmodifiableMap(map.toAnyMap());
		}
	}

	/**
	 * Returns a multi map that has the keys and values switched/exchanged. Exactly for each key-&gt;value relation of
	 * the original map there is a value-&gt;key relation in the returned one.
	 * <p>
	 * All changes made to the returned map are mapped into the specified map, and vice versa.
	 *
	 * @param map    the map to get a reversed one
	 * @param <K>    the key type of the map to be reversed, becoming the values type of the new map
	 * @param <V>the value type of the map to be reversed, becoming the key type of the new map
	 * @return the reversed map where keys and values are exchanged
	 */
	public static <K, V> MultiMap<V, K> reversed(MultiMap<K, V> map) {
		if (map instanceof ReversedMultiMap) {
			return ((ReversedMultiMap<K, V>) map).map;
		}
		return new ReversedMultiMap<>(map);
	}

	private static final class ReversedMultiMap<K, V> extends AbstractMultiMap<K, V> {

		private final MultiMap<V, K> map;

		private ReversedMultiMap(MultiMap<V, K> map) {
			this.map = map;
		}

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean containsKey(Object key) {
			return map.containsValue(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return map.containsKey(value);
		}

		@Override
		public boolean contains(Object key, Object value) {
			return map.contains(value, key);
		}

		@NotNull
		@Override
		public Set<V> getValues(Object key) {
			return map.getKeys(key);
		}

		@NotNull
		@Override
		public Set<K> getKeys(Object value) {
			return map.getValues(value);
		}

		@Override
		public boolean put(K key, V value) {
			return map.put(value, key);
		}

		@NotNull
		@Override
		public Set<V> removeKey(Object key) {
			return map.removeValue(key);
		}

		@NotNull
		@Override
		public Set<K> removeValue(Object value) {
			return map.removeKey(value);
		}

		@Override
		public boolean remove(Object key, Object value) {
			return map.remove(value, key);
		}

		@Override
		public void clear() {
			map.clear();
		}

		@NotNull
		@Override
		public Set<K> keySet() {
			return map.valueSet();
		}

		@NotNull
		@Override
		public Set<V> valueSet() {
			return map.keySet();
		}
	}

	public static final MultiMap EMPTY_MULTI_MAP = new EmptyMultiMap();

	public static <K, V> MultiMap<K, V> emptyMultiMap() {
		//noinspection unchecked
		return (MultiMap<K, V>) EMPTY_MULTI_MAP;
	}

	private static class EmptyMultiMap<K, V> extends AbstractMultiMap<K, V> {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public boolean contains(Object key, Object value) {
			return false;
		}

		@NotNull
		@Override
		public Set<V> getValues(Object key) {
			return Collections.emptySet();
		}

		@NotNull
		@Override
		public Set<K> getKeys(Object value) {
			return Collections.emptySet();
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
		public boolean putAll(Map<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean putAll(MultiMap<? extends K, ? extends V> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
		}

		@NotNull
		@Override
		public Set<K> keySet() {
			return Collections.emptySet();
		}

		@NotNull
		@Override
		public Set<V> valueSet() {
			return Collections.emptySet();
		}

		@NotNull
		@Override
		public Set<Entry<K, V>> entrySet() {
			return Collections.emptySet();
		}

		@NotNull
		@Override
		public Map<K, Set<V>> toMap() {
			return Collections.emptyMap();
		}

		@NotNull
		@Override
		public Map<K, V> toAnyMap() {
			return Collections.emptyMap();
		}
	}

	public static <K, V> MultiMap<K, V> singletonMultiMap(K key, V value) {
		return new SingletonMultiMap<>(key, value);
	}

	private static class SingletonMultiMap<K, V> extends AbstractMultiMap<K, V> {

		private final K key;
		private final V value;

		public SingletonMultiMap(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public boolean containsKey(Object key) {
			return Objects.equals(key, this.key);
		}

		@Override
		public boolean containsValue(Object value) {
			return Objects.equals(value, this.value);
		}

		@Override
		public boolean contains(Object key, Object value) {
			return containsKey(key) && containsValue(value);
		}

		@NotNull
		@Override
		public Set<V> getValues(Object key) {
			return containsKey(key) ? valueSet() : Collections.emptySet();
		}

		@NotNull
		@Override
		public Set<K> getKeys(Object value) {
			return containsValue(value) ? keySet() : Collections.emptySet();
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

		@NotNull
		@Override
		public Set<K> keySet() {
			return Collections.singleton(key);
		}

		@NotNull
		@Override
		public Set<V> valueSet() {
			return Collections.singleton(value);
		}

		@NotNull
		@Override
		public Set<Entry<K, V>> entrySet() {
			return Collections.singleton(new AbstractMap.SimpleImmutableEntry<>(key, value));
		}

		@NotNull
		@Override
		public Map<K, Set<V>> toMap() {
			return Collections.singletonMap(key, valueSet());
		}

		@NotNull
		@Override
		public Map<K, V> toAnyMap() {
			return Collections.singletonMap(key, value);
		}
	}

	/**
	 * Returns a {@code Collector} implementing a "group by" operation on input elements of type {@code V}, grouping
	 * elements according to a classification function, and returning the results in a {@link MultiMap}, where the keys
	 * are extracted by the specified function, and the values are the unmodified values of the stream.
	 * <p>
	 * <p>The classification function maps elements to some key type {@code K}. The collector produces a {@code
	 * MultiMap<K, V>} whose keys are the values resulting from applying the classification function to the input
	 * elements, and whose corresponding values are the input elements which map to the associated key under the
	 * classification function.
	 * <p>
	 * <p>There are no guarantees on the type, mutability, serializability, or thread-safety of the {@code MultiMap}
	 * objects returned.
	 *
	 * @param <V>          the type of the input elements
	 * @param <K>          the type of the keys
	 * @param keyExtractor the classifier function mapping input elements to keys
	 * @return a {@code Collector} implementing the group-by operation
	 * @see java.util.stream.Collectors#groupingBy(Function)
	 */
	public static <K, V> Collector<V, ?, MultiMap<K, V>> toMultiMap(Function<V, K> keyExtractor) {
		return new Collector<V, MultiMap<K, V>, MultiMap<K, V>>() {
			@Override
			public Supplier<MultiMap<K, V>> supplier() {
				return DefaultMultiMap::new;
			}

			@Override
			public BiConsumer<MultiMap<K, V>, V> accumulator() {
				return (mmap, value) -> mmap.put(keyExtractor.apply(value), value);
			}

			@Override
			public BinaryOperator<MultiMap<K, V>> combiner() {
				return (mmap1, mmap2) -> {
					mmap1.putAll(mmap2);
					return mmap1;
				};
			}

			@Override
			public Function<MultiMap<K, V>, MultiMap<K, V>> finisher() {
				return Function.identity();
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Collections.singleton(Collector.Characteristics.IDENTITY_FINISH);
			}
		};
	}

	/**
	 * Returns a {@code Collector} implementing a "group by" operation on input elements of type {@code V}, grouping
	 * elements according to a classification function, and returning the results in a {@link MultiMap}, where the keys
	 * are extracted by the specified keyExtractor function, and the values are the values of the stream mapped through
	 * the specified valueExtractor function.
	 * <p>
	 * <p>The classification function maps elements to some key type {@code K}. The collector produces a {@code
	 * MultiMap<K, V>} whose keys are the values resulting from applying the classification function to the input
	 * elements, and whose corresponding values are the mapped input elements which map to the associated key under the
	 * classification function.
	 * <p>
	 * <p>There are no guarantees on the type, mutability, serializability, or thread-safety of the {@code MultiMap}
	 * objects returned.
	 *
	 * @param <V>          the type of the input elements
	 * @param <K>          the type of the keys
	 * @param keyExtractor the classifier function mapping input elements to keys
	 * @return a {@code Collector} implementing the group-by operation
	 */
	public static <E, K, V> Collector<E, ?, MultiMap<K, V>> toMultiMap(Function<E, K> keyExtractor, Function<E, V> valueExtractor) {
		return toMultiMap(keyExtractor, valueExtractor, MultiMaps.hashFactory(), MultiMaps.hashFactory());
	}

	/**
	 * Returns a {@code Collector} implementing a "group by" operation on input elements of type {@code V}, grouping
	 * elements according to a classification function, and returning the results in a {@link MultiMap}, where the keys
	 * are extracted by the specified keyExtractor function, and the values are the values of the stream mapped through
	 * the specified valueExtractor function.
	 * <p>
	 * <p>The classification function maps elements to some key type {@code K}. The collector produces a {@code
	 * MultiMap<K, V>} whose keys are the values resulting from applying the classification function to the input
	 * elements, and whose corresponding values are the mapped input elements which map to the associated key under the
	 * classification function.
	 * <p>
	 * <p>There are no guarantees on the type, mutability, serializability, or thread-safety of the {@code MultiMap}
	 * objects returned.
	 *
	 * @param <V>          the type of the input elements
	 * @param <K>          the type of the keys
	 * @param keyExtractor the classifier function mapping input elements to keys
	 * @return a {@code Collector} implementing the group-by operation
	 */
	public static <E, K, V> Collector<E, ?, MultiMap<K, V>> toMultiMap(Function<E, K> keyExtractor, Function<E, V> valueExtractor, MultiMaps.CollectionFactory<K> keyFactory, MultiMaps.CollectionFactory<V> valueFactory) {
		return new Collector<E, MultiMap<K, V>, MultiMap<K, V>>() {
			@Override
			public Supplier<MultiMap<K, V>> supplier() {
				return () -> new DefaultMultiMap<>(keyFactory, valueFactory);
			}

			@Override
			public BiConsumer<MultiMap<K, V>, E> accumulator() {
				return (mmap, item) -> mmap.put(keyExtractor.apply(item), valueExtractor.apply(item));
			}

			@Override
			public BinaryOperator<MultiMap<K, V>> combiner() {
				return (mmap1, mmap2) -> {
					mmap1.putAll(mmap2);
					return mmap1;
				};
			}

			@Override
			public Function<MultiMap<K, V>, MultiMap<K, V>> finisher() {
				return Function.identity();
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Collections.singleton(Collector.Characteristics.IDENTITY_FINISH);
			}
		};
	}
}
