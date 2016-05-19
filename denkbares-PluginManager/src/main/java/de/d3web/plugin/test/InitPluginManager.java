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

package de.d3web.plugin.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.d3web.plugin.JPFPluginManager;
import de.d3web.strings.Strings;

/**
 * Provides a static method to initialize the JPF-PluginManager by using a classpath file generated
 * from the Maven dependency plugin
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public abstract class InitPluginManager {

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
	 * @throws IOException
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
	 * @throws IOException
	 */
	public static void init(File classpathFile, String... pluginFilterPattern) throws IOException {
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
			pluginFilterPattern = new String[] { "^d3web-Plugin.*", "^KnowWE-Plugin.*", "^denkbares-(.+-)?Plugin-.+" };
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
		if (file.getName().equals("classes")) {
			// jump two levels higher because dependencies to eclipse
			// projects are named: projectname/target/classes
			// the absolute file is needed to prevent a nullpointer in the own
			// project
			project = file.getParentFile().getAbsoluteFile();
			project = project.getParentFile();
		}
		String projectName = project.getName();
		return JPFPluginManager.isPlugin(projectName, pluginFilterPattern);
	}
}
