/*
 * Copyright (C) 2011 denkbares GmbH
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
package com.denkbares.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import com.denkbares.plugin.JPFPluginManager;
import com.denkbares.plugin.Plugin;
import com.denkbares.plugin.PluginManager;
import com.denkbares.plugin.Resource;

/**
 * A test for plugin resources
 * 
 * @author Markus Friedrich (denkbares GmbH)
 * @created 03.08.2011
 */
public class TestResource {

	@Before
	public void initPluginManager() {
		JPFPluginManager.init("src/test/resources");
	}

	@Test
	public void testFolderPlugin() throws IOException {
		Plugin plugin = PluginManager.getInstance().getPlugin("TestResourcePlugin");
		testPlugin(plugin, "Just a test file");
	}

	@Test
	public void testJarPlugin() throws IOException {
		Plugin plugin = PluginManager.getInstance().getPlugin("JarTestResourcePlugin");
		testPlugin(plugin, "Just a test file\nin a jar.");
	}

	private void testPlugin(Plugin plugin, String s) throws IOException {
		Assert.assertNotNull(plugin);
		Resource[] resources = plugin.getResources();
		List<Resource> filteredResources = new LinkedList<>();
		for (Resource r : resources) {
			if (!r.getPathName().contains(".svn")) {
				filteredResources.add(r);
			}
		}
		Assert.assertEquals(1, filteredResources.size());
		Resource resource = filteredResources.get(0);
		Assert.assertTrue(resource.getPathName().endsWith("Test.txt"));
		// check the content of the file
		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try (InputStream is = resource.getInputStream()) {
			Reader reader = new BufferedReader(
					new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		}
		Assert.assertTrue(writer.toString().equals(s));
	}

}
