package info.novatec.inspectit.agent.config;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.MethodSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.PlatformSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.RepositoryConfig;
import info.novatec.inspectit.agent.config.impl.StrategyConfig;
import info.novatec.inspectit.agent.sensor.exception.IExceptionSensor;
import info.novatec.inspectit.pattern.IMatchPattern;

import java.util.Collection;
import java.util.List;

/**
 * This storage is used by all configuration readers to store the information into.
 * 
 * @author Patrice Bouillet
 * @author Eduard Tudenhoefner
 * 
 */
public interface IConfigurationStorage {

	/**
	 * Sets the repository. Internally, a {@link RepositoryConfig} class is instantiated and filled
	 * with the proper arguments.
	 * 
	 * @param host
	 *            The host ip / name.
	 * @param port
	 *            The host port.
	 * @throws StorageException
	 *             Thrown if something with the host name or port is wrong.
	 */
	void setRepository(String host, int port) throws StorageException;

	/**
	 * Sets the {@link AgentConfiguration}.
	 * 
	 * @param agentConfiguration
	 *            Agent configuration received from the server.
	 */
	void setAgentConfiguration(AgentConfiguration agentConfiguration);

	/**
	 * Returns an instance of the {@link RepositoryConfig} class which is filled with the values by
	 * the {@link #setRepository(String, int)} method.
	 * 
	 * @return The {@link RepositoryConfig} instance.
	 */
	RepositoryConfig getRepositoryConfig();

	/**
	 * Sets the name of the Agent.
	 * 
	 * @param name
	 *            The name of the Agent to set.
	 * @throws StorageException
	 *             Thrown if something with the agent name is wrong.
	 */
	void setAgentName(String name) throws StorageException;

	/**
	 * Returns the name of the Agent.
	 * 
	 * @return The name of the Agent.
	 */
	String getAgentName();

	/**
	 * Returns a {@link StrategyConfig} instance containing the buffer strategy information.
	 * 
	 * @return An instance of {@link StrategyConfig}.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	StrategyConfig getBufferStrategyConfig() throws StorageException;

	/**
	 * Returns a {@link List} of {@link StrategyConfig} instances containing the sending strategy
	 * information.
	 * 
	 * @return A {@link List} of {@link StrategyConfig} instances.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	List<StrategyConfig> getSendingStrategyConfigs() throws StorageException;

	/**
	 * Returns a {@link List} of the {@link MethodSensorTypeConfig} classes.
	 * 
	 * @return A {@link List} of {@link MethodSensorTypeConfig} classes.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	List<MethodSensorTypeConfig> getMethodSensorTypes() throws StorageException;

	/**
	 * Returns a {@link List} of {@link MethodSensorTypeConfig} classes.
	 * 
	 * @return A {@link List} of {@link MethodSensorTypeConfig} classes.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	List<MethodSensorTypeConfig> getExceptionSensorTypes() throws StorageException;

	/**
	 * Returns a {@link List} of the {@link PlatformSensorTypeConfig} classes.
	 * 
	 * @return A {@link List} of {@link PlatformSensorTypeConfig} classes.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	List<PlatformSensorTypeConfig> getPlatformSensorTypes() throws StorageException;

	/**
	 * Returns whether the {@link IExceptionSensor} is activated.
	 * 
	 * @return Whether the {@link IExceptionSensor} is activated.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	boolean isExceptionSensorActivated() throws StorageException;

	/**
	 * Returns whether enhanced exception events are instrumented with try/catch.
	 * 
	 * @return Whether enhanced exception events are instrumented.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	boolean isEnhancedExceptionSensorActivated() throws StorageException;

	/**
	 * Returns the patterns that denote the classes that should be ignored.
	 * 
	 * @return Returns the patterns that denote the classes that should be ignored.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	Collection<IMatchPattern> getIgnoreClassesPatterns() throws StorageException;

	/**
	 * Returns time-stamp of the last modification of the agent configuration. This is a server
	 * related time-stamp.
	 * 
	 * @return Returns time-stamp of the last modification of the agent configuration.
	 * @throws StorageException
	 *             If agent configuration is not set.
	 */
	long getInstrumentationLastModified() throws StorageException;
}