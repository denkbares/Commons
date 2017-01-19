/*
 * Copyright (C) 2017 denkbares GmbH, Germany 
 */
package com.denkbares.streams;

import java.io.*;
import java.util.*;

/**
 * Implementation of the {@link java.io.FilterInputStream} interface that replaces a defined
 * String (byte[]) while reading a stream.
 *
 * @author Sebastian Furth (denkbares GmbH)
 * @created 19.01.17
 */
public class ReplacingInputStream extends FilterInputStream {

	private final LinkedList<Integer> inQueue = new LinkedList<>();
	private final LinkedList<Integer> outQueue = new LinkedList<>();
	private final byte[] search, replacement;

	public ReplacingInputStream(InputStream in, byte[] search, byte[] replacement) {
		super(in);
		this.search = search;
		this.replacement = replacement;
	}

	private boolean isMatchFound() {
		Iterator<Integer> inIter = inQueue.iterator();
		for (int i = 0; i < search.length; i++)
			if (!inIter.hasNext() || search[i] != inIter.next())
				return false;
		return true;
	}

	private void readAhead() throws IOException {
		// Work up some look-ahead.
		while (inQueue.size() < search.length) {
			int next = super.read();
			inQueue.offer(next);
			if (next == -1)
				break;
		}
	}

	@Override
	public int read() throws IOException {

		// Next byte already determined.
		if (outQueue.isEmpty()) {

			readAhead();

			if (isMatchFound()) {
				for (int i = 0; i < search.length; i++)
					inQueue.remove();

				for (byte b : replacement)
					outQueue.offer((int) b);
			}
			else
				outQueue.add(inQueue.remove());
		}

		return outQueue.remove();
	}

}



