package com.denkbares.semanticcore;

import com.denkbares.events.Event;
import com.denkbares.semanticcore.config.RepositoryConfig;

public class RepositoryConfigCreatedEvent implements Event {

	private final RepositoryConfig config;

	public RepositoryConfigCreatedEvent(RepositoryConfig config) {
		this.config = config;
	}

	public RepositoryConfig getConfig() {
		return config;
	}
}
