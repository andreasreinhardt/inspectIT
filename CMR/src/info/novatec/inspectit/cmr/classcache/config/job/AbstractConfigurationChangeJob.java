package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.config.AgentCacheEntry;
import info.novatec.inspectit.cmr.classcache.config.ClassCacheSearchNarrower;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationCreator;
import info.novatec.inspectit.cmr.classcache.config.InstrumentationCreator;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.spring.logger.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

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
	 * {@link InstrumentationCreator}.
	 */
	@Autowired
	InstrumentationCreator instrumentationCreator;

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
		try {
			AgentConfiguration agentConfiguration = configurationCreator.environmentToConfiguration(update, getAgentId());

			agentCacheEntry.setEnvironment(update);
			agentCacheEntry.setAgentConfiguration(agentConfiguration);

			return true;
		} catch (BusinessException e) { // TODO check enxeptions
			log.error("Error occurred trying to create updated Agent configuration during environment update job. ");
			return false;
		}
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
	 * @return If any change to the instrumentation points was performed.
	 */
	protected boolean processRemovedAssignments(Collection<MethodSensorAssignment> methodSensorAssignments, Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		final Collection<ClassType> changedClassTypes = new ArrayList<>();

		// first process all method sensors for removal
		for (final MethodSensorAssignment methodSensorAssignment : methodSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByMethodSensorAssignment(getClassCache(), methodSensorAssignment);
			for (final ImmutableClassType classType : classTypes) {
				try {
					boolean changed = getClassCache().executeWithWriteLock(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return instrumentationCreator.removeInstrumentationPoints((ClassType) classType, Collections.singletonList(methodSensorAssignment), null);
						}
					});

					if (changed) {
						changedClassTypes.add((ClassType) classType);
					}
				} catch (Exception e) {
					log.error("Error removing existing method instrumentation points during configuration change job.", e);
					continue;
				}
			}
		}

		// then process all exception sensors for removal
		for (final ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByExceptionSensorAssignment(getClassCache(), exceptionSensorAssignment);
			for (final ImmutableClassType classType : classTypes) {
				try {
					boolean changed = getClassCache().executeWithWriteLock(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return instrumentationCreator.removeInstrumentationPoints((ClassType) classType, null, Collections.singletonList(exceptionSensorAssignment));
						}
					});

					if (changed) {
						changedClassTypes.add((ClassType) classType);
					}
				} catch (Exception e) {
					log.error("Error removing existing exception instrumentation points during configuration change job.", e);
					continue;
				}
			}
		}

		// if no class was affected just return
		if (CollectionUtils.isEmpty(changedClassTypes)) {
			return false;
		}

		// if any class was affected re-check those classes against complete configuration
		// because we removed all instrumentation points
		try {
			getClassCache().executeWithWriteLock(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					instrumentationCreator.addInstrumentationPoints(getAgentConfiguration(), getEnvironment(), changedClassTypes);
					return null;
				}
			});
		} catch (Exception e) {
			log.error("Error removing existing exception instrumentation points during configuration change job.", e);
		}

		return true;
	}

	/**
	 * Process the added assignments. New instrumentation points will be added to all the classes in
	 * the class cache that fit to the given assignments.
	 * 
	 * @param methodSensorAssignments
	 *            Collection of removed {@link MethodSensorAssignment}s.
	 * @param exceptionSensorAssignments
	 *            Collection of removed {@link ExceptionSensorAssignment}s.
	 * @return If any new instrumentation point is added.
	 */
	protected boolean processAddedAssignments(Collection<MethodSensorAssignment> methodSensorAssignments, Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		boolean added = false;

		// go over all method sensor assignments
		for (final MethodSensorAssignment methodSensorAssignment : methodSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByMethodSensorAssignment(getClassCache(), methodSensorAssignment);
			for (final ImmutableClassType classType : classTypes) {
				try {
					added |= getClassCache().executeWithWriteLock(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return instrumentationCreator.addInstrumentationPoints(getAgentConfiguration(), getEnvironment(), (ClassType) classType, Collections.singletonList(methodSensorAssignment),
									null);
						}
					});
				} catch (Exception e) {
					log.error("Error adding new method instrumentation points configuration change job.", e);
					continue;
				}
			}
		}

		// go over all exception sensor assignments
		for (final ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
			Collection<? extends ImmutableClassType> classTypes = classCacheSearchNarrower.narrowByExceptionSensorAssignment(getClassCache(), exceptionSensorAssignment);
			for (final ImmutableClassType classType : classTypes) {
				try {
					added |= getClassCache().executeWithWriteLock(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return instrumentationCreator.addInstrumentationPoints(getAgentConfiguration(), getEnvironment(), (ClassType) classType, null,
									Collections.singletonList(exceptionSensorAssignment));
						}
					});
				} catch (Exception e) {
					log.error("Error adding new exception instrumentation points configuration change job.", e);
					continue;
				}
			}
		}

		return added;
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

}
