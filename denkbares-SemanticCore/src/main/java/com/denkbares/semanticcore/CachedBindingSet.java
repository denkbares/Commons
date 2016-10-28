package com.denkbares.semanticcore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.AbstractBindingSet;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.util.iterators.ConvertingIterator;

/**
 * Cached version of a binding set, making sure there are no references to any repository or connection.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 28.10.16
 */
public class CachedBindingSet extends AbstractBindingSet {

	private final Map<String, Value> bindings;

	public CachedBindingSet(BindingSet bindingSet) {
		this.bindings = new HashMap<>(bindingSet.size());
		for (Binding binding : bindingSet) {
			bindings.put(binding.getName(), convertValue(binding.getValue()));
		}
	}

	/**
	 * Here we convert all values to values for which we are sure, that they don't have a reference to the repository or
	 * connection to be closed after caching.
	 */
	private Value convertValue(Value value) {
		if (value instanceof URI) {
			if (value instanceof URIImpl) return value;
			else return new URIImpl(value.stringValue());
		}
		else if (value instanceof Literal) {
			if (value instanceof LiteralImpl) {
				return value;
			}
			else {
				Literal literal = (Literal) value;
				CachedLiteralImpl cachedLiteral = new CachedLiteralImpl();
				cachedLiteral.setLabel(literal.getLabel());
				cachedLiteral.setLanguage(literal.getLanguage());
				cachedLiteral.setDatatype(literal.getDatatype());
				return cachedLiteral;
			}
		}
		else if (value instanceof BNode) {
			if (value instanceof BNodeImpl) return value;
			else return new BNodeImpl(((BNode) value).getID());
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
			return new BindingImpl(bindingName, value);
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

	@Override
	public Iterator<Binding> iterator() {
		Iterator<Map.Entry<String, Value>> entries = bindings.entrySet().iterator();

		return new ConvertingIterator<Map.Entry<String, Value>, Binding>(entries) {

			@Override
			protected Binding convert(Map.Entry<String, Value> entry) {
				return new BindingImpl(entry.getKey(), entry.getValue());
			}
		};
	}

	@Override
	public int size() {
		return bindings.size();
	}

	private static final class CachedLiteralImpl extends LiteralImpl {
		@Override
		public void setDatatype(URI datatype) {
			super.setDatatype(datatype);
		}

		@Override
		public void setLabel(String label) {
			super.setLabel(label);
		}

		@Override
		public void setLanguage(String language) {
			super.setLanguage(language);
		}
	}

}
