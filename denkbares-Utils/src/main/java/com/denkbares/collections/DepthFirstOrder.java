/*
 * Copyright (C) 2019 denkbares GmbH. All rights reserved.
 */

package com.denkbares.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Class that creates a depth first order, based on se set of start nodes, and a successor function. It considers that
 * multiple paths may lead to a single successor node, and avoids running into cycles.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 22.09.2019
 */
public class DepthFirstOrder<Node> {

	private final Set<Node> startNodes = new LinkedHashSet<>();
	private final Function<Node, Collection<Node>> successors;

	private Map<Node, Integer> positions;
	private int nextPosition;

	@SafeVarargs
	public DepthFirstOrder(Function<Node, Collection<Node>> successors, Node... startNodes) {
		this(successors, Arrays.asList(startNodes));
	}

	public DepthFirstOrder(Function<Node, Collection<Node>> successors, Collection<Node> startNodes) {
		this.successors = successors;
		this.startNodes.addAll(startNodes);
	}

	public List<Node> getOrderedNodes() {
		if (positions == null) {
			positions = new HashMap<>();
			nextPosition = 0;
			for (Node node : startNodes) {
				recurse(node, new HashSet<>());
			}
		}

		List<Node> order = new ArrayList<>(positions.keySet());
		order.sort(Comparator.comparingInt(positions::get));
		return order;
	}

	private void recurse(Node node, Set<Node> stackNodes) {
		// if we reach a node for the second time, we continue to recurse,
		// because we have to re-mark them with a higher number,
		// but if we have it in the call-stack of the recursion, we skip to resolve cycles
		if (!stackNodes.add(node)) return;

		// mark the node with the position
		positions.put(node, nextPosition++);
		for (Node next : successors.apply(node)) {
			recurse(next, stackNodes);
		}

		// finally we remove the node before returning, to have only the nodes of the call stack
		stackNodes.remove(node);
	}
}
