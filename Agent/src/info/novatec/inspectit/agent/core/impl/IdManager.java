package info.novatec.inspectit.agent.core.impl;

import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.RepositoryConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.RegistrationException;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IIdManager;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.spring.logger.Log;
import info.novatec.inspectit.version.VersionService;

import java.net.ConnectException;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * New id manager, way simpler.
 * 
 * @author Ivan Senic
 * 
 */
@Component("idManager")
public class IdManager implements IIdManager, InitializingBean {

	/**
	 * The logger of the class.
	 */
	@Log
	Logger log;

	/**
	 * The configuration storage used to access some information which needs to be registered at the
	 * server.
	 */
	@Autowired
	IConfigurationStorage configurationStorage;

	/**
	 * The versioning service.
	 */
	@Autowired
	private final VersionService versionService;

	/**
	 * The connection to the Central Measurement Repository.
	 */
	@Autowired
	IConnection connection;

	/**
	 * The id of this platform.
	 */
	private long platformId = -1;

	/**
	 * If set to <code>true</code>, the connection to server created an exception.
	 */
	private volatile boolean serverErrorOccured = false;

	/**
	 * {@inheritDoc}
	 */
	public boolean isPlatformRegistered() {
		return -1 != platformId;
	}

	/**
	 * {@inheritDoc}
	 * <P>
	 * For now just return the id.
	 */
	public long getPlatformId() throws IdNotAvailableException {
		if (-1 == platformId) {
			throw new IdNotAvailableException("No ID available in the moment.");
		}
		return platformId;
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregisterPlatform() {
		if (connection.isConnected() && isPlatformRegistered()) {
			try {
				connection.unregister(configurationStorage.getAgentName());
				platformId = -1;
			} catch (Throwable e) { // NOPMD
				log.warn("Could not un-register the platform.");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Just return given method id, there's no difference any more.
	 */
	public long getRegisteredMethodId(long methodId) throws IdNotAvailableException {
		// TODO Remove this later
		return methodId;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Just return given sensor id, there's no difference any more.
	 */
	public long getRegisteredSensorTypeId(long sensorTypeId) throws IdNotAvailableException {
		// TODO Remove this later
		return sensorTypeId;
	}

		@Override
	/**
	 * Execute the registration if needed.
	 */
	private void doRegistration() {
		try {
			// not connected? -> connect
			if (!connection.isConnected()) {
				connect();
			}

			// register the agent
			if (!isPlatformRegistered()) {
				register();
			}

			// clear the flag
			serverErrorOccured = false;
		} catch (ServerUnavailableException serverUnavailableException) {
			if (!serverErrorOccured) {
				log.error("Server unavailable while trying to register something at the server.");
			}
			serverErrorOccured = true;
			if (log.isTraceEnabled()) {
				log.trace("doRegistration()", serverUnavailableException);
			}
		} catch (RegistrationException registrationException) {
			if (!serverErrorOccured) {
				log.error("Registration exception occurred while trying to register something at the server.");
			}
			serverErrorOccured = true;
			if (log.isTraceEnabled()) {
				log.trace("doRegistration()", registrationException);
			}
		} catch (ConnectException connectException) {
			if (!serverErrorOccured) {
				log.error("Connection to the server failed.");
			}
			serverErrorOccured = true;
			if (log.isTraceEnabled()) {
				log.trace("doRegistration()", connectException);
			}
		}
	}

	/**
	 * Establish the connection to the server.
	 * 
	 * @exception ConnectException
	 *                Throws a ConnectException if there was a problem connecting to the repository.
	 */
	private void connect() throws ConnectException {
		RepositoryConfig repositoryConfig = configurationStorage.getRepositoryConfig();
		connection.connect(repositoryConfig.getHost(), repositoryConfig.getPort());
	}

	/**
	 * Registers the platform at the CMR.
	 * 
	 * @throws ServerUnavailableException
	 *             If the sending wasn't successful in any way, a {@link ServerUnavailableException}
	 *             exception is thrown.
	 * @throws RegistrationException
	 *             This exception is thrown when a problem with the registration process appears.
	 */
	private void register() throws ServerUnavailableException, RegistrationException {
		AgentConfiguration agentConfiguration = connection.register(configurationStorage.getAgentName(), getVersion());
		configurationStorage.setAgentConfiguration(agentConfiguration);
		platformId = agentConfiguration.getPlatformId();

		if (log.isDebugEnabled()) {
			log.debug("Received platform ID: " + platformId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterPropertiesSet() throws Exception {
		doRegistration();
	}

}
