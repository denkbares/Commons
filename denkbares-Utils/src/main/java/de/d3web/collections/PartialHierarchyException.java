package de.d3web.collections;

import java.util.Set;

/**
 * An Exception that can be thrown if a problem (usually a cycle)
 * within the use of the PartialHierarchy occurs.
 *
 *
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 25.05.16.
 */
public class PartialHierarchyException extends Exception {

	private Set<?> path;
	private Object cycleStartObject;

	public PartialHierarchyException(Object cycleStartObject, Set path) {
		super("The following row "+cycleStartObject+" forms a hierarchy cycle during insertion: " + path);
		this.cycleStartObject = cycleStartObject;
		this.path = path;
	}

	/**
	 * Returns the cyclic path (for example for printing an error message)
	 *
	 * @return
	 */
	public Set<?> getPath() {
		return path;
	}

	/**
	 * The object that is the starting point and end point of the cycle.
	 *
	 * @return
	 */
	public Object getCycleStartObject() {
		return cycleStartObject;
	}
}
