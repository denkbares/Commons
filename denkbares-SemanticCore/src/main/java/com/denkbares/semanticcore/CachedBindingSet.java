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

package com.denkbares.semanticcore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleBNode;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.util.iterators.ConvertingIterator;
import org.jetbrains.annotations.NotNull;

/**
 * Cached version of a binding set, making sure there are no references to any repository or connection.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 28.10.16
 */
public class CachedBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = 6002089496337116648L;
	private final Map<String, Value> bindings;

	public CachedBindingSet(BindingSet bindingSet) {
		if (bindingSet.size() >= 2) {
			this.bindings = new HashMap<>((int) (bindingSet.size() * 1.5));
			for (Binding binding : bindingSet) {
				bindings.put(binding.getName(), convertValue(binding.getValue()));
			}
		}
		else if (bindingSet.size() == 1) {
			Binding binding = bindingSet.iterator().next();
			bindings = Collections.singletonMap(binding.getName(), convertValue(binding.getValue()));
		}
		else {
			bindings = Collections.emptyMap();
		}
	}

	/**
	 * Here we convert all values to values for which we are sure, that they don't have a reference to the repository or
	 * connection to be closed after caching.
	 */
	private Value convertValue(Value value) {
		if (value instanceof IRI) {
			return new CachedIRI(value.stringValue());
		}
		else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			CachedLiteral cachedLiteral = new CachedLiteral();
			cachedLiteral.setLabel(literal.getLabel());
			cachedLiteral.setDatatype(literal.getDatatype());
			literal.getLanguage().ifPresent(cachedLiteral::setLanguage);
			return cachedLiteral;
		}
		else if (value instanceof BNode) {
			return new CachedBNode(((BNode) value).getID());
		}
		throw new UnsupportedOperationException("Unable to cache value of type " + value.getClass());
	}

	@Override
	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	@Override
	public Binding getBinding(String bindingName) {
		Value value = getValue(bindingName);
		if (value != null) {
			return new SimpleBinding(bindingName, value);
		}

		return null;
	}

	@Override
	public Value getValue(String bindingName) {
		return bindings.get(bindingName);
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	@NotNull
	@Override
	public Iterator<Binding> iterator() {
		Iterator<Map.Entry<String, Value>> entries = bindings.entrySet().iterator();

		return new ConvertingIterator<Map.Entry<String, Value>, Binding>(entries) {

			@Override
			protected Binding convert(Map.Entry<String, Value> entry) {
				return new SimpleBinding(entry.getKey(), entry.getValue());
			}
		};
	}

	@Override
	public int size() {
		return bindings.size();
	}

	private static final class CachedLiteral extends SimpleLiteral {
		private static final long serialVersionUID = 8135962119311429522L;

		@Override
		public void setDatatype(IRI datatype) {
			super.setDatatype(SimpleValueFactory.getInstance().createIRI(datatype.stringValue()));
		}

		@Override
		public void setLabel(String label) {
			super.setLabel(label);
		}

		@Override
		public void setLanguage(String language) {
			if (language != null) {
				super.setLanguage(language);
			}
		}
	}

	private static class CachedBNode extends SimpleBNode {
		private static final long serialVersionUID = -2870840425921911510L;

		public CachedBNode(String id) {
			super(id);
		}
	}

	private static class CachedIRI extends SimpleIRI {
		private static final long serialVersionUID = -2118422531535486044L;

		public CachedIRI(String id) {
			super(id);
		}
	}
}
