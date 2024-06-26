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

package com.denkbares.plugin;

/**
 * This interface describes a plugin loaded by the {@link PluginManager}.
 * 
 * @author volker_belli
 * 
 */
public interface Plugin {

	/**
	 * Returns the unique id of this plugin.
	 * 
	 * @return the unique plugin id
	 */
	String getPluginID();

	/**
	 * Returns a list of all Resources, available in this this plugin. The
	 * resources are those files, exported in the plugin declaration as non-code
	 * resources.
	 * 
	 * @return the plugin resources
	 */
	Resource[] getResources();

	/**
	 * Returns the {@link ClassLoader} of the Plugin.
	 * 
	 * The {@link ClassLoader} should be used to load classes from this plugin.
	 * Caution: All classes of plugins are not part of the application's
	 * classpath.
	 * 
	 * @created 20.05.2011
	 * @return {@link ClassLoader}
	 */
	ClassLoader getClassLoader();
}
