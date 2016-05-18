package com.denkbares.semanticcore.reasoning;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.d3web.plugin.Extension;
import de.d3web.plugin.PluginManager;

/**
 * Util class for reasoning configs.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.05.16
 */
public class ReasoningConfigs {

	private static final Map<Class<? extends ReasoningConfig>, ReasoningConfig> cache = new HashMap<>();

	/**
	 * Get an instance for the given reasoning config class. It will be a singleton retrieved from the PluginManger.
	 * The return value cannot be null (except in case of a faulty plugin).
	 *
	 * @return an instance of the given reasoning config class.
	 */
	@NotNull
	public static ReasoningConfig get(Class<? extends ReasoningConfig> reasoningClass) {
		Objects.nonNull(reasoningClass);
		initExtensions();
		ReasoningConfig reasoningConfig = cache.get(reasoningClass);
		if (reasoningConfig == null) {
			throw new IllegalArgumentException("There seems to be no valid extension for the given config class '"
					+ reasoningClass.getName() + "'. This is most likely due to a faulty plugin.");
		}
		return reasoningConfig;
	}

	/**
	 * Get an instance for the given config name, if one exists (null else).
	 * It will be a singleton retrieved from the PluginManger.
	 *
	 * @return an instance of the given reasoning config name.
	 */
	@Nullable
	public static ReasoningConfig get(String name) {
		Objects.nonNull(null);
		initExtensions();
		return cache.values().stream().filter(reasoning -> reasoning.getName().equals(name)).findFirst().orElse(null);
	}

	public static void initExtensions() {
		if (cache.isEmpty()) {
			Extension[] reasoningExtensions = PluginManager.getInstance()
					.getExtensions("denkbares-SemanticCore-Plugin-ExtensionPoints",
							"ReasoningConfig");
			for (Extension reasoningExtension : reasoningExtensions) {
				ReasoningConfig singleton = (ReasoningConfig) reasoningExtension.getSingleton();
				cache.put(singleton.getClass(), singleton);
			}
			cache.put(RdfConfig.class, new RdfConfig());
		}
	}

	public static Collection<ReasoningConfig> values() {
		initExtensions();
		return Collections.unmodifiableCollection(cache.values());
	}
}
