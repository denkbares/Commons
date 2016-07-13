package com.denkbares.collections.test;

import com.denkbares.collections.PartialHierarchy;

public class StringPrefixHierarchy implements PartialHierarchy<String> {

	@Override
	public boolean isSuccessorOf(String node1, String node2) {
		return node1.startsWith(node2);
	}

}
