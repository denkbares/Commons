package com.denkbares.semanticcore.jena;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.config.MemoryStoreConfig;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 25.05.16
 */
public class JenaSailFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "jena:DefaultStore";

	/**
	 * Returns the Sail's type: <tt>jena:DefaultStore</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return null;
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		Model model = ModelFactory.createDefaultModel();
		MemoryStore memoryStore = new MemoryStore();

		if (config instanceof MemoryStoreConfig) {
			MemoryStoreConfig memConfig = (MemoryStoreConfig) config;

			memoryStore.setPersist(memConfig.getPersist());
			memoryStore.setSyncDelay(memConfig.getSyncDelay());

			if (memConfig.getIterationCacheSyncThreshold() > 0) {
				memoryStore.setIterationCacheSyncThreshold(memConfig.getIterationCacheSyncThreshold());
			}
		}

		return memoryStore;
	}
}

