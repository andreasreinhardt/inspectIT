package info.novatec.inspectit.cmr.service;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableType;
import info.novatec.inspectit.classcache.Type;
import info.novatec.inspectit.cmr.ci.IConfigurationInterfaceChangeListener;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.ClassCacheModificationException;
import info.novatec.inspectit.cmr.classcache.config.AgentCacheEntry;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationCreator;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationResolver;
import info.novatec.inspectit.cmr.classcache.config.InstrumentationPointsUtil;
import info.novatec.inspectit.cmr.classcache.config.job.EnvironmentUpdateJob;
import info.novatec.inspectit.cmr.classcache.config.job.ProfileUpdateJob;
import info.novatec.inspectit.cmr.classcache.config.job.RefreshInstrumentationTimestampsJob;
import info.novatec.inspectit.cmr.spring.aop.MethodLog;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.exception.enumeration.AgentManagementErrorCodeEnum;
import info.novatec.inspectit.spring.logger.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service for agent communication with the CMR.
 * 
 * @author Ivan Senic
 * 
 */
@Service
public class AgentService implements IAgentService, IConfigurationInterfaceChangeListener {

	/**
	 * Logger for the class.
	 */
	@Log
	Logger log;

	/**
	 * Factory for creating new class caches.
	 */
	@Autowired
	private ObjectFactory<ClassCache> classCacheFactory;

	/**
	 * Factory for creating new {@link ProfileUpdatedJob}.
	 */
	@Autowired
	private ObjectFactory<ProfileUpdateJob> profileUpdateJobFactory;

	/**
	 * Factory for creating new {@link EnvironmentUpdateJob}.
	 */
	@Autowired
	private ObjectFactory<EnvironmentUpdateJob> environmentUpdateJobFactory;

	/**
	 * Factory for creating new {@link EnvironmentUpdateJob}.
	 */
	@Autowired
	private ObjectFactory<RefreshInstrumentationTimestampsJob> refreshInstrumentationTimestampsJobFactory;

	/**
	 * Registration service.
	 */
	@Autowired
	private IRegistrationService registrationService;

	/**
	 * Configuration creator for the agents.
	 */
	@Autowired
	private ConfigurationCreator configurationCreator;

	/**
	 * Configuration resolver.
	 */
	@Autowired
	private ConfigurationResolver configurationResolver;

	/**
	 * Instrumentation points util.
	 */
	@Autowired
	private InstrumentationPointsUtil instrumentationPointsUtil;

	/**
	 * Executor for dealing with configuration updates.
	 */
	@Autowired
	@Qualifier("agentServiceExecutorService")
	private ExecutorService executor;

	/**
	 * Cache for the agents and it's used class cache, environments and configurations.
	 */
	private ConcurrentHashMap<Long, AgentCacheEntry> agentCacheMap = new ConcurrentHashMap<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	@MethodLog
	public AgentConfiguration register(List<String> definedIPs, String agentName, String version) throws BusinessException {
		// load environment for the agent
		final Environment environment = configurationResolver.getEnvironmentForAgent(definedIPs, agentName);

		// if environment load is success register agent
		long id = registrationService.registerPlatformIdent(definedIPs, agentName, version);

		// get or create the agent cache entry
		AgentCacheEntry agentCacheEntry = getAgentCacheEntry(id);

		// check if this agent was already registered and we have environment
		Environment cachedEnvironment = agentCacheEntry.getEnvironment();
		ClassCache classCache = agentCacheEntry.getClassCache();

		// if we have same environment, just return cached configuration
		if (Objects.equals(environment, cachedEnvironment)) {
			AgentConfiguration agentConfiguration = agentCacheEntry.getAgentConfiguration();

			// recollect the points
			// TODO skip this if the changes in the CI already set new points in the agent
			// configuration :)
			if (null != agentConfiguration) {
				Map<Collection<String>, InstrumentationResult> initInstrumentationResults = instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment);
				agentConfiguration.setInitialInstrumentationResults(initInstrumentationResults);
				agentConfiguration.setClassCacheExistsOnCmr(true);
				return agentConfiguration;
			}
		} else if (null != cachedEnvironment) {
			// otherwise if we have different environment cached, then clear all points
			// TODO same skip this if the changes in the CI already set new points in the agent
			// configuration :)
			instrumentationPointsUtil.removeAllInstrumentationPoints(classCache);
		}

		// else kick the configuration creator
		AgentConfiguration agentConfiguration = configurationCreator.environmentToConfiguration(environment, id);
		boolean cacheExists = null != cachedEnvironment;
		agentConfiguration.setClassCacheExistsOnCmr(cacheExists);

