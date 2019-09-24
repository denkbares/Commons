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

package com.denkbares.utils.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import com.denkbares.strings.Strings;
import com.denkbares.utils.ReplacingOutputStream;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 16.11.2017
 */
public class ReplacingOutputStreamTest {
	@Test
	public void basic() throws Exception {
		assertReplace("fooblafoo", "foo${foo}foo", "foo", "bla");
		assertReplace("fooblafoobla", "foo${foo}foo${foo}", "foo", "bla");
		assertReplace("foo${foo}foo", "foo${foo}foo", "xxx", "bla");
		assertReplace("foofoo", "foo${foo}foo", "foo", "");

		assertReplace("foofoo", "foo${}foo", "", "");
		assertReplace("fooblafoo", "foo${}foo", "", "bla");
		assertReplace("foo${}foo", "foo${}foo", "xxx", "bla");

		assertReplace("foobla", "foo${foo}", "foo", "bla");
		assertReplace("blafoo", "${foo}foo", "foo", "bla");
		assertReplace("bla", "${foo}", "foo", "bla");
		assertReplace("${foo}", "${foo}", "xxx", "bla");
		assertReplace("", "${foo}", "foo", "");
	}

	@Test
	public void unbalanced() throws Exception {
		assertReplace("foo${foo", "foo${foo", "foo", "bla");
		assertReplace("foobla}", "foo${foo}}", "foo", "bla");
	}

	@Test
	public void complex() throws Exception {
		Assert.assertEquals("text-17", replace("text-${Math.add(13,4)}", expr ->
				expr.startsWith("Math.add(")
						? String.valueOf(Arrays.stream(expr.substring(9, expr.length() - 1).split("\\s*,\\s*"))
						.map(Strings::trim).mapToInt(Integer::valueOf).sum())
						: null));
	}

	private void assertReplace(String expected, String text, String variable, String replacement) throws IOException {
		Assert.assertEquals(expected, replace(text, variable, replacement));
	}

	private String replace(String text, String variable, String replacement) throws IOException {
		Function<String, String> replacer = Collections.singletonMap(variable, replacement)::get;
		return replace(text, replacer);
	}

	@NotNull
	private String replace(String text, Function<String, String> replacer) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		ReplacingOutputStream replace = new ReplacingOutputStream(result, replacer);
		replace.write(text.getBytes(Strings.Encoding.UTF8.charset()));
		replace.flush();
		return new String(result.toByteArray(), Strings.Encoding.UTF8.charset());
	}
}
