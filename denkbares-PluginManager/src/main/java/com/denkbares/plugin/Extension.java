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

import java.util.List;

/**
 * An interface for Extensions
 *
 * @author Markus Friedrich (denkbares GmbH)
 */
public interface Extension {

	/**
	 * Creates a new instance of the Class represented by this extension
	 *
	 * @return a new instance
	 */
	Object getNewInstance();

	/**
	 * Returns the value of any parameter
	 *
	 * @param parameter the name of the parameter, which should be returned
	 * @return the value of the parameter
	 */
	String getParameter(String parameter);

	/**
	 * Returns the values of any parameter
	 *
	 * @param parameter the name of the parameter, which should be returned
	 * @return the values of the parameter
	 */
	List<String> getParameters(String parameter);

	/**
	 * Returns an Instance of the Class represented by this extension Each time this method is called, it returns the
	 * same instance. At the first call, an instance is created.
	 *
	 * @return a Singleton of the Class
	 */
	Object getSingleton();

	/**
	 * Returns the class of the instances that are denoted by the "class" attribute of the extension's xml definition.
	 * These are the class that will be instantiated by {@link #getSingleton()} or {@link #getNewInstance()} method.
	 *
	 * @return the class of the instances
	 */
	Class getInstanceClass();

	/**
	 * Each Extension has a name, which can be accessed by this method
	 *
	 * @return the name of the Extension
	 */
	String getName();

	/**
	 * Each Extension has a version, which can be accessed by this method
	 *
	 * @return the version of the Extension
	 */
	String getVersion();

	/**
	 * Each Extension has a description, which can be accessed by this method
	 *
	 * @return the description of the Extension
	 */
	String getDescription();

	/**
	 * Each Extension has a priority, which can be accessed by this method Higher priorities have lower values
	 *
	 * @return the priority of the Extension
	 */
	double getPriority();

	/**
	 * Each Extension has an ID, which can be accessed by this method
	 *
	 * @return the ID of the Extension
	 */
	String getID();

	/**
	 * The ID of the ExtensionPoint extended by this extension can be accessed by this method
	 *
	 * @return ID of the ExtensionPoint
	 * @deprecated because of typo, use {@link #getExtendedPointID()} instead
	 */
	@Deprecated
	String getExtendetPointID();

	/**
	 * The ID of the ExtensionPoint extended by this extension can be accessed by this method
	 *
	 * @return ID of the ExtensionPoint
	 */
	String getExtendedPointID();

	/**
	 * The ID of the Plugin, containing the ExtensionPoint this extension extends can be accessed by this method
	 *
	 * @return ID of the extended plugin
	 */
	String getExtendedPluginID();

	/**
	 * Returns the id of the plugin, short for getPlugin().getID().
	 *
	 * @return the id of the plugin
	 */
	String getPluginID();

	/**
	 * Returns the Plugin this Extension is part of
	 *
	 * @return {@link Plugin} this Extension is part of
	 * @created 20.05.2011
	 */
	Plugin getPlugin();
}
