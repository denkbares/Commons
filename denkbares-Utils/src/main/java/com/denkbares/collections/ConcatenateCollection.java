package com.denkbares.collections;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

/**
 * Concatenates multiple collections into one large logical collection. This will not copy the elements, but give a
 * logical view over  all collections. The ConcatenateCollection itself is immutable, but the collections contained in
 * it are still mutable and the view will always be up to date.
 * This implementation is not thread safe!
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 13.09.16
 */
public class ConcatenateCollection<E> implements Collection<E> {

	private final Collection<E>[] collections;

	@SafeVarargs
	public ConcatenateCollection(Collection<E>... collections) {
		this.collections = collections;
	}

	@Override
	public int size() {
		int size = 0;
		for (Collection<E> collection : collections) {
			size += collection.size();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (Collection<E> collection : collections) {
			if (!collection.isEmpty()) return false;
		}
		return true;
	}

	@Override
	public boolean contains(Object o) {
		for (Collection<E> collection : collections) {
			if (collection.contains(o)) return true;
		}
		return false;
	}

	@NotNull
	@Override
	public Iterator<E> iterator() {
		return new ConcatenateIterable<>(collections).iterator();
	}

	@NotNull
	@Override
	public Object[] toArray() {
		int size = size();
		Object[] result = new Object[size];
		int i = 0;
		for (E e : this) {
			result[i++] = e;
		}
		return result;
	}

	@NotNull
	@Override
	public <T> T[] toArray(@NotNull T[] a) {
		int size = size();
		if (a.length < size) {
			//noinspection unchecked
			a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
		}
		int i = 0;
		Object[] result = a;
		for (E e : this) {
			result[i++] = e;
		}
		return a;
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) return false;
		}
		return true;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
