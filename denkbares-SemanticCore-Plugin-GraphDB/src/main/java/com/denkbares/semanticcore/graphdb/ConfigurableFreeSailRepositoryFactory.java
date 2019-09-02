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

package com.denkbares.semanticcore.graphdb;

import com.ontotext.trree.config.OWLIMSailConfig;
import com.ontotext.trree.free.GraphDBRepositoryConfig;
import com.ontotext.trree.monitorRepository.MonitorRepositoryConfig;
import com.ontotext.trree.monitorRepository.MonitorRepositoryFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.config.SailRegistry;

/**
 * RepositoryFactory creating a normal graphdb:FreeSailRepository, while also allowing to deactivate some unwanted
 * GraphDB plugins. Could also be used to perform additional configurations, that are not available otherwise.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 03.09.2019
 */
public class ConfigurableFreeSailRepositoryFactory extends MonitorRepositoryFactory {

	public static final String REPOSITORY_TYPE = "graphdb:ConfigurableFreeSailRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new MonitorRepositoryConfig(REPOSITORY_TYPE);
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		MonitorRepositoryConfig monitorConfig = (MonitorRepositoryConfig) config;
		overridePlugins(monitorConfig);
		try {
			Sail sail = createSail(monitorConfig.getSailImplConfig());
			return new SailRepository(sail);
		}
		catch (SailConfigException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}

	/**
	 * Here we override the parameter allowing us to deactivate plugins. Some of the plugins we just don't need, but in
	 * case of the mongodb plugin, it messes up dependencies of certain other tools that already use a mongodb.
	 * <p>
	 * Unfortunately, there doesn't seem to be any other way than doing the overriding in this factory. You would
	 * think, that you can set the plugin parameter using the grapdh configuration file des-defaults.ttl, but from that
	 * file, only certain parameters are used.
	 *
	 * @since graphb-free-runtime:8.11
	 */
	protected void overridePlugins(MonitorRepositoryConfig monitorConfig) {
		SailImplConfig sailImplConfig = monitorConfig.getSailImplConfig();
		IRI disablePluginsIri = SimpleValueFactory.getInstance()
				.createIRI("http://www.ontotext.com/trree/owlim#disable-plugins");
		((OWLIMSailConfig) sailImplConfig).getConfigParams()
				.put(disablePluginsIri, "mongodb,notifications,notifications-logger,rdfrank,expose-entity");
	}

	private Sail createSail(SailImplConfig config) throws RepositoryConfigException, SailConfigException {
		SailFactory sailFactory = SailRegistry.getInstance().get(config.getType()).orElse(null);
		if (sailFactory != null) {
			return sailFactory.getSail(config);
		}
		else {
			throw new RepositoryConfigException("Unsupported Sail type: " + config.getType());
		}
	}

}