		// collect instrumentation points
		Collection<ClassType> instrumentedTypes = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);
		Map<Collection<String>, InstrumentationResult> initialInstrumentationResults = instrumentationPointsUtil.collectInstrumentationResultsWithHashes(instrumentedTypes, classCache, environment);
		agentConfiguration.setInitialInstrumentationResults(initialInstrumentationResults);

		// save results to cache entry
		agentCacheEntry.setEnvironment(environment);
		agentCacheEntry.setAgentConfiguration(agentConfiguration);

		// return configuration
		return agentConfiguration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@MethodLog
	public void unregister(List<String> definedIPs, String agentName) throws BusinessException {
		registrationService.unregisterPlatformIdent(definedIPs, agentName);
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	@MethodLog
	public InstrumentationResult analyzeAndInstrument(long platformIdent, String hash, Type sentType) throws BusinessException {
		AgentCacheEntry agentCacheEntry = agentCacheMap.get(Long.valueOf(platformIdent));
		if (null == agentCacheEntry) {
			throw new BusinessException("Instrumenting class with hash '" + hash + "' for the agent with id=" + platformIdent, AgentManagementErrorCodeEnum.AGENT_DOES_NOT_EXIST);
		}

		ClassCache classCache = agentCacheEntry.getClassCache();
		ImmutableType type = classCache.getLookupService().findByHash(hash);
		// if does not exists, parse, merge & configure instrumentation points
		if (null == type) {
			try {
				// TODO how to use Events
				classCache.getModificationService().merge(sentType);

				// get real object after merging
				type = classCache.getLookupService().findByHash(hash);
			} catch (ClassCacheModificationException e) {
				log.error("Type can not be analyzed due to the exception during merging.", e);
				return null;
			}
		}

		// no need to do anything with types that are not classes
		// just return
		if (!type.isClass()) {
			return null;
		}

		final ImmutableClassType classType = type.castToClass();
		final Environment environment = agentCacheEntry.getEnvironment();

		// then run configuration if it's a class and does no have instrumentation points
		// this is a contract with the agent, agent will not send classes that should not have
		// instrumentation points
		// and for us is to overcome situation when all points are removed from class cache, and to
		// kick in new analysis
		// TODO by current contract we should never get the same class twice, so always run the
		// instrumentation and collect, easy :)
		if (!classType.hasInstrumentationPoints()) {
			// kick in configuration
			final AgentConfiguration agentConfiguration = agentCacheEntry.getAgentConfiguration();
			instrumentationPointsUtil.addAllInstrumentationPoints(Collections.singleton(classType), classCache, agentConfiguration, environment);
		} else {
			// refresh the time-stamps of the instrumentation points in the DB
			RefreshInstrumentationTimestampsJob job = refreshInstrumentationTimestampsJobFactory.getObject();
			job.setClassType(classType);
			executor.execute(job);
		}

		Collection<InstrumentationResult> instrumentationResults = instrumentationPointsUtil.collectInstrumentationResults(Collections.singleton(classType), classCache, environment);
		if (CollectionUtils.isNotEmpty(instrumentationResults)) {
			return instrumentationResults.iterator().next();
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void profileUpdated(Profile profile, Collection<MethodSensorAssignment> removedMethodSensorAssignments, Collection<ExceptionSensorAssignment> removedExceptionSensorAssignments,
			Collection<MethodSensorAssignment> addedMethodSensorAssignments, Collection<ExceptionSensorAssignment> addedExceptionSensorAssignments) {

		// if no changes, do nothing
		if (CollectionUtils.isEmpty(removedMethodSensorAssignments) && CollectionUtils.isEmpty(removedExceptionSensorAssignments) && CollectionUtils.isEmpty(addedMethodSensorAssignments)
				&& CollectionUtils.isEmpty(addedExceptionSensorAssignments)) {
			return;
		}

		// otherwise look all agent cache entries if profile is contained in environment
		for (AgentCacheEntry agentCacheEntry : agentCacheMap.values()) {
			Environment environment = agentCacheEntry.getEnvironment();

			if (null == environment) {
				continue;
			}
			if (CollectionUtils.isEmpty(environment.getProfileIds()) || !environment.getProfileIds().contains(profile.getId())) {
				continue;
			}

			// create and fire job
			ProfileUpdateJob profileUpdateJob = profileUpdateJobFactory.getObject();
			profileUpdateJob.setRemovedMethodSensorAssignments(removedMethodSensorAssignments);
			profileUpdateJob.setRemovedExceptionSensorAssignments(removedExceptionSensorAssignments);
			profileUpdateJob.setAddedMethodSensorAssignments(addedMethodSensorAssignments);
			profileUpdateJob.setAddedExceptionSensorAssignments(addedExceptionSensorAssignments);
			profileUpdateJob.setAgentCacheEntry(agentCacheEntry);

			executor.execute(profileUpdateJob);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void environmentUpdated(Environment updated, Collection<Profile> removedProfiles, Collection<Profile> addedProfiles) {
		for (AgentCacheEntry agentCacheEntry : agentCacheMap.values()) {
			Environment environment = agentCacheEntry.getEnvironment();

			if (null == environment) {
				continue;
			}
			if (!Objects.equals(environment.getId(), updated.getId())) {
				continue;
			}

			// create and fire job
			EnvironmentUpdateJob environmentUpdateJob = environmentUpdateJobFactory.getObject();
			environmentUpdateJob.setEnvironment(updated);
			environmentUpdateJob.setRemovedProfiles(removedProfiles);
			environmentUpdateJob.setAddedProfiles(addedProfiles);
			environmentUpdateJob.setAgentCacheEntry(agentCacheEntry);

			executor.execute(environmentUpdateJob);
		}
	}

	/**
	 * Returns agent cache entry for the agent.
	 * 
	 * @param platformIdent
	 *            Agent id.
	 * @return {@link AgentCacheEntry}
	 */
	private AgentCacheEntry getAgentCacheEntry(long platformIdent) {
		AgentCacheEntry agentCacheEntry = agentCacheMap.get(Long.valueOf(platformIdent));
		if (null == agentCacheEntry) {
			ClassCache classCache = classCacheFactory.getObject();
			agentCacheEntry = new AgentCacheEntry(platformIdent, classCache);
			AgentCacheEntry existing = agentCacheMap.putIfAbsent(Long.valueOf(platformIdent), agentCacheEntry);
			if (null != existing) {
				agentCacheEntry = existing;
			}
		}
		return agentCacheEntry;
	}

}
