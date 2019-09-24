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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.collections.Matrix;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Files;
import com.denkbares.utils.Streams;

import static org.junit.Assert.*;

/**
 * This test does only test methods which are not used very frequently and are therefore not tested
 * by other tests already (like Headless-App-Tests).
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 18.10.2013
 */
public class FilesTest {

	private static final String TXT_FILE = "src/test/resources/exampleFiles/faust.txt";
	private static final String JPG_FILE = "src/test/resources/exampleFiles/faust.jpg";
	private static final String PROPERTIES_FILE = "src/test/resources/exampleFiles/test.properties";

	@Test
	public void readFiles() throws IOException {
		checkBinarySize(JPG_FILE);
		checkBinarySize(TXT_FILE);

		assertEquals(
				"check text length of '" + TXT_FILE + "'",
				219369,
				Files.getText(new File(TXT_FILE)).replace("\r", "").length());
	}

	@Test
	public void copy() throws IOException {
		File target = new File("target/copyTest.txt");
		target.delete();
		File source = new File(TXT_FILE);
		Files.copy(source, target);
		assertArrayEquals(Files.getBytes(source), Files.getBytes(target));
	}

	@Test
	public void hasEqualFingerprint() throws IOException {
		assertTrue(Files.hasEqualFingerprint(new File(TXT_FILE), new File(TXT_FILE)));
		assertTrue(Files.hasEqualFingerprint(new File(JPG_FILE), new File(JPG_FILE)));
		assertFalse(Files.hasEqualFingerprint(new File(TXT_FILE), new File(JPG_FILE)));
		File target = new File("target/copyTest.txt");
		target.delete();
		File source = new File(TXT_FILE);
		InputStream in = new FileInputStream(source);
		OutputStream out = new FileOutputStream(target);
		Streams.stream(in, out);
		assertFalse(Files.hasEqualFingerprint(source, target));
		target.setLastModified(source.lastModified());
		assertTrue(Files.hasEqualFingerprint(source, target));

		assertFalse(Files.hasEqualFingerprint(new File(""), new File("")));
		assertFalse(Files.hasEqualFingerprint(source, new File("")));
	}

