/*
 * Copyright (C) 2016 denkbares GmbH. All rights reserved.
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

import com.denkbares.plugin.JPFPluginManager;
import com.denkbares.plugin.Plugin;
import com.denkbares.plugin.PluginManager;
import com.denkbares.plugin.Resource;
import com.denkbares.plugin.test.InitPluginManager;
import com.denkbares.utils.Log;
import com.denkbares.utils.Streams;

/**
 * Encapsulates application initialization, SWT-free code.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 28.08.2015
 */
public class InitUtils {

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
	public static File initPlugins(Collection<String> potentialAppNames) throws IOException {
		File rootDirectory = null;
		for (String appName : potentialAppNames) {
			File jarFile = new File(appName + ".jar");
			File appFile = new File(appName + ".app");

			// check bundled jar file
			Log.info("initialize d3web Mobile, check for jar app at: " + jarFile.getAbsolutePath());
			if (new File("./lib/").exists() && jarFile.exists()) {
				// started as standalone application
				JPFPluginManager.init("./lib/");
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
			Log.info("initialize d3web Mobile, check for osx app at: " + appFile.getAbsolutePath());
			if (appFile.exists()) {
				Log.info("Mac OSX app detected at: " + appFile.getAbsolutePath());
				// started as MacOS bundle,
				// check both d3webMobile and ServiceMate
				String dirMate = appFile + "/Contents/Resources/ServiceMate/WEB-INF/lib/";
				String dirDialog = appFile + "/Contents/Resources/Java";
				if (new File(dirDialog).isDirectory()) {
					Log.info("initialize for macos app: d3web Mobile");
					JPFPluginManager.init(dirDialog);
					rootDirectory = new File(appFile, "webapp");
					break;
				}
				else if (new File(dirMate).isDirectory()) {
					Log.info("initialize for mac osx app: Service Mate");
					JPFPluginManager.init(dirMate);
					rootDirectory = new File(appFile + "/Contents/Resources/ServiceMate");
					break;
				}
			}
		}

		// Find app in ServiceMate (OSX): Hack via protection domain
		try {
			final String thisClassPath = InitUtils.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI()
					.getPath();
			final File thisClass = new File(thisClassPath);

			if (thisClass.isFile() && thisClass.getParentFile().getName().equals("lib")) {
				Log.info("Initializing MobileApplication in Service Mate (OSX) at " + thisClass.getParentFile()
						.getAbsolutePath());
				JPFPluginManager.init(thisClass.getParent());
				rootDirectory = thisClass.getParentFile().getParentFile().getParentFile();
			}
			else {
				Log.info("Code source of InitUtils inconclusive for finding MobileApplication: " + thisClassPath);
			}
		}
		catch (URISyntaxException e) {
			Log.info("Cannot get source location from protection domain.");
		}

		// if still not initialized: try the windows structure
		if (rootDirectory == null) {
			File winMate = new File("ServiceMate/WEB-INF/lib");
			Log.info("initialize d3web Mobile, check for Windows app at: " + winMate.getAbsolutePath());
			if (winMate.isDirectory()) {
				Log.info("Windows application found at: " + winMate.getAbsolutePath());

				JPFPluginManager.init(winMate.getAbsolutePath());
				rootDirectory = winMate.getParentFile().getParentFile();
			}
		}

		// if still not initialized: use "target" folder, running in IDE debugger
		if (rootDirectory == null && new File("target").exists()) {
			Log.info("start from debugger detected: target folder used");
			InitPluginManager.init();
			rootDirectory = new File("target/webapp/");
			// copy files from src/main/resources to
			copyDir(new File("src/main/resources/webapp"), new File("target/webapp/"));
		}

		// if still not initialized: use dependency file, running in IDE debugger's war server
		File classpathFile = new File("WEB-INF/classes/output.txt");
		if (rootDirectory == null && classpathFile.exists()) {
			Log.info("start from debugger detected: dependencies file used");
			InitPluginManager.init(classpathFile);
			rootDirectory = new File(".");
		}

		// install on root directory
		if (rootDirectory == null) {
			throw new IOException("no valid application root folder was found. " +
					"Current working directory: " + new File(".").getAbsolutePath());
		}
		installPluginResources(rootDirectory);
		return rootDirectory;
	}

	private static void installPluginResources(File targetFolder) {
		Plugin[] plugins = PluginManager.getInstance().getPlugins();
		for (Plugin p : plugins) {
			if (p.getPluginID().startsWith("KnowWE-Plugin-DenkbaresDialog")
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
