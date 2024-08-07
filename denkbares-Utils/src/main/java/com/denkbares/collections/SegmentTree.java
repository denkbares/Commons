package com.denkbares.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

/**
 * A segment tree is a special case of a {@link PartialHierarchy}: The root node forms an interval, e.g. [0, 10].
 * Each recursive child node is a subset of the parent interval, e.g.,
 * <pre>
 * A: [0, 10]
 * - B: [0, 5]
 * -- D: [1, 2]
 * - C: [5, 10]
 * </pre>
 * Items can be inserted without worrying about hierarchy as long as they are a segment of the original interval.
 * The items can then be traversed given their is-subset-of relations.
 * <br>
 * This data structure was originally implemented to work with parse trees. Here, it's convenient to simplify the
 * original hierarchy of segments by simply inserting segments into a new structure.
 *
 * @param <T> the kind of data each node holds
 */
public class SegmentTree<T> {

	private final Node root;

	/**
	 * Create a new tree for the interval [start, end]. Each subsequent insertion must be within this interval.
	 *
	 * @param start the start of the whole interval
	 * @param end   the end of the whole interval
	 * @param item  the root item
	 */
	public SegmentTree(int start, int end, T item) {
		this.root = new Node(start, end, item);
	}

	/**
	 * Allows to visit each node by traversing the tree top-to-bottom, left-to-right, aka "depth-first".
	 *
	 * @param consumer a consumer for each node
	 */
	public void visit(Consumer<Node> consumer) {
		this.root.visit(consumer);
	}

	/**
	 * Insert an item into the tree. Throws if the interval doesn't lie within the tree's interval.
	 * Note, that currently multiple overlapping intervals are allowed, as long as they are subsets of their parent.
	 *
	 * @param start the start of the segment
	 * @param end   the end of the segment
	 * @param item  the item to insert
	 * @throws IllegalArgumentException has no is-subset-of relation within the tree
	 */
	public void insert(int start, int end, T item) {
		if (root.contains(start, end)) {
			root.addItem(start, end, item);
		}
		else {
			throw new IllegalArgumentException("Segment to insert is not a sub-set of this tree");
		}
	}

	public Node getRoot() {
		return root;
	}

	@Override
	public String toString() {
		return root.toString();
	}

	/**
	 * A node of the segment tree.
	 */
	public final class Node {

		private final int start;
		private final int end;
		private final T item;
		private final List<Node> children = new ArrayList<>();

		private Node(int start, int end, @NotNull T item) {
			if (start > end) {
				throw new IllegalArgumentException("Start cannot be greater than end: " + start + " > " + end);
			}
			this.start = start;
			this.end = end;
			this.item = item;
		}

		void addItem(int start, int end, T item) {
			// check if the new node should be a child of the current one
			for (Node child : children) {
				if (child.contains(start, end)) {
					child.addItem(start, end, item);
					return;
				}
			}

			// otherwise, create a new node and potentially re-parent existing children
			Node newNode = new Node(start, end, item);
			List<Node> newChildren = new ArrayList<>();
			for (Node child : children) {
				if (newNode.contains(child.start, child.end)) {
					newChildren.add(child);
				}
			}
			children.removeAll(newChildren);
			newNode.children.addAll(newChildren);
			children.add(newNode);
			children.sort(Comparator.comparingInt((Node a) -> a.start));
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public boolean isLeaf() {
			return children.isEmpty();
		}

		public List<Node> getChildren() {
			return children;
		}

		@NotNull
		public T getItem() {
			return item;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			buildString(builder, "");
			return builder.toString();
		}

		public void visit(Consumer<Node> visitor) {
			visitor.accept(this);
			for (Node child : children) {
				child.visit(visitor);
			}
		}

		private void buildString(StringBuilder builder, String prefix) {
			builder.append(prefix)
					.append('[')
					.append(start)
					.append(", ")
					.append(end)
					.append("]: ")
					.append(item)
					.append("\n");
			for (Node child : children) {
				child.buildString(builder, prefix + "  ");
			}
		}

		private boolean contains(int start, int end) {
			return this.start <= start && this.end >= end;
		}
	}
}