	@Test
	public void testGetCSVCells() throws IOException {
		String path = "target/csvTest.csv";
		Strings.writeFile(path, "11,12,13\n21,22,23\n31,32,33");
		Matrix<String> csvCells = Files.getCSVCells(new File(path));
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals((i + 1) + "" + (j + 1), csvCells.get(i, j));
			}
		}
	}

	@Test
	public void hasEqualContent() throws IOException {
		assertTrue(Files.hasEqualContent(new File(TXT_FILE), new File(TXT_FILE)));
		assertTrue(Files.hasEqualContent(new File(JPG_FILE), new File(JPG_FILE)));
		assertFalse(Files.hasEqualContent(new File(TXT_FILE), new File(JPG_FILE)));
		File target = new File("target/copyTest.txt");
		target.delete();
		File source = new File(TXT_FILE);
		Files.copy(source, target);
		assertTrue(Files.hasEqualContent(source, target));

		assertFalse(Files.hasEqualContent(new File(""), new File("")));
		assertFalse(Files.hasEqualContent(source, new File("")));
	}

	public void checkBinarySize(String filename) throws IOException {
		File file = new File(filename);
		assertEquals(
				"check file lenght of '" + file + "'",
				file.length(),
				Files.getBytes(file).length);
	}

	@Test
	public void directories() throws IOException {
		File folder = Files.createTempDir();
		Assert.assertTrue(folder.exists());

		File sub = new File(folder, "foo/bla/goo");
		Assert.assertTrue(sub.mkdirs());

		File file = new File(sub, "test.jpg");
		Streams.streamAndClose(
				new FileInputStream(JPG_FILE),
				new FileOutputStream(file));
		Assert.assertTrue(file.exists());

		Files.recursiveDelete(folder);
		Assert.assertFalse(file.exists());
		Assert.assertFalse(sub.exists());
		Assert.assertFalse(folder.exists());
	}

	@Test
	public void extensions() {
		// test with null
		assertEquals(null, Files.getExtension((String) null));
		assertEquals(null, Files.getExtension((File) null));
		assertEquals(null, Files.stripExtension((String) null));
		assertEquals(null, Files.stripExtension((File) null));
		assertFalse(Files.hasExtension((String) null, "jpg", "txt"));
		assertFalse(Files.hasExtension((File) null, "jpg", "txt"));
		assertFalse(Files.hasExtension((String) null, (String) null));
		assertFalse(Files.hasExtension((File) null, (String[]) null));

		String txtFileName = "hello world..foo.txt";
		File txtFile = new File(txtFileName);
		assertEquals("txt", Files.getExtension(txtFileName));
		assertEquals("txt", Files.getExtension(txtFile));
		assertEquals("hello world..foo", Files.stripExtension(txtFileName));
		assertEquals("hello world..foo", Files.stripExtension(txtFile));
		assertTrue(Files.hasExtension(txtFileName, "jpg", "txt"));
		assertTrue(Files.hasExtension(txtFile, "jpg", "txt"));
		assertFalse(Files.hasExtension(txtFileName, "foo", "bla"));
		assertFalse(Files.hasExtension(txtFile, "foo", "bla"));
		assertFalse(Files.hasExtension(txtFileName, (String) null));
		assertFalse(Files.hasExtension(txtFile, (String[]) null));

		assertFalse(Files.hasExtension("txt", "txt"));
		assertFalse(Files.hasExtension(".txt", "txt"));
		assertFalse(Files.hasExtension("/.txt", "txt"));
		assertFalse(Files.hasExtension("\\.txt", "txt"));
		assertFalse(Files.hasExtension("foo/.txt", "txt"));
		assertFalse(Files.hasExtension("foo\\.txt", "txt"));
		assertTrue(Files.hasExtension("foo.txt", "txt"));
		assertTrue(Files.hasExtension("/foo.txt", "txt"));
		assertTrue(Files.hasExtension("\\foo.txt", "txt"));
		assertTrue(Files.hasExtension("foo/foo.txt", "txt"));
		assertTrue(Files.hasExtension("foo\\foo.txt", "txt"));

		assertEquals(Files.stripExtension("txt"), "txt");
		assertEquals(Files.stripExtension(".txt"), ".txt");
		assertEquals(Files.stripExtension("/.txt"), "/.txt");
		assertEquals(Files.stripExtension("\\.txt"), "\\.txt");
		assertEquals(Files.stripExtension("foo/.txt"), "foo/.txt");
		assertEquals(Files.stripExtension("foo\\.txt"), "foo\\.txt");
		assertEquals(Files.stripExtension("foo.txt"), "foo");
		assertEquals(Files.stripExtension("/foo.txt"), "/foo");
		assertEquals(Files.stripExtension("\\foo.txt"), "\\foo");
		assertEquals(Files.stripExtension("foo/foo.txt"), "foo/foo");
		assertEquals(Files.stripExtension("foo\\foo.txt"), "foo\\foo");
	}

	@Test
	public void recursiveGet() throws IOException {
		new File("target/dir1/").mkdirs();
		new File("target/dir2/dir3/dir3/").mkdirs();
		Strings.writeFile("target/a1.txt", "testing123");
		Strings.writeFile("target/dir1/a2.txt", "testing123");
		Strings.writeFile("target/dir1/a3.txt", "testing123");
		Strings.writeFile("target/dir2/a4.txt", "testing123");
		Strings.writeFile("target/dir2/dir3/dir3/a5.txt", "testing123");
		List<File> files = new ArrayList<>(Files.recursiveGet(new File("target"), file -> file.getName()
				.endsWith(".txt") && file.getName().startsWith("a")));
		files.sort(Comparator.comparing(File::getName));
		int count = 1;
		for (File file : files) {
			assertEquals("a" + count++ + ".txt", file.getName());
		}
	}

	@Test
	public void getProperties() throws IOException {
		Properties properties = Files.getProperties(new File(PROPERTIES_FILE));
		assertEquals("Hi", properties.getProperty("test.prop.a"));
		assertEquals("Ho", properties.getProperty("test.prop.b"));
		assertEquals("Ha", properties.getProperty("test.prop.c"));
		assertEquals("Hu", properties.getProperty("test.prop.d"));
		assertNull(properties.getProperty("test.prop.e"));
	}

	@Test
	public void updateProperties() throws IOException {
		File properties = new File("target/properties-copy.properties");
		Files.copy(new File(PROPERTIES_FILE), properties);

		Files.updatePropertiesFile(properties, "test.prop.b", "New!");

		assertEquals("New!", Files.getProperties(properties).getProperty("test.prop.b"));
		assertEquals(4, Files.getProperties(properties).entrySet().size());

		Files.updatePropertiesFile(properties, "test.prop.e", "Also New!");

		assertEquals("Also New!", Files.getProperties(properties).getProperty("test.prop.e"));
		assertEquals(5, Files.getProperties(properties).entrySet().size());
	}
}
