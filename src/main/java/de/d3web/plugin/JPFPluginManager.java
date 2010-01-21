/*
 * Copyright (C) 2009 denkbares GmbH
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.standard.StandardPluginLocation;

import de.d3web.plugin.util.PluginCollectionComparatorByPriority;
/**
 * An implementation of the PluginManager for the Java Plugin Framework (JPF)
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public class JPFPluginManager extends PluginManager {
	private org.java.plugin.PluginManager manager;

	private JPFPluginManager(File[] plugins) {
		manager = ObjectFactory.newInstance().createManager();
		List<PluginLocation> locations = new ArrayList<PluginLocation>();;
		for (int i = 0; i < plugins.length; i++) {
			try {
				
				PluginLocation location = StandardPluginLocation.create(plugins[i]);
				if (location!=null) {
					locations.add(location);
				} else {
					Logger.getLogger("PluginManager").warning(plugins[i]+" is no plugin");
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			manager.publishPlugins(locations.toArray(new PluginLocation[locations.size()]));
		} catch (JpfException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	/**
	 * This method initialises the JPFPluginmanager as PluginManager (which can be accessed via
	 * PluginManager.getInstance()) with the directory of the plugins as a String
	 * @param dir directory of the plugins
	 */
	public static void init(String dir) {
		File pluginsDir = new File(dir);
		File[] listFiles = pluginsDir.listFiles();
		if (listFiles!=null) instance = new JPFPluginManager(listFiles);
	}
	
	/**
	 * This method initialises the JPFPluginmanager as PluginManager (which can be accessed via
	 * PluginManager.getInstance()) with an array of plugins
	 * @param pathes to the plugins
	 */
	public static void init(File[] plugins) {
		instance = new JPFPluginManager(plugins);
	}


	@Override
	public Extension[] getExtensions(String extendetPointID, String extendetPluginID) {
		List<Extension> result = new ArrayList<Extension>();
		ExtensionPoint toolExtPoint = manager.getRegistry().getExtensionPoint(
				extendetPointID, extendetPluginID);
		Collection<org.java.plugin.registry.Extension> connectedExtensions = toolExtPoint
				.getConnectedExtensions();
		for (org.java.plugin.registry.Extension e : connectedExtensions) {
			result.add(new JPFExtension(e, manager));
		}
		Extension[] ret = result.toArray(new Extension[result.size()]);
		Arrays.sort(ret, new PluginCollectionComparatorByPriority());
		return ret;
	}

	@Override
	public Extension getExtension(String extendetPluginID,
			String extendetPointID, String extensionID) {
		Extension[] extensions = getExtensions(extendetPointID, extendetPluginID);
		for (Extension e: extensions) {
			if (e.getID().equals(extensionID)) return e;
		}
		return null;
	}
}
