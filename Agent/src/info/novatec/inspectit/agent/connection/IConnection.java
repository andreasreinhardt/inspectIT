package info.novatec.inspectit.agent.connection;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.communication.DefaultData;

import java.net.ConnectException;
import java.util.List;

/**
 * The connection interface to implement different connection types, like RMI, Corba, etc.
 * 
 * @author Patrice Bouillet
 * 
 */
public interface IConnection {

	/**
	 * Establish the connection to the server.
	 * 
	 * @param host
	 *            The host / ip of the server.
	 * @param port
	 *            The port of the server.
	 * @exception ConnectException
	 *                Throws a ConnectException if there was a problem connecting to the repository.
	 */
	void connect(String host, int port) throws ConnectException;

	/**
	 * Disconnect from the server if possible.
	 */
	void disconnect();

	/**
	 * Returns if the connection is initialized and ready.
	 * 
	 * @return Is the connection initialized and ready to use.
	 */
	boolean isConnected();

	/**
	 * Send the measurements to the server for further processing.
	 * 
	 * @param dataObjects
	 *            The measurements to send.
	 * @throws ServerUnavailableException
	 *             If the sending wasn't successful in any way, a {@link ServerUnavailableException}
	 *             exception is thrown.
	 */
	void sendDataObjects(List<? extends DefaultData> dataObjects) throws ServerUnavailableException;

	/**
	 * Registers the agent with the CMR. The CMR will answer with the {@link AgentConfiguration}
	 * containing all necessary information for the agent initialization.
	 * 
	 * @param agentName
	 *            The self-defined name of the inspectIT Agent. Can be <code>null</code>.
	 * @param version
	 *            The version the agent is currently running with.
	 * @return {@link AgentConfiguration}.
	 * @throws ServerUnavailableException
	 *             If the sending wasn't successful in any way, a {@link ServerUnavailableException}
	 *             exception is thrown.
	 * @throws RegistrationException
	 *             This exception is thrown when a problem with the registration process appears.
	 */
	AgentConfiguration register(String agentName, String version) throws ServerUnavailableException, RegistrationException;

	/**
	 * Unregisters the platform in the CMR by sending the agent name and the network interfaces
	 * defined by the machine.
	 * 
	 * @param agentName
	 *            Name of the Agent.
	 * @throws ServerUnavailableException
	 *             If the action wasn't successful in any way, a {@link ServerUnavailableException}
	 *             exception is thrown.
	 * @throws RegistrationException
	 *             This exception is thrown when a problem with the un-registration process appears.
	 */
	void unregister(String agentName) throws ServerUnavailableException, RegistrationException;

	/**
	 * Analyzes and instruments the given byte code if necessary, returning the byte code to use on
	 * the Agent.
	 * 
	 * @param platformIdent
	 *            Id of the agent.
	 * @param hash
	 *            Class hash code.
	 * @param bytecode
	 *            ByteCode of the class.
	 * @return Instrumentation result containing modified byte code or <code>null</code> if nothing
	 *         was instrumented or any kind of exception occurred.
	 * @throws ServerUnavailableException
	 *             If the sending wasn't successful in any way, a {@link ServerUnavailableException}
	 *             exception is thrown.
	 */
	InstrumentationResult analyzeAndInstrument(long platformIdent, String hash, byte[] bytecode) throws ServerUnavailableException;
}
