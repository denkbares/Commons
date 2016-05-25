package com.denkbares.semanticcore.jena;

import java.util.Objects;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

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
}
