package com.denkbares.collections;



/**
 * Defines a generic hierarchy.
 * 
 * @author Jochen Reutelshöfer
 * @created 25.11.2013
 * @param <T>
 */
public interface PartialHierarchy<T> {

	/**
	 * Returns true if node1 is a (transitive!) successor of node2, false
	 * otherwise.
	 * 
	 * @created 25.11.2013
	 */
	boolean isSuccessorOf(T node1, T node2) throws PartialHierarchyException;
}
