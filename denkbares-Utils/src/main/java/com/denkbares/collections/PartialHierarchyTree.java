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
package com.denkbares.collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * This data structure is able to capture a (evolving) subset of nodes of a
 * hierarchy as a tree, still retaining the hierarchical relations. That is, if
 * node A is successor of node B in the overall hierarchy G, then node A is also
 * successor of B in this PartialHierarchyTree P (if both contained).
 * <p>
 * A,B &isin; P and succ(A,B) &isin; G => succ(A,B) &isin; P
 * <p>
 * The collection of nodes in this data structure can be modified easily, i.e.,
 * nodes can be added or removed, while all hierarchical relations are
 * guaranteed to be retained. In particular, one can start with an empty
 * PartialHierarchyTree and step-by-step insert nodes, while the
 * PartialHierarchyTree will maintain and update the tree structure.
 * <p>
 * NOTE: This data structure only works correct for well-formed hierarchies,
 * this is each node has at most one parent and the hierarchy is free of cycles.
 *
 * @author Jochen Reutelshöfer
 * @created 12.04.2013
 */
public class PartialHierarchyTree<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PartialHierarchyTree.class);

	private final Node<T> root;
	private final Comparator<T> comparator;
	private final PartialHierarchy<T> hierarchy;

	public PartialHierarchyTree(PartialHierarchy<T> h) {
		this(h, (T) null, null);
	}

	public PartialHierarchyTree(PartialHierarchy<T> h, Comparator<T> comparator) {
		this(h, (T) null, comparator);
	}

	public PartialHierarchyTree(PartialHierarchy<T> h, T rootData) {
		this(h, rootData, null);
	}

	public PartialHierarchyTree(PartialHierarchy<T> h, T rootData, Comparator<T> comparator) {
		this.hierarchy = h;
		this.comparator = comparator;
		root = new Node<>(rootData, comparator);
	}

	private PartialHierarchyTree(PartialHierarchy<T> h, @NotNull Node<T> root, Comparator<T> comparator) {
		this.hierarchy = h;
		this.comparator = comparator;
		this.root = root;
	}

	/**
	 * Creates a deep copy of this tree
	 * (this is, new node objects are created, but same data objects are used)
	 *
	 * @return tree copy
	 */
	public PartialHierarchyTree<T> createCopy() {
		Node<T> copyRoot = root.copyWithSubTree();
		return new PartialHierarchyTree<>(this.hierarchy, copyRoot, comparator);
	}

	/**
	 * Returns the root node of this tree. WARNING: This root node is a
	 * convenience node containing NO data. It is NOT part of the data inserted
	 * into this tree and should be used as starting point for tree traversion
	 * only.
	 *
	 * @created 25.11.2013
	 */
	public Node<T> getRoot() {
		return root;
	}

	/**
	 * Returns all top level nodes of this tree. If this returns an empty list,
	 * the overall tree is empty.
	 *
	 * @created 26.11.2013
	 */
	public List<Node<T>> getRootLevelNodes() {
		return root.getChildren();
	}

	/**
	 * Returns all top level nodes of this tree, sorted according to the given
	 * comparator. If this returns an empty list, the overall tree is empty.
	 *
	 * @created 26.11.2013
	 */
	public List<Node<T>> getRootLevelNodesSorted(Comparator<T> comp) {
		List<Node<T>> children = root.getChildren();
		List<Node<T>> copy = new ArrayList<>(children);
		copy.sort(new PartialHierarchyNodeComparator<>(comp));
		return Collections.unmodifiableList(copy);
	}

	/**
	 * Removes the element from the tree. It considers correct hierarchical
	 * removement, i.e., tree reflects hierarchical node structure after
	 * removement. The tree is restructured if necessary.
	 *
	 * @return whether t has been found and removed
	 * @created 12.04.2013
	 */
	public synchronized boolean remove(T term) throws PartialHierarchyException {
		return remove(term, root);
	}

	/**
	 * Removes all elements of the given collection from this data structure, see remove(T term).
	 * Returns true if the some value has been found and removed.
	 *
	 * @return true if the PartialHierarchyTree has changed, false otherwise.
	 */
	public synchronized boolean removeAll(Collection<T> terms) throws PartialHierarchyException {
		boolean changed = false;
		for (T term : terms) {
			boolean removed = remove(term);
			if (removed) {
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Returns the node with value t if existing in this tree. Search runs in
	 * logarithmic time using the hierarchy relation.
	 *
	 * @created 25.11.2013
	 */
	public Node<T> find(T t) {
		if (t == null) {
			return null;
		}
		try {
			return findRecursiveNode(t, root);
		}
		catch (PartialHierarchyException e) {
			return null;
		}
	}

	/**
	 * Returns the maximum depth level of this node in the tree. Since the can be multiple parents for each node, it is
	 * possible to have different depth level depending on the path to the root. We return the length of longest path to
	 * the root.
	 */
	public int getMaxDepthLevel(Node<T> node) {
		int max = -1;
		for (Node<T> parent : node.getParents()) {
			int depthLevel = getMaxDepthLevel(parent);
			if (depthLevel > max) max = depthLevel;
		}
		return max + 1;
	}

	private synchronized Node<T> findRecursiveNode(T externalNode, Node<T> treeNode) throws PartialHierarchyException {
		if (externalNode.equals(treeNode.data)) {
			return treeNode;
		}

		// descending tree for search only makes sense for sub-node
		if (treeNode.equals(root) || hierarchy.isSuccessorOf(externalNode, treeNode.data)) {
			List<Node<T>> children = treeNode.getChildren();
			for (Node<T> child : children) {
				// other search recursive
				Node<T> found = findRecursiveNode(externalNode, child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * Returns all nodes contained in this tree.
	 *
	 * @created 12.04.2013
	 */
	public Set<Node<T>> getNodes() {
		Set<Node<T>> result = new HashSet<>();
		collectNodes(root, result);
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Returns all nodes contained in this tree.
	 *
	 * @created 12.04.2013
	 */
	public List<T> getNodesDFSOrder() {
		List<T> result = new ArrayList<>();
		addDFS(root, result);
		return Collections.unmodifiableList(result);
	}

	private void addDFS(Node<T> n, List<T> result) {
		List<Node<T>> children = n.getChildren();
		for (Node<T> child : children) {
			result.add(child.data);
			addDFS(child, result);
		}
	}

	/**
	 * Returns a set containing the data elements of all nodes contained in this
	 * tree.
	 *
	 * @created 12.04.2013
	 */
	public Set<T> getNodeContents() {
		Set<Node<T>> nodes = new HashSet<>();
		collectNodes(root, nodes);
		Set<T> result = new HashSet<>();
		for (Node<T> node : nodes) {
			result.add(node.data);
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * @created 12.04.2013
	 */
	private void collectNodes(Node<T> n, Set<Node<T>> result) {
		result.add(n);
		List<Node<T>> children = n.getChildren();
		for (Node<T> child : children) {
			collectNodes(child, result);
		}
		result.remove(root);
	}

	/**
	 * Collects and returns all leafs of this partial hierarchy.
	 * <p/>
	 * Note that in this partial hierarchy (i.e. forest) data structure a leaf can at the same time also be a root.
	 *
	 * @return all leafs of the hierarchy
	 */
	public Collection<T> getLeafNodes() {
		Collection<T> result = new HashSet<>();
		addLeafNodes(root, result);
		return Collections.unmodifiableCollection(result);
	}

	private void addLeafNodes(Node<T> node, Collection<T> result) {
		final List<Node<T>> children = node.getChildren();
		if (children == null || children.isEmpty()) {
			//is a leaf
			result.add(node.getData());
		}
		else {
			for (Node<T> child : children) {
				addLeafNodes(child, result);
			}
		}
	}

	public int getNodeCount() {
		Set<Node<T>> set = new HashSet<>();
		collectNodes(getRoot(), set);
		return set.size();
	}

	/**
	 * Inserts a new element into the tree. It considers correct hierarchical
	 * insertion, i.e., tree reflects hierarchical node structure after
	 * insertion. The tree is restructured if necessary.
	 * <p>
	 * If the element is already contained in the tree nothing happens.
	 *
	 * @return true if node was inserted, false otherwise (t already in tree)
	 * @throws PartialHierarchyException if a the hierarchy is invalid (forming a cycle) a PartialHierarchyException is
	 *                                   thrown.
	 * @created 12.04.2013
	 */
	public boolean insert(T t) throws PartialHierarchyException {
		if (t == null) {
			return false;
		}
		if (find(t) == null) {
			insertNodeUnder(new Node<>(t, comparator), root);
			return true;
		}
		else {
			// element already in tree
			return false;
		}
	}

	/**
	 * Inserts a new element into the tree. It considers correct hierarchical
	 * insertion, i.e., tree reflects hierarchical node structure after
	 * insertion. The tree is restructured if necessary.
	 * <p>
	 * If a the hierarchy is invalid (forming a cycle) insertion is aborted an 'false' is returned.
	 *
	 * @return true if the value has been inserted into the tree, false otherwise
	 */
	public boolean insertNode(T t) {
		try {
			return insert(t);
		}
		catch (PartialHierarchyException e) {
			LOGGER.error("Unable to insert node", e);
			return false;
		}
	}

	/**
	 * Two cases need to be considered:
	 * <p>
	 * 1) One of the siblings is ancestor of the new concept:
	 * <p>
	 * - -Then we need to descent and insert under that one
	 * <p>
	 * 2) A sibling can be a successor of the new concept:
	 * <p>
	 * - -Then the new concept has to be inserted at this level, but the
	 * siblings need to be searched for successor-concepts. If such concepts are
	 * found, they need to be re-hanged to be child of the new concept.
	 *
	 * @created 12.04.2013
	 */
	private synchronized void insertNodeUnder(Node<T> newNode, Node<T> parent) throws PartialHierarchyException {

		// look for super concepts to descent
		boolean descent = false;
		for (Node<T> child : parent.getChildren()) {
			if (hierarchy.isSuccessorOf(newNode.getData(), child.data)) {
				insertNodeUnder(newNode, child);
				descent = true;
			}
		}
		// if no super concept for descent is found, insert at this level
		if (!descent) {
			parent.addChild(newNode);
			newNode.addParent(parent);

			// then check siblings, which could be sub-concepts of the new one
			List<Node<T>> successorSiblings = new ArrayList<>();
			for (Node<T> sibling : parent.getChildren()) {
				if (sibling.data.equals(newNode.getData())) {
					continue;
				}
				if (hierarchy.isSuccessorOf(sibling.data, newNode.getData())) {
					// re-hang sibling to be successor of t
					successorSiblings.add(sibling);
				}
			}
			for (Node<T> successorSibling : successorSiblings) {
				parent.removeChild(successorSibling);
				newNode.addChild(successorSibling);
				successorSibling.removeParent(parent);
				successorSibling.addParent(newNode);
			}
		}
	}

	private boolean remove(T term, Node<T> node) throws PartialHierarchyException {
		if (term != null && node.getData() != null && !hierarchy.isSuccessorOf(term, node.getData())) return false;
		boolean found = false;
		for (Node<T> child : new ArrayList<>(node.getChildren())) {
			if (Objects.equals(child.getData(), term)) {
				// if found, delete child node and hook up grand-children
				removeChildNode(child, node);
				found = true;
			}
			else {
				// other search recursive
				if (remove(term, child)) {
					found = true;
				}
			}
		}
		return found;
	}

	private void removeChildNode(Node<T> child, Node<T> parent) {
		List<Node<T>> grandChildren = child.getChildren();
		parent.removeChild(child);
		for (Node<T> grandChild : grandChildren) {
			grandChild.parents.remove(child);
			parent.addChild(grandChild);
		}
	}

	/**
	 * Returns dash tree of this hierarchy, mainly for debugging purposes.
	 */
	@SuppressWarnings("unused")
	public String toDashTree() {
		StringBuilder builder = new StringBuilder();
		for (Node<T> child : root.getChildren()) {
			toDashTree(child, 0, builder);
		}
		return builder.toString();
	}

	private void toDashTree(Node<T> currentNode, int currentDepth, StringBuilder builder) {
		builder.append("-".repeat(Math.max(0, currentDepth)));
		if (currentDepth > 0) builder.append(" ");
		builder.append(currentNode.data).append("\n");

		for (Node<T> child : currentNode.getChildren()) {
			toDashTree(child, currentDepth + 1, builder);
		}
	}

	public static class Node<T> {
		private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

		final T data;
		private final transient Collection<Node<T>> parents = new MinimizedHashSet<>();
		private final List<Node<T>> children = new ArrayList<>();

		private Comparator<T> comparator;

		@Override
		public String toString() {
			if (data == null) {
				return super.toString() + " data=null";
			}
			return data.toString();
		}

		public Node(T data, Comparator<T> comparator) {
			this.data = data;
			this.comparator = comparator;
		}

		public Node(T data) {
			this.data = data;
		}

		/**
		 * Creates a copy of the subtree of this node.
		 *
		 * @return root node of the subtree copy
		 */
		public Node<T> copyWithSubTree() {
			Node<T> copyRoot = new Node<>(this.data, this.comparator);
			this.children.forEach(child -> copyRoot.addChild(child.copyWithSubTree()));
			return copyRoot;
		}

		/**
		 * Returns the parent nodes of this node, returns empty list if this node is on
		 * top-level.
		 *
		 * @created 26.11.2013
		 */
		@NotNull
		public Collection<Node<T>> getParents() {
			return parents;
		}

		@Override
		public int hashCode() {
			if (data != null) {
				return data.hashCode();
			}
			else {
				return super.hashCode();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Node<?>) {
				Object otherData = ((Node<?>) obj).getData();
				if (otherData == null && data == null) {
					return super.equals(obj);
				}
				//noinspection SimplifiableIfStatement
				if (data == null) {
					return false;
				}
				return data.equals(otherData);
			}
			return false;
		}

		public void addParent(Node<T> parent) {
			if (parent.data == null) return;
			this.parents.add(parent);
		}

		public T getData() {
			return data;
		}

		public void addChild(Node<T> node) {
			if (!children.contains(node)) {
				children.add(node);
				node.addParent(this);
			}
		}

		public boolean removeChild(Node<T> child) {
			return this.children.remove(child);
		}

		public boolean containsChild(Node<T> n) {
			return children.contains(n);
		}

		public List<Node<T>> getChildren() {
			if (this.comparator != null) {
				return getChildrenSorted(this.comparator);
			}
			return Collections.unmodifiableList(children);
		}

		public List<Node<T>> getChildrenSortedDefault() {
			return getChildrenSorted(new DefaultComparator());
		}

		public List<Node<T>> getChildrenSorted(Comparator<T> c) {
			List<Node<T>> copy = new ArrayList<>(children);
			copy.sort(new PartialHierarchyNodeComparator<>(c));
			return Collections.unmodifiableList(copy);
		}

		public void removeParent(Node<T> parent) {
			parents.remove(parent);
		}

		class DefaultComparator implements Comparator<T> {

			@SuppressWarnings("unchecked")
			@Override
			public int compare(T o1, T o2) {
				if (o1 instanceof Comparable && o2 instanceof Comparable) {
					return ((Comparable<T>) o1).compareTo((o2));
				}
				throw new IllegalArgumentException(
						"DefaultComparator can not be used. Object are no Comparables!");
			}
		}
	}
}
