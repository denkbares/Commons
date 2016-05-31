package com.denkbares.semanticcore.jena;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.jetbrains.annotations.NotNull;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.AbstractBindingSet;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.BindingImpl;

/**
 * Some util methods for handling Jena repositories.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaUtils {

	public static RDFNode valueToNode(Model model, Value value) {
		Objects.requireNonNull(value);
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			String label = literal.getLabel();
			String language = literal.getLanguage();
			URI dataType = literal.getDatatype();
			if (dataType != null) {
				return model.createTypedLiteral(label, dataType.stringValue());
			}
			return model.createLiteral(label, language);
		}
		else if (value instanceof URI) {
			return model.createResource(value.stringValue());
		}
		else if (value instanceof BNode) {
			return model.createResource(new AnonId(((BNode) value).getID()));
		}
		throw new IllegalArgumentException("Unable to create jena RDFNode from sesame Value " + value.stringValue());
	}

	public static org.apache.jena.rdf.model.Statement toStatement(Model model, Resource subj, URI pred, Value obj) {
		return model.createStatement(
				sesame2Jena(model, subj),
				sesame2Jena(model, pred),
				sesame2Jena(model, obj));
	}

	public static RDFNode sesame2Jena(Model model, Value obj) {
		return JenaUtils.valueToNode(model, obj);
	}

	public static Property sesame2Jena(Model model, URI pred) {
		return model.createProperty(pred.stringValue());
	}

	public static org.apache.jena.rdf.model.Resource sesame2Jena(Model model, Resource subj) {
		return model.createResource(subj.stringValue());
	}

	public static Statement jena2Sesame(ValueFactory valueFactory, org.apache.jena.rdf.model.Statement statement) {
		return valueFactory.createStatement(jena2Sesame(valueFactory, statement.getSubject()),
				jena2Sesame(valueFactory, statement.getPredicate()),
				jena2Sesame(valueFactory, statement.getObject()));
	}

	public static Value jena2Sesame(ValueFactory valueFactory, RDFNode object) {
		if (object.isURIResource()) return valueFactory.createURI(object.asResource().getURI());
		if (object.isAnon()) valueFactory.createBNode(object.asNode().getBlankNodeLabel());
		return valueFactory.createLiteral(object.asLiteral().getString(), object.asLiteral().getDatatypeURI());
	}

	public static URI jena2Sesame(ValueFactory valueFactory, org.apache.jena.rdf.model.Property property) {
		return valueFactory.createURI(property.getURI());
	}

	public static Resource jena2Sesame(ValueFactory valueFactory, org.apache.jena.rdf.model.Resource subject) {
		return valueFactory.createURI(subject.getURI());
	}

	public static BindingSet jena2Sesame(JenaRepository repository, QuerySolution solution) {
		return new AbstractBindingSet() {
			Set<String> names = null;

			@Override
			public Iterator<Binding> iterator() {
				Iterator<String> names = solution.varNames();
				return new Iterator<Binding>() {

					@Override
					public boolean hasNext() {
						return names.hasNext();
					}

					@Override
					public Binding next() {
						return getBinding(names.next());
					}
				};
			}

			@Override
			public Set<String> getBindingNames() {
				initNames();
				return names;
			}

			@NotNull
			private void initNames() {
				if (names == null) {
					names = new HashSet<>();
					solution.varNames().forEachRemaining(names::add);
				}
			}

			@Override
			public Binding getBinding(String bindingName) {
				return new BindingImpl(bindingName, getValue(bindingName));
			}

			@Override
			public boolean hasBinding(String bindingName) {
				initNames();
				return names.contains(bindingName);
			}

			@Override
			public Value getValue(String bindingName) {
				return JenaUtils.jena2Sesame(repository.getValueFactory(), solution.get(bindingName));
			}

			@Override
			public int size() {
				return getBindingNames().size();
			}
		};
	}
}
