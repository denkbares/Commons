package com.denkbares.semanticcore.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.events.EventListener;
import com.denkbares.events.EventManager;
import com.denkbares.plugin.Extension;
import com.denkbares.plugin.PluginManager;
import com.denkbares.utils.Log;

/**
 * Util class for repository configs.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class RepositoryConfigs {

	private static final Map<Class<? extends RepositoryConfig>, RepositoryConfig> cache = new LinkedHashMap<>();
	public static final String PLUGIN_ID = "denkbares-SemanticCore-Plugin-ExtensionPoints";
	public static final String POINT_ID_CONFIG = "RepositoryConfig";
	public static final String POINT_ID_EVENT_LISTENER = "EventListener";

	/**
	 * Get an instance for the given repository config class. It will be a singleton retrieved from the PluginManger.
	 * The return value cannot be null (except in case of a faulty plugin).
	 *
	 * @return an instance of the given repository config class.
	 */
	public static @NotNull RepositoryConfig get(Class<? extends RepositoryConfig> reasoningClass) {
		Objects.nonNull(reasoningClass);
		initExtensions();
		RepositoryConfig repositoryConfig = cache.get(reasoningClass);
		if (repositoryConfig == null) {
			throw new IllegalArgumentException("There seems to be no valid extension for the given config class '"
					+ reasoningClass.getName() + "'. This is most likely due to a faulty plugin.");
		}
		return repositoryConfig;
	}

	/**
	 * Finds an instance for the given config name. If no config with the exact name is given, alternatives will be
	 * checked, e.g. non-optimized or other vendor. If no suitable alternative is found, null will be returned.
	 *
	 * @return an repository suitable for the given repository name
	 */
	@Nullable
	public static RepositoryConfig find(String name) {
		@Nullable RepositoryConfig repositoryConfig = get(name);
		if (repositoryConfig == null && name.endsWith("_OPTIMIZED")) {
			// check if unoptimized config is available
			repositoryConfig = get(name.replaceAll("_OPTIMIZED$", ""));
		}
		if (repositoryConfig == null && name.contains("_")) {
			// trim last suffix (probably vendor), see if available
			repositoryConfig = get(name.replaceAll("_[^_]$", ""));
		}
		if (repositoryConfig == null) {
			// see if config with given prefix is available
			repositoryConfig = cache.values()
					.stream()
					.filter(reasoning -> reasoning.getName().startsWith(name))
					.findFirst()
					.orElse(null);
		}
		return repositoryConfig;
	}

	/**
	 * Get an instance for the given config name, if one exists (null else).
	 * It will be a singleton retrieved from the PluginManger.
	 *
	 * @return an instance of the given repository config name.
	 */
	@Nullable
	public static RepositoryConfig get(String name) {
		initExtensions();
		return cache.values().stream().filter(reasoning -> reasoning.getName().equals(name)).findFirst().orElse(null);
	}

	public static void initExtensions() {
		if (cache.isEmpty()) {
			initEventListener();
			initConfigs();
		}
	}

	private static void initConfigs() {
		Extension[] reasoningExtensions = PluginManager.getInstance()
				.getExtensions(PLUGIN_ID, POINT_ID_CONFIG);
		for (Extension reasoningExtension : reasoningExtensions) {
			try {
				RepositoryConfig singleton = (RepositoryConfig) reasoningExtension.getSingleton();
				cache.put(singleton.getClass(), singleton);
			}
			catch (NoClassDefFoundError e) {
				Log.severe("Extension " + reasoningExtension.getID() + " was listed as a dependency, but not available in the class path. Skipping.");
			}
		}
	}

	private static void initEventListener() {
		Extension[] extensions = PluginManager.getInstance().getExtensions(
				PLUGIN_ID, POINT_ID_EVENT_LISTENER);
		for (Extension extension : extensions) {
			Object listener = extension.getSingleton();
			if (listener instanceof EventListener) {
				synchronized (EventManager.getInstance()) {
					EventManager.getInstance()
							.registerListener(((EventListener) listener));
				}
			}
		}
	}

	public static Collection<RepositoryConfig> values() {
		initExtensions();
		return Collections.unmodifiableCollection(cache.values());
	}
}
