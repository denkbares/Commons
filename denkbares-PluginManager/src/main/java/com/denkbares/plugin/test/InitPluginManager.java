/*
 * Copyright (C) 2009 denkbares GmbH
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

package com.denkbares.plugin.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.denkbares.plugin.JPFPluginManager;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Log;

/**
 * Provides a static method to initialize the JPF-PluginManager by using a classpath file generated
 * from the Maven dependency plugin
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public final class InitPluginManager {

	/**
	 * Avoids the creation of an instance for this class.
	 */
	private InitPluginManager() {
	}

	/**
	 * Initializes the JPF-PluginManager with the information stored in
	 * "target/dependencies/output.txt". This file can be generated with the maven dependency
	 * plugin. If the plugin manager is already initialized, the method does nothing.
	 * <p>
	 * Important: Tests using this function must run maven install after each dependency update
	 *
	 * @param pluginFilterPattern specifies patterns to filter plugins to be loaded by the plugin
	 *                            manager. If no specific patterns are given, d3web-Plugins and KnowWE-Plugins are
	 *                            exclusively
	 *                            loaded.
	 * @throws IOException if the dependencies file or references plugins could not been read
	 */
	public static void init(String... pluginFilterPattern) throws IOException {
		init(new File("target/dependencies/output.txt"), pluginFilterPattern);
	}

	/**
	 * Initializes the JPF-PluginManager with the information stored in
	 * "target/dependencies/output.txt" This file can be generated with the maven dependency plugin.
	 * If the plugin manager is already initialized, the method does nothing. <p> Important: Tests
	 * using this function must run maven install after each dependency update
	 *
	 * @param pluginFilterPattern specifies patterns to filter plugins to be loaded by the plugin
	 *                            manager. If no specific patterns are given, d3web-Plugins and KnowWE-Plugins are
	 *                            exclusively
	 *                            loaded.
	 * @throws IOException if the classpath file or references plugins could not been read
	 */
	public static void init(File classpathFile, String... pluginFilterPattern) throws IOException {
		if (!Files.exists(classpathFile.toPath())) {
			Log.severe("Dependency information file does not exist: " + classpathFile.getAbsolutePath());
		}
		init(Strings.readFile(classpathFile).split(";"), pluginFilterPattern);
	}

	/**
	 * Initializes the JPF-PluginManager with a list of plugin files. This file can be generated
	 * with the maven dependency plugin. If the plugin manager is already initialized, the method
	 * does nothing.
	 * <p>
	 * Important: Tests using this function must run maven install after each dependency update
	 *
	 * @param pluginFilterPattern specifies patterns to filter plugins to be loaded by the plugin
	 *                            manager. If no specific patterns are given, d3web-Plugins and KnowWE-Plugins are
	 *                            exclusively
	 *                            loaded.
	 */
	public static void init(String[] jarFiles, String... pluginFilterPattern) {
		if (pluginFilterPattern == null || pluginFilterPattern.length == 0) {
			pluginFilterPattern = new String[] { "^d3web-Plugin.*", "^KnowWE-Plugin.*", "^denkbares-(.+-)?Plugin-.+", "^SemanticAnalytics.*" };
		}
		List<File> filteredJars = new ArrayList<>();
		// adding the plugin itself
		File ownSources = new File("target/classes");
		if (checkIfPlugin(ownSources, pluginFilterPattern)) {
			filteredJars.add(ownSources);
		}
		for (String s : jarFiles) {
			File jarFile = new File(s);
			if (checkIfPlugin(jarFile, pluginFilterPattern)) {
				filteredJars.add(jarFile);
			}
		}
		JPFPluginManager.init(filteredJars.toArray(new File[filteredJars.size()]));
	}

	private static boolean checkIfPlugin(File file, String... pluginFilterPattern) {
		File project = file;
		if ("classes".equals(file.getName())) {
			// jump two levels higher because dependencies to eclipse
			// projects are named: projectName/target/classes
			// the absolute file is needed to prevent a NullPointerException in the own
			// project
			project = file.getParentFile().getAbsoluteFile();
			project = project.getParentFile();
		}
		String projectName = project.getName();
		return JPFPluginManager.isPlugin(projectName, pluginFilterPattern);
	}
}
