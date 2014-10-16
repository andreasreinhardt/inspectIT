package info.novatec.inspectit.cmr.classcache.config;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.cmr.classcache.ClassCache;

/**
 * Agent cache entry saved by the service.
 * 
 * @author Ivan Senic
 * 
 */
public class AgentCacheEntry {

	/**
	 * Id of the agent.
	 */
	private final long id;

	/**
	 * Class cache for the agent. Can not be <code>null</code>.
	 */
	private final ClassCache classCache;

	/**
	 * Environment used for the agent.
	 */
	private Environment environment;

	/**
	 * Agent configuration used for the agent.
	 */
	private AgentConfiguration agentConfiguration;

	/**
	 * Default constructor.
	 * 
	 * @param id
	 *            Agent id.
	 * @param classCache
	 *            Class cache to use.
	 */
	public AgentCacheEntry(long id, ClassCache classCache) {
		this.id = id;
		this.classCache = classCache;
	}

	/**
	 * Gets {@link #id}.
	 * 
	 * @return {@link #id}
	 */
	public long getId() {
		return id;
	}

	/**
	 * Gets {@link #classCache}.
	 * 
	 * @return {@link #classCache}
	 */
	public ClassCache getClassCache() {
		return classCache;
	}

	/**
	 * Gets {@link #environment}.
	 * 
	 * @return {@link #environment}
	 */
	public Environment getEnvironment() {
		return environment;
	}

	/**
	 * Sets {@link #environment}.
	 * 
	 * @param environment
	 *            New value for {@link #environment}
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Gets {@link #agentConfiguration}.
	 * 
	 * @return {@link #agentConfiguration}
	 */
	public AgentConfiguration getAgentConfiguration() {
		return agentConfiguration;
	}

	/**
	 * Sets {@link #agentConfiguration}.
	 * 
	 * @param agentConfiguration
	 *            New value for {@link #agentConfiguration}
	 */
	public void setAgentConfiguration(AgentConfiguration agentConfiguration) {
		this.agentConfiguration = agentConfiguration;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Mapped only by id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Mapped only by id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AgentCacheEntry other = (AgentCacheEntry) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

}