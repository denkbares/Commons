package com.denkbares.semanticcore.reasoning;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
	 * Get an instance of the given reasoning config class. It will be a singleton retrieved from the PluginManger.
	 *
	 * @return an instance of the given reasoning config class.
	 */
	public static ReasoningConfig get(Class<? extends ReasoningConfig> reasoningClass) {
		initExtensions();
		return cache.get(reasoningClass);
	}

	/**
	 * Get an instance of the given config name. It will be a singleton retrieved from the PluginManger.
	 *
	 * @return an instance of the given reasoning config name.
	 */
	public static ReasoningConfig get(String name) {
		initExtensions();
		return cache.values().stream().filter(reasoning -> reasoning.getName().equals(name)).findFirst().orElse(null);
	}

	public static void initExtensions() {
		if (cache.isEmpty()) {
			Extension[] reasoningExtensions = PluginManager.getInstance()
					.getExtensions("denkbares-SemanticCore-Plugin-ExtensionPoints",
							"SemanticCoreReasoning");
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
