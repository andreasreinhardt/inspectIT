package info.novatec.inspectit.cmr.classcache.config;

import info.novatec.inspectit.ci.AgentMapping;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.ci.exclude.ExcludeRule;
import info.novatec.inspectit.cmr.ci.ConfigurationInterfaceManager;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.exception.enumeration.ConfigurationInterfaceErrorCodeEnum;
import info.novatec.inspectit.pattern.IMatchPattern;
import info.novatec.inspectit.pattern.PatternFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Component that can resolve the different CI configuration questions.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class ConfigurationResolver {

	/**
	 * {@link ConfigurationInterfaceManager}.
	 */
	@Autowired
	ConfigurationInterfaceManager configurationInterfaceManager;

	/**
	 * Returns all {@link MethodSensorAssignment}s contained in all profiles for the given
	 * environment.
	 * 
	 * @param environment
	 *            {@link Environment} to get assignments for.
	 * @return Returns all {@link MethodSensorAssignment}s contained in all profiles for the given
	 *         environment.
	 */
	public Collection<MethodSensorAssignment> getAllMethodSensorAssignments(Environment environment) {
		if (null == environment) {
			return Collections.emptyList();
		}

		Collection<MethodSensorAssignment> assignments = new ArrayList<>();
		for (String profileId : environment.getProfileIds()) {
			try {
				Profile profile = configurationInterfaceManager.getProfile(profileId);
				assignments.addAll(profile.getMethodSensorAssignments());
			} catch (Exception e) {
				continue;
			}
		}
		return assignments;
	}

	/**
	 * Returns all {@link ExceptionSensorAssignment}s contained in all profiles for the given
	 * environment.
	 * 
	 * @param environment
	 *            {@link Environment} to get assignments for.
	 * @return Returns all {@link ExceptionSensorAssignment}s contained in all profiles for the
	 *         given environment.
	 */
	public Collection<ExceptionSensorAssignment> getAllExceptionSensorAssignments(Environment environment) {
		if (null == environment) {
			return Collections.emptyList();
		}

		Collection<ExceptionSensorAssignment> assignments = new ArrayList<>();
		for (String profileId : environment.getProfileIds()) {
			try {
				Profile profile = configurationInterfaceManager.getProfile(profileId);
				assignments.addAll(profile.getExceptionSensorAssignments());
			} catch (Exception e) {
				continue;
			}
		}
		return assignments;
	}

	/**
	 * Returns all {@link ExcludeRule}s contained in all profiles for the given environment.
	 * 
	 * @param environment
	 *            {@link Environment} to get rules for.
	 * @return Returns all {@link ExcludeRule}s contained in all profiles for the given environment.
	 */
	public Collection<ExcludeRule> getAllExcludeRules(Environment environment) {
		if (null == environment) {
			return Collections.emptyList();
		}

		Collection<ExcludeRule> assignments = new ArrayList<>();
		for (String profileId : environment.getProfileIds()) {
			try {
				Profile profile = configurationInterfaceManager.getProfile(profileId);
				assignments.addAll(profile.getExcludeRules());
			} catch (Exception e) {
				continue;
			}
		}
		return assignments;
	}

	/**
	 * Tries to locate one {@link Environment} for the given agent name and IPs. If only one
	 * {@link Environment} fits the agent by current mappings this one will be returned. Otherwise
	 * an exception will be raised.
	 * 
	 * @param definedIPs
	 *            The list of all network interfaces.
	 * @param agentName
	 *            The self-defined name of the inspectIT Agent. Can be <code>null</code>.
	 * @return {@link Environment}.
	 * @throws BusinessException
	 *             Throws {@link Exception} if there is no matching environment for the agent or if
	 *             there is more than one valid environment for the agent.
	 */
	public Environment getEnvironmentForAgent(List<String> definedIPs, String agentName) throws BusinessException {
		List<AgentMapping> mappings = new ArrayList<>(configurationInterfaceManager.getAgentMappings().getMappings());

		for (Iterator<AgentMapping> it = mappings.iterator(); it.hasNext();) {
			AgentMapping agentMapping = it.next();
			if (!matches(agentMapping, definedIPs, agentName)) {
				it.remove();
			}
		}

		if (CollectionUtils.isEmpty(mappings) || mappings.size() > 1) {
			throw new BusinessException("Determing an environment to use for the agent with name '" + agentName + "' and IP adress(es): " + definedIPs,
					ConfigurationInterfaceErrorCodeEnum.ENVIRONMENT_FOR_AGENT_NOT_FOUND);
		} else {
			String environmentId = mappings.get(0).getEnvironmentId();
			return configurationInterfaceManager.getEnvironment(environmentId);
		}
	}

	/**
	 * Checks if the specified {@link AgentMapping} is matching the agent name and IPs.
	 * 
	 * @param agentMapping
	 *            {@link AgentMapping} to check.
	 * @param definedIPs
	 *            The list of all network interfaces.
	 * @param agentName
	 *            The self-defined name of the inspectIT Agent.
	 * @return <code>true</code> if the name and any of the IP addresses match the defined
	 *         {@link AgentMapping}
	 */
	private boolean matches(AgentMapping agentMapping, List<String> definedIPs, String agentName) {
		// first match name
		String definedName = agentMapping.getAgentName();
		IMatchPattern namePattern = PatternFactory.getPattern(definedName);
		if (!namePattern.match(agentName)) {
			return false;
		}

		String definedIps = agentMapping.getIpAddress();
		// then all IPs, if any matches return true
		IMatchPattern ipPattern = PatternFactory.getPattern(definedIps);
		for (String ip : definedIPs) {
			if (ipPattern.match(ip)) {
				return true;
			}
		}
		return false;
	}
}
