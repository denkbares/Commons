/*
 * Copyright (C) 2017 denkbares GmbH, Germany 
 */
package com.denkbares.streams.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.denkbares.streams.ReplacingInputStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ReplacingInputStream}.
 *
 * @author Sebastian Furth (denkbares GmbH)
 * @created 19.01.17
 */
public class ReplacingInputStreamTest {

	@Test
	public void testReplacement() throws IOException {

		byte[] bytes = "hello xyz world.".getBytes("UTF-8");

		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

		Map<byte[], byte[]> replacements = new HashMap<>();
		byte[] search = "xyz".getBytes("UTF-8");
		byte[] replacement = "abc".getBytes("UTF-8");
		replacements.put(search, replacement);

		InputStream ris = new ReplacingInputStream(bis, replacements);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = ris.read())) {
			bos.write(b);
		}

		assertEquals("hello abc world.", new String(bos.toByteArray()));

	}



}
