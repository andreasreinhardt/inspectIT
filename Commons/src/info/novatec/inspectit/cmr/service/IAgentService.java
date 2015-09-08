package info.novatec.inspectit.cmr.service;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.exception.BusinessException;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface for agent communication with the CMR.
 * 
 * @author Ivan Senic
 * 
 */
@ServiceInterface(exporter = ServiceExporterType.RMI, serviceId = 3)
public interface IAgentService {

	/**
	 * Registers the agent with the CMR. The CMR will answer with the {@link AgentConfiguration}
	 * containing all necessary information for the agent initialization.
	 * 
	 * @param definedIPs
	 *            The list of all network interfaces.
	 * @param agentName
	 *            The self-defined name of the inspectIT Agent. Can be <code>null</code>.
	 * @param version
	 *            The version the agent is currently running with.
	 * @return {@link AgentConfiguration}.
	 * @throws ServiceException
	 *             TODO remove these exceptions
	 * @throws RemoteException
	 *             TODO
	 */
	AgentConfiguration register(List<String> definedIPs, String agentName, String version) throws BusinessException;

	/**
	 * Unregisters the platform in the CMR by sending the agent name and the network interfaces
	 * defined by the machine.
	 * 
	 * @param definedIPs
	 *            The list of all network interfaces.
	 * @param agentName
	 *            Name of the Agent. * @throws ServiceException TODO remove these exceptions
	 * @throws RemoteException
	 *             TODO
	 */
	void unregister(List<String> definedIPs, String agentName) throws BusinessException;

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
	 *         was instrumented.
	 * @throws BusinessException
	 */
	InstrumentationResult analyzeAndInstrument(long platformIdent, String hash, byte[] bytecode) throws BusinessException;

}
