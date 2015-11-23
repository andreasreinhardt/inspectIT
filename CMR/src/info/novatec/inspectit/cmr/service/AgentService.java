package info.novatec.inspectit.cmr.service;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
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
import info.novatec.inspectit.cmr.classcache.config.job.MappingUpdateJob;
import info.novatec.inspectit.cmr.classcache.config.job.ProfileUpdateJob;
import info.novatec.inspectit.cmr.dao.PlatformIdentDao;
import info.novatec.inspectit.cmr.model.PlatformIdent;
import info.novatec.inspectit.cmr.spring.aop.MethodLog;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.exception.enumeration.AgentManagementErrorCodeEnum;
import info.novatec.inspectit.spring.logger.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
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
	 * Factory for creating new {@link MappingUpdateJob}.
	 */
	@Autowired
	private ObjectFactory<MappingUpdateJob> mappingUpdateJobFactory;

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
	 * Platform ident dao for resolving agents by ids.
	 */
	@Autowired
	private PlatformIdentDao platformIdentDao;

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
		AgentConfiguration agentConfiguration = agentCacheEntry.getAgentConfiguration();

		// if we have same environment and configuration return configuration
		if (Objects.equals(environment, cachedEnvironment) && null != agentConfiguration) {
			return agentConfiguration;
		}

		// else kick the configuration creator
		agentConfiguration = configurationCreator.environmentToConfiguration(environment, id);

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

		// then run configuration
		final ImmutableClassType classType = type.castToClass();
		final Environment environment = agentCacheEntry.getEnvironment();
		final AgentConfiguration agentConfiguration = agentCacheEntry.getAgentConfiguration();

		Collection<? extends ImmutableClassType> instrumented = instrumentationPointsUtil.addAllInstrumentationPoints(Collections.singleton(classType), classCache, agentConfiguration, environment);
		Collection<InstrumentationResult> instrumentationResults = instrumentationPointsUtil.collectInstrumentationResults(instrumented, classCache, environment);

		if (CollectionUtils.isNotEmpty(instrumentationResults)) {
			// as we have only one class, we expect only one instrumentation result
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
	 * {@inheritDoc}
	 */
	public void agentMappingsUpdated() {
		for (AgentCacheEntry agentCacheEntry : agentCacheMap.values()) {
			Environment cachedEnvironment = agentCacheEntry.getEnvironment();
			PlatformIdent platformIdent = platformIdentDao.load(agentCacheEntry.getId());
			try {
				Environment environment = configurationResolver.getEnvironmentForAgent(platformIdent.getDefinedIPs(), platformIdent.getAgentName());

				// if we have new environment fire job
				if (!ObjectUtils.equals(cachedEnvironment, environment)) {
					MappingUpdateJob mappingUpdateJob = mappingUpdateJobFactory.getObject();
					mappingUpdateJob.setEnvironment(environment);
					mappingUpdateJob.setAgentCacheEntry(agentCacheEntry);

					executor.execute(mappingUpdateJob);
				}
			} catch (BusinessException e) {
				// if we have exception by resolving new environment run job with no new
				// environment
				MappingUpdateJob mappingUpdateJob = mappingUpdateJobFactory.getObject();
				mappingUpdateJob.setAgentCacheEntry(agentCacheEntry);

				executor.execute(mappingUpdateJob);
			}
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
