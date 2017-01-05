package com.denkbares.semanticcore.utils;

import java.util.Comparator;

import org.eclipse.rdf4j.model.URI;

public class URIComparator implements Comparator<URI> {

	@Override
	public int compare(URI u1, URI u2) {
		return u1.stringValue().compareTo(u2.stringValue());
	}

}
