package com.denkbares.collections.test;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.SegmentTree;

public class SegmentTreeTest {

	@Test
	public void testSegments() {
		SegmentTree<String> tree = new SegmentTree<>(0, 10, "root");
		tree.insert(0, 5, "A");
		tree.insert(5, 10, "B");
		tree.insert(1, 2, "C");

		String expected = """
				[0, 10]: root
				  [0, 5]: A
				    [1, 2]: C
				  [5, 10]: B
				""";
		String actual = tree.toString();

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testOverlapping() {
		SegmentTree<String> tree = new SegmentTree<>(0, 10, "root");
		tree.insert(1, 4, "A");
		tree.insert(3, 7, "B");
		tree.insert(6, 10, "C");

		String expected = """
				[0, 10]: root
				  [1, 4]: A
				  [3, 7]: B
				  [6, 10]: C
				""";
		String actual = tree.toString();

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testRestructuring() {
		SegmentTree<String> tree = new SegmentTree<>(0, 10, "root");
		tree.insert(6, 7, "B");
		tree.insert(1, 2, "A");
		tree.insert(5, 10, "D");
		tree.insert(0, 5, "C");

		String expected = """
				[0, 10]: root
				  [0, 5]: C
				    [1, 2]: A
				  [5, 10]: D
				    [6, 7]: B
				""";
		String actual = tree.toString();

		Assert.assertEquals(expected, actual);
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertInvalid() {
		SegmentTree<String> tree = new SegmentTree<>(0, 10, "root");
		tree.insert(0, 11, "foo");
	}
}
