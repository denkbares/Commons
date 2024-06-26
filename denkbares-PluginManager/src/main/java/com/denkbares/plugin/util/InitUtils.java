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

package com.denkbares.plugin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jetbrains.annotations.NotNull;

import com.denkbares.plugin.JPFPluginManager;
import com.denkbares.plugin.Plugin;
import com.denkbares.plugin.PluginManager;
import com.denkbares.plugin.Resource;
import com.denkbares.plugin.test.InitPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.denkbares.utils.Streams;

/**
 * Encapsulates application initialization, SWT-free code.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 28.08.2015
 */
public class InitUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(InitUtils.class);

	/**
	 * Initialize the plugin manager, the resources and external viewers. Handles both, started
	 * application in debugger or as a jar-application or as an mac bundle app.
	 *
	 * @param potentialAppNames the potentially known names for the app
	 * @return the root directory used for initializing the application and plugins
	 */
	public static File initPlugins(String... potentialAppNames) throws IOException {
		return initPlugins(Arrays.asList(potentialAppNames));
	}

	/**
	 * Initialize the plugin manager, the resources and external viewers. Handles both, started
	 * application in debugger or as a jar-application or as an mac bundle app.
	 *
	 * @param potentialAppNames the potentially known names for the app
	 * @return the root directory used for initializing the application and plugins
	 */
	public static File initPlugins(Collection<String> potentialAppNames, String... pluginFilterPatterns) throws IOException {
		File rootDirectory = null;
		for (String appName : potentialAppNames) {
			File jarFile = new File(appName + ".jar");
			File appFile = new File(appName + ".app");

			// check bundled jar file
			if (new File("./lib/").exists() && jarFile.exists()) {
				// started as standalone application
				JPFPluginManager.init("./lib/", pluginFilterPatterns);
				rootDirectory = new File("./webapp/");

				// copy files from main jar
				ZipFile zipFile = new ZipFile(jarFile);
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry nextEntry = entries.nextElement();
					if (!nextEntry.isDirectory() && nextEntry.getName().startsWith("webapp/")) {
						File outputFile = new File(nextEntry.getName());
						outputFile.getParentFile().mkdirs();
						FileOutputStream out = new FileOutputStream(outputFile);
						Streams.stream(zipFile.getInputStream(nextEntry), out);
						out.close();
					}
				}
				break;
			}

			// check max osx app
			if (appFile.exists()) {
				LOGGER.info("Mac OSX app detected at: " + appFile.getAbsolutePath());
				// started as MacOS bundle,
				// check both d3webMobile and ServiceMate
				String dirMate = appFile + "/Contents/Resources/ServiceMate/WEB-INF/lib/";
				String dirDialog = appFile + "/Contents/Resources/Java";
				if (new File(dirDialog).isDirectory()) {
					JPFPluginManager.init(dirDialog, pluginFilterPatterns);
					rootDirectory = new File(appFile, "webapp");
					break;
				}
				else if (new File(dirMate).isDirectory()) {
					JPFPluginManager.init(dirMate, pluginFilterPatterns);
					rootDirectory = new File(appFile + "/Contents/Resources/ServiceMate");
					break;
				}
			}

			File winMate = new File(appName + "/WEB-INF/lib");
			if (winMate.isDirectory()) {
				JPFPluginManager.init(winMate.getAbsolutePath(), pluginFilterPatterns);
				rootDirectory = winMate.getParentFile().getParentFile();
			}
		}

		// if still not initialized: use dependency file, running in IDE debugger's war server
		if (rootDirectory == null) {
			File classpathFile = new File("WEB-INF/classes/output.txt");
			if (!classpathFile.exists()) {
				classpathFile = new File("WEB-INF/dependencies/output.txt");
			}
			if (classpathFile.exists()) {
				LOGGER.info("start from debugger detected: dependencies file used");
				InitPluginManager.init(classpathFile, pluginFilterPatterns);
				rootDirectory = new File(".");
			}
		}

		// Find web app
		if (rootDirectory == null) {
			File classFile = getClassFile();
			File libDir = getLibFolder(classFile);
			if (libDir != null) {
				JPFPluginManager.init(libDir.getAbsolutePath(), pluginFilterPatterns);
				rootDirectory = libDir.getParentFile().getParentFile();
			}
			else {
				LOGGER.info("Unable to find lib dir based on code source of: " + classFile);
			}
		}

		// if still not initialized: use "target" folder, running in IDE debugger
		if (rootDirectory == null && new File("target").exists()) {
			LOGGER.info("start from debugger detected: target folder used");
			InitPluginManager.init(pluginFilterPatterns);
			rootDirectory = new File("target/webapp/");
			// copy files from src/main/resources to
			File source = new File("src/main/resources/webapp");
			if (source.exists()) {
				copyDir(source, new File("target/webapp/"));
			}
		}

		// install on root directory
		if (rootDirectory == null) {
			throw new IOException("no valid application root folder was found. " +
					"Current working directory: " + new File(".").getAbsolutePath());
		}
		installPluginResources(rootDirectory);
		return rootDirectory;
	}

	private static File getLibFolder(File thisClassFile) {
		File libDir = thisClassFile.getParentFile();
		while (libDir != null && !(libDir.isDirectory() && libDir.getName().equals("lib"))) {
			libDir = libDir.getParentFile();
		}
		return libDir;
	}

	@NotNull
	private static File getClassFile() {
		String thisClassPath = null;
		try {
			thisClassPath = InitUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		}
		catch (URISyntaxException e) {
			LOGGER.info("Cannot get source location from protection domain.");
		}
		if (thisClassPath == null) {
			String absolutePath = new File(InitUtils.class.getProtectionDomain()
					.getCodeSource().getLocation().getFile()).getAbsolutePath();
			absolutePath = absolutePath.substring(absolutePath.lastIndexOf("file:") + 5);
			return new File(absolutePath);
		}
		else {
			return new File(thisClassPath);
		}
	}

	private static void installPluginResources(File targetFolder) {
		Plugin[] plugins = PluginManager.getInstance().getPlugins();
		for (Plugin p : plugins) {
			if (p.getPluginID().startsWith("KnowWE-Plugin-DenkbaresDialog")
					|| p.getPluginID().startsWith("KnowWE-Plugin-Dialog")
					|| p.getPluginID().startsWith("Mobile-Application")) {
				Resource[] resources = p.getResources();
				for (Resource r : resources) {
					String pathName = r.getPathName();
					if (!pathName.endsWith("/") && pathName.startsWith("webapp/")) {
						pathName = pathName.substring("webapp/".length());
						try {
							File file = new File(targetFolder, pathName);
							File parent = file.getParentFile();
							if (!parent.isDirectory()) {
								parent.mkdirs();
							}
							FileOutputStream out = new FileOutputStream(file);
							InputStream in = r.getInputStream();
							Streams.streamAndClose(in, out);
						}
						catch (IOException e) {
							throw new InstantiationError(
									"Cannot instantiate plugin "
											+ p
											+ ", the following error occurred while extracting its resources: "
											+ e.getMessage());
						}
					}
				}
			}
		}
	}

	private static void copyDir(File source, File target) throws IOException {
		target.mkdirs();
		File[] files = source.listFiles();
		if (files == null) throw new IOException("source folder does not exists: " + source);
		for (File file : files) {
			if (file.getName().contains(".svn")) continue;
			if (file.isDirectory()) {
				copyDir(file, new File(target, file.getName()));
			}
			else {
				FileOutputStream out = new FileOutputStream(new File(target, file.getName()));
				InputStream in = new FileInputStream(file);
				Streams.streamAndClose(in, out);
			}
		}
	}
}
