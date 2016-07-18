package com.denkbares.collections.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.denkbares.collections.PartialHierarchyTree;
import com.denkbares.collections.PartialHierarchyTree.Node;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class PartialHierarchyTreeTest {

	@Test
	public void testHierarchyBasic() {
		PartialHierarchyTree<String> tree = new PartialHierarchyTree<>(
				new StringPrefixHierarchy());

		assertEquals(0, tree.getNodeCount());

		String a = "A";
		String b = "B";
		String ba = "BA";

		// insert A
		tree.insertNode(a);

		assertEquals(1, tree.getNodeCount());

		// insert B
		tree.insertNode(b);


		assertEquals(2, tree.getNodeCount());
		assertEquals(2, tree.getNodes().size());
		assertEquals(2, tree.getRootLevelNodes().size());
		assertEquals(Arrays.asList("A", "B"), asStringList(tree.getRootLevelNodes()));
		assertEquals(Arrays.asList("B", "A"), asStringList(tree.getRootLevelNodesSorted(Comparator.comparing(String::toString).reversed())));

		Node<String> aNode = tree.find(a);
		assertTrue(aNode != null);
		assertTrue(aNode.getData().equals(a));
		assertEquals(0, aNode.getChildren().size());

		Node<String> bNode = tree.find(b);
		assertTrue(bNode != null);
		assertTrue(bNode.getData().equals(b));
		assertEquals(0, bNode.getChildren().size());
		assertEquals(0, tree.getDepthLevel(bNode));

		// insert BA
		tree.insertNode(ba);
		assertEquals(3, tree.getNodeCount());
		assertEquals(3, tree.getNodes().size());

		// check DFS order
		assertEquals(Arrays.asList("A", "B", "BA"), tree.getNodesDFSOrder());

		ArrayList<String> actual = new ArrayList<>(tree.getNodeContents());
		actual.sort(Comparator.comparing(String::toString));
		assertEquals(Arrays.asList("A", "B", "BA"), actual);

		Node<String> baNode = tree.find(ba);
		assertTrue(baNode != null);
		assertTrue(baNode.getData().equals(ba));
		assertEquals(1, tree.getDepthLevel(baNode));
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParent().getData().equals(b));

		// b should have BA as child now
		bNode = tree.find(b);
		assertTrue(bNode != null);
		assertTrue(bNode.getData().equals(b));
		List<Node<String>> bChildren = bNode.getChildren();
		assertEquals(1, bChildren.size());
		assertTrue(bChildren.contains(new Node<>(ba)));


		// A should be still alone
		aNode = tree.find(a);
		assertTrue(aNode != null);
		assertTrue(aNode.getData().equals(a));
		assertEquals(0, aNode.getChildren().size());

		// test leafs
		final Collection<String> leafNodes = tree.getLeafNodes();
		assertEquals(2, leafNodes.size());
		assertTrue(leafNodes.contains(a));
		assertTrue(leafNodes.contains(ba));

		tree.removeAll(Arrays.asList(a, b, ba));
		assertEquals(0, tree.getNodes().size());
	}

	@Test
	public void testHierarchyRestructuring1() {
		PartialHierarchyTree<String> tree = new PartialHierarchyTree<>(
				new StringPrefixHierarchy());

		String b = "B";
		String ba = "BA";
		String bac = "BAC";

		tree.insertNode(ba);
		tree.insertNode(bac);
		tree.insertNode(b);

		// check correct insertion
		assertEquals(3, tree.getNodeCount());

		Node<String> baNode = tree.find(ba);
		assertTrue(baNode.getParent().equals(tree.find(b)));
		List<Node<String>> baChildren = baNode.getChildren();
		assertEquals(1, baChildren.size());
		assertTrue(baChildren.contains(tree.find(bac)));

		// remove ba
		boolean removed = tree.remove(ba);
		assertTrue(removed);
		assertFalse(tree.remove(ba));
		assertEquals(null, tree.find(ba));

		// then BAC should be child of B
		assertEquals(2, tree.getNodeCount());
		Node<String> bacNode = tree.find(bac);
		Node<String> bNode = tree.find(b);
		List<Node<String>> bChildren = bNode.getChildren();
		assertEquals(1, bChildren.size());
		assertTrue(bChildren.contains(bacNode));
		assertTrue(bacNode.getParent().equals(bNode));

		// test leafs
		final Collection<String> leafNodes = tree.getLeafNodes();
		assertEquals(1, leafNodes.size());
		assertTrue(leafNodes.contains(bac));

	}

	@Test
	public void testHierarchyRestructuring2() {
		PartialHierarchyTree<String> tree = new PartialHierarchyTree<>(
				new StringPrefixHierarchy());

		String b = "B";
		String ba = "BA";
		String bi = "BI";

		tree.insertNode(ba);
		tree.insertNode(bi);

		// check BA
		Node<String> baNode = tree.find(ba);
		assertTrue(baNode != null);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParent() == null);

		// check BI
		Node<String> biNode = tree.find(bi);
		assertTrue(biNode != null);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParent() == null);

		// now insert B
		tree.insertNode(b);
		assertEquals(3, tree.getNodeCount());

		Node<String> bNode = tree.find(b);
		baNode = tree.find(ba);
		biNode = tree.find(bi);

		// check B
		assertTrue(bNode != null);
		List<Node<String>> bChildren = bNode.getChildren();
		assertEquals(2, bChildren.size());
		assertTrue(bChildren.contains(biNode));
		assertTrue(bChildren.contains(baNode));
		assertTrue(bNode.containsChild(biNode));
		assertEquals(Arrays.asList("BA", "BI"),
				asStringList(bNode.getChildrenSortedDefault()));
		assertEquals(Arrays.asList("BI", "BA"),
				asStringList(bNode.getChildrenSorted(Comparator.comparing(String::toString).reversed())));

		// check BA again
		assertTrue(baNode != null);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParent().equals(bNode));

		// check BI again
		assertTrue(biNode != null);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParent().equals(bNode));

		// remove B
		boolean removedB = tree.remove(b);
		assertTrue(removedB);
		assertFalse(tree.remove(b));
		assertEquals(null, tree.find(b));

		// check BA again
		baNode = tree.find(ba);
		assertTrue(baNode != null);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParent() == null);

		// check BI again
		biNode = tree.find(bi);
		assertTrue(biNode != null);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParent() == null);

	}

	private List<String> asStringList(List<Node<String>> childrenSortedDefault) {
		return childrenSortedDefault.stream().map(Node::toString).collect(toList());
	}

}
