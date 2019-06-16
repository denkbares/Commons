package com.denkbares.collections.test;

import com.denkbares.collections.PartialHierarchyException;
import com.denkbares.collections.PartialHierarchyTree;
import com.denkbares.collections.PartialHierarchyTree.Node;

import org.junit.Test;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class PartialHierarchyTreeTest {

	@Test
	public void testHierarchyBasic() throws PartialHierarchyException {
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
		assertEquals(Arrays.asList("B", "A"), asStringList(tree.getRootLevelNodesSorted(Comparator.comparing(String::toString)
				.reversed())));

		Node<String> aNode = tree.find(a);
		assertNotNull(aNode);
		assertEquals(aNode.getData(), a);
		assertEquals(0, aNode.getChildren().size());

		Node<String> bNode = tree.find(b);
		assertNotNull(bNode);
		assertEquals(bNode.getData(), b);
		assertEquals(0, bNode.getChildren().size());
		assertEquals(0, tree.getMaxDepthLevel(bNode));

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
		assertNotNull(baNode);
		assertEquals(baNode.getData(), ba);
		assertEquals(1, tree.getMaxDepthLevel(baNode));
		assertEquals(0, baNode.getChildren().size());
		assertEquals(baNode.getParents().iterator().next().getData(), b);

		// b should have BA as child now
		bNode = tree.find(b);
		assertNotNull(bNode);
		assertEquals(bNode.getData(), b);
		List<Node<String>> bChildren = bNode.getChildren();
		assertEquals(1, bChildren.size());
		assertTrue(bChildren.contains(new Node<>(ba)));

		// A should be still alone
		aNode = tree.find(a);
		assertNotNull(aNode);
		assertEquals(aNode.getData(), a);
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
	public void testHierarchyRestructuring1() throws PartialHierarchyException {
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
		assertEquals(baNode.getParents().iterator().next(), tree.find(b));
		List<Node<String>> baChildren = baNode.getChildren();
		assertEquals(1, baChildren.size());
		assertTrue(baChildren.contains(tree.find(bac)));

		// remove ba
		boolean removed = tree.remove(ba);
		assertTrue(removed);
		assertFalse(tree.remove(ba));
		assertNull(tree.find(ba));

		// then BAC should be child of B
		assertEquals(2, tree.getNodeCount());
		Node<String> bacNode = tree.find(bac);
		Node<String> bNode = tree.find(b);
		List<Node<String>> bChildren = bNode.getChildren();
		assertEquals(1, bChildren.size());
		assertTrue(bChildren.contains(bacNode));
		assertEquals(bacNode.getParents().iterator().next(), bNode);

		// test leafs
		final Collection<String> leafNodes = tree.getLeafNodes();
		assertEquals(1, leafNodes.size());
		assertTrue(leafNodes.contains(bac));

	}

	@Test
	public void testHierarchyRestructuring2() throws PartialHierarchyException {
		PartialHierarchyTree<String> tree = new PartialHierarchyTree<>(
				new StringPrefixHierarchy());

		String b = "B";
		String ba = "BA";
		String bi = "BI";

		tree.insertNode(ba);
		tree.insertNode(bi);

		// check BA
		Node<String> baNode = tree.find(ba);
		assertNotNull(baNode);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParents().isEmpty());

		// check BI
		Node<String> biNode = tree.find(bi);
		assertNotNull(biNode);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParents().isEmpty());

		// now insert B
		tree.insertNode(b);
		assertEquals(3, tree.getNodeCount());

		Node<String> bNode = tree.find(b);
		baNode = tree.find(ba);
		biNode = tree.find(bi);

		// check B
		assertNotNull(bNode);
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
		assertNotNull(baNode);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParents().contains(bNode) && baNode.getParents().size() == 1);

		// check BI again
		assertNotNull(biNode);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParents().contains(bNode) && baNode.getParents().size() == 1);

		// remove B
		boolean removedB = tree.remove(b);
		assertTrue(removedB);
		assertFalse(tree.remove(b));
		assertNull(tree.find(b));

		// check BA again
		baNode = tree.find(ba);
		assertNotNull(baNode);
		assertEquals(0, baNode.getChildren().size());
		assertTrue(baNode.getParents().isEmpty());

		// check BI again
		biNode = tree.find(bi);
		assertNotNull(biNode);
		assertEquals(0, biNode.getChildren().size());
		assertTrue(biNode.getParents().isEmpty());

	}

	private List<String> asStringList(List<Node<String>> childrenSortedDefault) {
		return childrenSortedDefault.stream().map(Node::toString).collect(toList());
	}

}
