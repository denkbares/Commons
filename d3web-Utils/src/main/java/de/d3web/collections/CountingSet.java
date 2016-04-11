/*
 * Copyright (C) 2013 denkbares GmbH, Germany
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to count occurrences of particular objects. For each object a counter is created.
 * It will be counted how often the particular objects has been added.
 * <p>
 * In contrast to an ordinary set, if an object has been added multiple times, it remains in the set
 * even on removal, until if has been removed as often as it has been added.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 14.02.2013
 */
public class CountingSet<E> implements Set<E> {

	private static class Count {

		int count = 0;

		@Override
		public String toString() {
			return String.valueOf(count);
		}
	}

	private final HashMap<E, Count> counters = new HashMap<E, Count>();

	/**
	 * Constructs a new, empty counting set.
	 */
	public CountingSet() {
	}

	/**
	 * Constructs a new counting set containing the elements in the specified collection. If the
	 * specified collections contains equal elements multiple times, they are already counted by
	 * this set.
	 *
	 * @param c the collection whose elements are to be placed into this set
	 * @throws NullPointerException if the specified collection is null
	 */
	public CountingSet(Collection<? extends E> c) {
		addAll(c);
	}

	@Override
	public int size() {
		return counters.size();
	}

	@Override
	public boolean isEmpty() {
		return counters.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		//noinspection SuspiciousMethodCalls
		return counters.containsKey(o);
	}

	@Override
	public Iterator<E> iterator() {
		return counters.keySet().iterator();
	}

	@Override
	public Object[] toArray() {
		return counters.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		//noinspection SuspiciousToArrayCall
		return counters.keySet().toArray(a);
	}

	@Override
	public boolean add(E object) {
		return inc(object) == 1;
	}

	/**
	 * Removes an object from the set. If the object has been added multiple times, the object still
	 * remains in the set, but its counter will be decreased by 1 instead. The method returns true
	 * if the object has been removed from the set.
	 */
	@Override
	public boolean remove(Object object) {
		return dec(object) == 0;
	}

	/**
	 * Returns the number of times the object has been added or 0 if the object is not in this set.
	 *
	 * @param object the object to access its count
	 * @return how often the object has been added
	 * @created 14.02.2013
	 */
	public int getCount(E object) {
		Count count = counters.get(object);
		return (count != null) ? count.count : 0;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return counters.keySet().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean changed = false;
		for (E object : c) {
			changed |= add(object);
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		Collection<E> copy = new ArrayList<E>(this);
		if (!(c instanceof Set)) c = new HashSet<Object>(c);
		for (Object o : copy) {
			if (!c.contains(o)) {
				changed |= remove(o);
			}
		}
		return changed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object object : c) {
			changed |= remove(object);
		}
		return changed;
	}

	@Override
	public void clear() {
		counters.clear();
	}

	/**
	 * The method is similar to {@link #add(Object)}, which also adds the object to the set. The
	 * only difference is that the method returns the number of occurrences of the specified object
	 * after it has been added (instead of a flag if the set has been changed).
	 *
	 * @param object the object to add / increase the counter for
	 * @return the actual counter of that object
	 * @created 14.02.2013
	 */
	public int inc(E object) {
		return inc(object, 1);
	}

	/**
	 * The method is similar to {@link #inc(Object)}, which also adds the object to the set and
	 * return the number of occurrences of the specified object after it has been added. But this
	 * method adds the object multiple times, specified by the amount parameter.
	 *
	 * @param object the object to add / increase the counter for
	 * @param amount how often the object shall be added
	 * @return the actual counter of that object
	 * @created 14.02.2013
	 */
	public int inc(E object, int amount) {
		Count count = counters.get(object);
		if (count == null) {
			count = new Count();
			counters.put(object, count);
		}
		count.count += amount;
		return count.count;
	}

	/**
	 * The method is similar to {@link #remove(Object)}, which also removes the object form the set
	 * or decrease its counter by 1. The only difference is that the method returns the number of
	 * occurrences of the specified object after it has been removed (instead of a flag if the set
	 * has been changed).
	 * <p>
	 * The method return '0' if the object has been removed from the set by this method call. It
	 * return '-1' if the object has not been present in the set before.
	 *
	 * @param object the object to add / increase the counter for
	 * @return the actual counter of that object
	 * @created 14.02.2013
	 */
	public int dec(Object object) {
		return dec(object, 1);
	}

	/**
	 * The method is similar to {@link #dec(Object)}, which also removes the object form the set or
	 * decrease its counter by 1 and return the number of occurrences of the specified object after
	 * it has been removed (instead of a flag if the set has been changed). But this method removes
	 * the object multiple times, specified by the amount parameter. The object will be removed if
	 * the returned counter is equal or below zero.
	 * <p>
	 * The method returns a number between '0' (inclusively) above '-amount' (exclusively) if the
	 * object has been removed from the set by this method call. It return '-amount' if the object
	 * has not been present in the set before.
	 *
	 * @param object the object to remove / decrease the counter for
	 * @return the actual counter of that object
	 * @created 14.02.2013
	 */
	@SuppressWarnings("SuspiciousMethodCalls")
	public int dec(Object object, int amount) {
		Count count = counters.get(object);
		if (count != null) {
			count.count -= amount;
			if (count.count <= 0) {
				counters.remove(object);
			}
			return count.count;
		}
		return -amount;
	}

	/**
	 * Returns a map that represents the elements of the set and their current count as integer. The
	 * map does only contains those elements with a positive count. The returned map is
	 * unmodifiable.
	 *
	 * @return the map representation of this counting set
	 */
	public Map<E, Integer> toMap() {
		return new MappingMap<>(counters, c -> c.count);
	}

	@Override
	public String toString() {
		return counters.toString();
	}
}
