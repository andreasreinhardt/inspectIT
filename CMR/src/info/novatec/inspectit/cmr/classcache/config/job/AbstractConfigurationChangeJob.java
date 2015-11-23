package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.config.AgentCacheEntry;
import info.novatec.inspectit.cmr.classcache.config.ClassCacheSearchNarrower;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationCreator;
import info.novatec.inspectit.cmr.classcache.config.InstrumentationPointsUtil;
import info.novatec.inspectit.spring.logger.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for all configuration change jobs. This class knows how to add or remove
 * instrumentation points on the given class cache, environment and agent configuration. Note that
 * {@link #environment}, {@link #classCache} and {@link #agentConfiguration} must be set using
 * setters before running the {@link #run()} method.
 * 
 * @author Ivan Senic
 * 
 */
public abstract class AbstractConfigurationChangeJob implements Runnable {

	/**
	 * Log for this class.
	 */
	@Log
	Logger log;

	/**
	 * {@link ClassCacheSearchNarrower}.
	 */
	@Autowired
	ClassCacheSearchNarrower classCacheSearchNarrower;

	/**
	 * {@link InstrumentationPointsUtil}.
	 */
	@Autowired
	InstrumentationPointsUtil instrumentationPointsUtil;

	/**
	 * {@link ConfigurationCreator}.
	 */
	@Autowired
	ConfigurationCreator configurationCreator;

	/**
	 * {@link AgentCacheEntry} containing all necessary information.
	 */
	private AgentCacheEntry agentCacheEntry;

	/**
	 * Creates new configuration based on the update {@link Environment} and set's the correct new
	 * settings to the agent cache entry.
	 * 
	 * @param update
	 *            Updated Environment.
	 * @return If update was successful.
	 */
	protected boolean updateConfiguration(Environment update) {
		AgentConfiguration agentConfiguration = configurationCreator.environmentToConfiguration(update, getAgentId());

		agentCacheEntry.setEnvironment(update);
		agentCacheEntry.setAgentConfiguration(agentConfiguration);

		return true;
	}

	/**
	 * Process the removed assignments. All instrumentation points affected by the any of these
	 * assignments are first completely removed. All classes that have any point removed will be
	 * re-analyzed against complete configuration in order to reset the possible points coming not
	 * from removed assignments.
	 * 
	 * @param methodSensorAssignments
	 *            Collection of removed {@link MethodSensorAssignment}s.
	 * @param exceptionSensorAssignments
	 *            Collection of removed {@link ExceptionSensorAssignment}s.
	 * @return if processing removed any instrumentation points
	 */
	protected boolean processRemovedAssignments(Collection<MethodSensorAssignment> methodSensorAssignments, Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		final Collection<ImmutableClassType> changedClassTypes = new ArrayList<>();

		// first process all method sensors for removal
		for (final MethodSensorAssignment methodSensorAssignment : methodSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByMethodSensorAssignment(getClassCache(), methodSensorAssignment);
			changedClassTypes.addAll(instrumentationPointsUtil.removeInstrumentationPoints(classTypes, getClassCache(), methodSensorAssignments, exceptionSensorAssignments));
		}

		// then process all exception sensors for removal
		for (final ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByExceptionSensorAssignment(getClassCache(), exceptionSensorAssignment);
			changedClassTypes.addAll(instrumentationPointsUtil.removeInstrumentationPoints(classTypes, getClassCache(), methodSensorAssignments, exceptionSensorAssignments));
		}

		// if no class was affected just return
		if (CollectionUtils.isNotEmpty(changedClassTypes)) {
			// if any class was affected re-check those classes against complete configuration
			// because we removed all instrumentation points
			instrumentationPointsUtil.addAllInstrumentationPoints(changedClassTypes, getClassCache(), getAgentConfiguration(), getEnvironment());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Process the added assignments. New instrumentation points will be added to all the classes in
	 * the class cache that fit to the given assignments.
	 * 
	 * @param methodSensorAssignments
	 *            Collection of removed {@link MethodSensorAssignment}s.
	 * @param exceptionSensorAssignments
	 *            Collection of removed {@link ExceptionSensorAssignment}s.
	 * @return if processing inserted any new instrumentation points
	 */
	protected boolean processAddedAssignments(Collection<MethodSensorAssignment> methodSensorAssignments, Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		boolean added = false;

		// go over all method sensor assignments
		for (final MethodSensorAssignment methodSensorAssignment : methodSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByMethodSensorAssignment(getClassCache(), methodSensorAssignment);
			Collection<? extends ImmutableClassType> instrumentedClassTypes = instrumentationPointsUtil.addInstrumentationPoints(classTypes, getClassCache(), getAgentConfiguration(),
					getEnvironment(), methodSensorAssignments, exceptionSensorAssignments);
			added |= CollectionUtils.isNotEmpty(instrumentedClassTypes);
		}

		// go over all exception sensor assignments
		for (final ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByExceptionSensorAssignment(getClassCache(), exceptionSensorAssignment);
			Collection<? extends ImmutableClassType> instrumentedClassTypes = instrumentationPointsUtil.addInstrumentationPoints(classTypes, getClassCache(), getAgentConfiguration(),
					getEnvironment(), methodSensorAssignments, exceptionSensorAssignments);
			added |= CollectionUtils.isNotEmpty(instrumentedClassTypes);
		}

		return added;
	}

	/**
	 * Updates the instrumentation points in the {@link AgentConfiguration} so they reflect the
	 * latest changes.
	 */
	protected void updateInstrumentationPointsInConfiguration() {
		Map<Collection<String>, InstrumentationResult> instrumentationResults = instrumentationPointsUtil.collectInstrumentationResultsWithHashes(getClassCache(), getEnvironment());
		getAgentConfiguration().setInitialInstrumentationResults(instrumentationResults);
		getAgentConfiguration().setClassCacheExistsOnCmr(true);
	}

	/**
	 * Clears the environment and configuration settings in the agent cache entry.
	 */
	protected void clearEnvironmentAndConfiguration() {
		agentCacheEntry.setAgentConfiguration(null);
		agentCacheEntry.setEnvironment(null);
	}

	/**
	 * @return Returns agent id based on the {@link AgentCacheEntry}.
	 */
	protected long getAgentId() {
		return agentCacheEntry.getId();
	}

	/**
	 * @return Returns class cache based on the {@link AgentCacheEntry}.
	 */
	protected ClassCache getClassCache() {
		return agentCacheEntry.getClassCache();
	}

	/**
	 * @return Returns environment based on the {@link AgentCacheEntry}.
	 */
	protected Environment getEnvironment() {
		return agentCacheEntry.getEnvironment();
	}

	/**
	 * @return Returns agent configuration based on the {@link AgentCacheEntry}.
	 */
	protected AgentConfiguration getAgentConfiguration() {
		return agentCacheEntry.getAgentConfiguration();
	}

	/**
	 * Sets {@link #agentCacheEntry}.
	 * 
	 * @param agentCacheEntry
	 *            New value for {@link #agentCacheEntry}
	 */
	public void setAgentCacheEntry(AgentCacheEntry agentCacheEntry) {
		this.agentCacheEntry = agentCacheEntry;
	}

	/**
	 * Gets {@link #instrumentationPointsUtil}.
	 * 
	 * @return {@link #instrumentationPointsUtil}
	 */
	public InstrumentationPointsUtil getInstrumentationPointsUtil() {
		return instrumentationPointsUtil;
	}

}
