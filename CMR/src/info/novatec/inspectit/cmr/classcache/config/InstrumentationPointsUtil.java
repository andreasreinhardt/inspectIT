package info.novatec.inspectit.cmr.classcache.config;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableType;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.spring.logger.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class holds some general utility methods when working with class cache and instrumentation
 * points. The class has methods that can:
 * <ul>
 * <li>Remove all instrumentation points the set of Types or complete ClassCache
 * <li>Add instrumentation points to the the set of Types or complete ClassCache with given
 * {@link AgentConfiguration} and {@link Environment}
 * <li>Collect instrumentation points from the set of Types or complete ClassCache
 * </ul>
 * <p>
 * Note that this class will access the {@link ClassCache} with needed read or write locks. Thus,
 * caller should not care about acquiring the locks prior to calling the methods.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class InstrumentationPointsUtil {

	/**
	 * Logger of the class.
	 */
	@Log
	Logger log;

	/**
	 * {@link InstrumentationCreator} when creating new points.
	 */
	@Autowired
	private InstrumentationCreator instrumentationCreator;

	/**
	 * Configuration resolver.
	 */
	@Autowired
	private ConfigurationResolver configurationResolver;

	/**
	 * Removes all instrumentation point from the {@link ClassCache}.
	 * 
	 * @param classCache
	 *            {@link ClassCache}.
	 * @return types from which instrumentation points have been removed
	 */
	public Collection<? extends ImmutableClassType> removeAllInstrumentationPoints(final ClassCache classCache) {
		final Collection<? extends ImmutableType> types = classCache.getLookupService().findAll();
		return removeAllInstrumentationPoints(types, classCache);
	}

	/**
	 * Removes all instrumentation point from the given types that belong to the specified class
	 * cache.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to remove instrumentation points
	 * @param classCache
	 *            {@link ClassCache}.
	 * @return types from which instrumentation points have been removed
	 */
	public Collection<? extends ImmutableClassType> removeAllInstrumentationPoints(final Collection<? extends ImmutableType> types, final ClassCache classCache) {
		if (CollectionUtils.isEmpty(types)) {
			return Collections.emptyList();
		}

		try {
			return classCache.executeWithWriteLock(new Callable<Collection<? extends ImmutableClassType>>() {
				@Override
				public Collection<? extends ImmutableClassType> call() throws Exception {
					Collection<ImmutableClassType> results = new ArrayList<>();
					for (ImmutableType type : types) {
						// only initialized class types can have instrumentation points
						if (type.isClass() && type.isInitialized()) {
							if (instrumentationCreator.removeInstrumentationPoints((ClassType) type.castToClass())) {
								results.add(type.castToClass());
							}
						}
					}
					return results;
				}
			});
		} catch (Exception e) {
			log.error("Error occurred while trying to remove all instrumentation points from the class cache.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Removes all instrumentation point from the {@link ClassCache} that might be created as result
	 * of given method and exception sensor assignment .
	 * 
	 * @param classCache
	 *            {@link ClassCache}.
	 * @param methodSensorAssignments
	 *            {@link MethodSensorAssignment}s to process.
	 * @param exceptionSensorAssignments
	 *            {@link ExceptionSensorAssignment}s to process.
	 * @return types from which instrumentation points have been removed
	 */
	public Collection<? extends ImmutableClassType> removeInstrumentationPoints(final ClassCache classCache, Collection<MethodSensorAssignment> methodSensorAssignments,
			Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		final Collection<? extends ImmutableType> types = classCache.getLookupService().findAll();
		return removeInstrumentationPoints(types, classCache, methodSensorAssignments, exceptionSensorAssignments);
	}

	/**
	 * Removes all instrumentation point from the given types that belong to the specified class
	 * cache and that might be created as result of given method and exception sensor assignment.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to remove instrumentation points
	 * @param classCache
	 *            {@link ClassCache}.
	 * @param methodSensorAssignments
	 *            {@link MethodSensorAssignment}s to process.
	 * @param exceptionSensorAssignments
	 *            {@link ExceptionSensorAssignment}s to process.
	 * @return types from which instrumentation points have been removed
	 */
	public Collection<? extends ImmutableClassType> removeInstrumentationPoints(final Collection<? extends ImmutableType> types, final ClassCache classCache,
			final Collection<MethodSensorAssignment> methodSensorAssignments, final Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		if (CollectionUtils.isEmpty(types)) {
			return Collections.emptyList();
		}

		try {
			return classCache.executeWithWriteLock(new Callable<Collection<? extends ImmutableClassType>>() {
				@Override
				public Collection<? extends ImmutableClassType> call() throws Exception {
					Collection<ImmutableClassType> results = new ArrayList<>();
					for (ImmutableType type : types) {
						// only initialized class types can have instrumentation points
						if (type.isClass() && type.isInitialized()) {
							if (instrumentationCreator.removeInstrumentationPoints((ClassType) type.castToClass(), methodSensorAssignments, exceptionSensorAssignments)) {
								results.add(type.castToClass());
							}
						}
					}
					return results;
				}
			});
		} catch (Exception e) {
			log.error("Error occurred while trying to remove specific instrumentation points from the class cache.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Add all instrumentation point in the given {@link ClassCache}.
	 * 
	 * @param classCache
	 *            {@link ClassCache}.
	 * @param agentConfiguration
	 *            configuration to use
	 * @param environment
	 *            environment
	 * @return Returns collection of class types to which the instrumentation points have been
	 *         added.
	 */
	public Collection<? extends ImmutableClassType> addAllInstrumentationPoints(final ClassCache classCache, final AgentConfiguration agentConfiguration, final Environment environment) {
		final Collection<? extends ImmutableType> types = classCache.getLookupService().findAll();
		return addAllInstrumentationPoints(types, classCache, agentConfiguration, environment);

		// TODO make a decision here what is better, in theory bottom code should be more effective

		/*
		Collection<MethodSensorAssignment> methodSensorAssignments = configurationResolver.getAllMethodSensorAssignments(environment);
		Collection<ExceptionSensorAssignment> exceptionSensorAssignments = configurationResolver.getAllExceptionSensorAssignments(environment);

		Collection<ImmutableClassType> results = new ArrayList<ImmutableClassType>();

		for (MethodSensorAssignment assignment : methodSensorAssignments) {
			results.addAll(addInstrumentationPoints(searchNarrower.narrowByMethodSensorAssignment(classCache, assignment), classCache, agentConfiguration, environment,
					Collections.singleton(assignment), null));
		}

		for (ExceptionSensorAssignment assignment : exceptionSensorAssignments) {
			results.addAll(addInstrumentationPoints(searchNarrower.narrowByExceptionSensorAssignment(classCache, assignment), classCache, agentConfiguration, environment, null,
					Collections.singleton(assignment)));
		}

		return results;
		*/
	}

	/**
	 * Add all instrumentation point in the given types that belong to the specified class cache.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to add instrumentation points based on given configuration and environment.
	 * @param classCache
	 *            {@link ClassCache} types belong to.
	 * @param agentConfiguration
	 *            configuration to use
	 * @param environment
	 *            environment
	 * @return Returns collection of class types to which the instrumentation points have been
	 *         added.
	 */
	public Collection<? extends ImmutableClassType> addAllInstrumentationPoints(final Collection<? extends ImmutableType> types, final ClassCache classCache,
			final AgentConfiguration agentConfiguration, final Environment environment) {
		Collection<MethodSensorAssignment> methodSensorAssignments = configurationResolver.getAllMethodSensorAssignments(environment);
		Collection<ExceptionSensorAssignment> exceptionSensorAssignments = configurationResolver.getAllExceptionSensorAssignments(environment);

		return addInstrumentationPoints(types, classCache, agentConfiguration, environment, methodSensorAssignments, exceptionSensorAssignments);
	}

	/**
	 * Adds instrumentation points to the set of types belonging to the class cache. Instrumentation
	 * points added will be created only if the types satisfies the given method and exception
	 * assignment.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to add instrumentation points based on given configuration and environment.
	 * @param classCache
	 *            {@link ClassCache} types belong to.
	 * @param agentConfiguration
	 *            configuration to use
	 * @param environment
	 *            environment
	 * @param methodSensorAssignments
	 *            {@link MethodSensorAssignment}s to process.
	 * @param exceptionSensorAssignments
	 *            {@link ExceptionSensorAssignment}s to process.
	 * @return Returns collection of class types to which the instrumentation points have been
	 *         added.
	 */
	public Collection<? extends ImmutableClassType> addInstrumentationPoints(final Collection<? extends ImmutableType> types, final ClassCache classCache, final AgentConfiguration agentConfiguration,
			final Environment environment, final Collection<MethodSensorAssignment> methodSensorAssignments, final Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		if (CollectionUtils.isEmpty(types)) {
			return Collections.emptyList();
		}

		try {
			return classCache.executeWithWriteLock(new Callable<Collection<? extends ImmutableClassType>>() {
				@Override
				public Collection<? extends ImmutableClassType> call() throws Exception {
					Collection<ImmutableClassType> results = new ArrayList<>();
					for (ImmutableType type : types) {
						// only initialized class types can have instrumentation points
						if (type.isClass() && type.isInitialized()) {
							if (instrumentationCreator.addInstrumentationPoints(agentConfiguration, environment, (ClassType) type.castToClass(), methodSensorAssignments, exceptionSensorAssignments)) {
								results.add(type.castToClass());
							}
						}
					}
					return results;
				}
			});
		} catch (Exception e) {
			log.error("Error occurred while trying to add instrumentation points from the class cache.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Collects instrumentation points for all the initialized class types in the given class cache.
	 * <p>
	 * This method is thread-safe, as it will use the class cache read lock.
	 * 
	 * @param classCache
	 *            Class cache to search in.
	 * @param environment
	 *            Environment that is used.
	 * @return Collection holding all the {@link InstrumentationResult}.
	 */
	public Collection<InstrumentationResult> collectInstrumentationResults(final ClassCache classCache, final Environment environment) {
		final Collection<? extends ImmutableType> types = classCache.getLookupService().findAll();
		return collectInstrumentationResults(types, classCache, environment);
	}

	/**
	 * Collects instrumentation points for given types in the given class cache. Only initialized
	 * class types will be checked.
	 * <p>
	 * This method is thread-safe, as it will use the class cache read lock.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to collect results for
	 * @param classCache
	 *            Class cache to search in.
	 * @param environment
	 *            Environment that is used.
	 * @return Collection holding all the {@link InstrumentationResult}.
	 */
	public Collection<InstrumentationResult> collectInstrumentationResults(final Collection<? extends ImmutableType> types, final ClassCache classCache, final Environment environment) {
		if (CollectionUtils.isEmpty(types)) {
			return Collections.emptyList();
		}

		try {
			return classCache.executeWithReadLock(new Callable<Collection<InstrumentationResult>>() {
				@Override
				public Collection<InstrumentationResult> call() throws Exception {
					Collection<InstrumentationResult> results = new ArrayList<>();
					for (ImmutableType type : types) {
						if (type.isInitialized() && type.isClass()) {
							ImmutableClassType immutableClassType = type.castToClass();
							InstrumentationResult instrumentationResult = instrumentationCreator.createInstrumentationResult(immutableClassType, environment);
							if (null != instrumentationResult) {
								results.add(instrumentationResult);
							}
						}
					}
					return results;
				}
			});
		} catch (Exception e) {
			log.error("Error occurred while trying to collect instrumentation results from the class cache.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Collects instrumentation points for all the initialized class types in the given class cache.
	 * The return map will contain a key-value pairs where key is set of hashes that correspond to
	 * the instrumentation result (value).
	 * <p>
	 * This method is thread-safe, as it will use the class cache read lock.
	 * 
	 * @param classCache
	 *            Class cache to search in.
	 * @param environment
	 *            Environment that is used.
	 * @return Map holding key-value pairs that connect set of hashes to the
	 *         {@link InstrumentationResult}.
	 */
	public Map<Collection<String>, InstrumentationResult> collectInstrumentationResultsWithHashes(final ClassCache classCache, final Environment environment) {
		final Collection<? extends ImmutableType> types = classCache.getLookupService().findAll();
		return collectInstrumentationResultsWithHashes(types, classCache, environment);
	}

	/**
	 * Collects instrumentation points for given types in the given class cache. Only initialized
	 * class types will be checked.The return map will contain a key-value pairs where key is set of
	 * hashes that correspond to the instrumentation result (value).
	 * <p>
	 * This method is thread-safe, as it will use the class cache read lock.
	 * <p>
	 * <b>IMPORTANT:</b> It's responsibility of the caller to ensure the given types do belong to
	 * the given class cache.
	 * 
	 * @param types
	 *            to collect results for
	 * @param classCache
	 *            Class cache to search in.
	 * @param environment
	 *            Environment that is used.
	 * @return Map holding key-value pairs that connect set of hashes to the
	 *         {@link InstrumentationResult}.
	 */
	public Map<Collection<String>, InstrumentationResult> collectInstrumentationResultsWithHashes(final Collection<? extends ImmutableType> types, final ClassCache classCache,
			final Environment environment) {
		if (CollectionUtils.isEmpty(types)) {
			return Collections.emptyMap();
		}

		try {
			return classCache.executeWithReadLock(new Callable<Map<Collection<String>, InstrumentationResult>>() {
				@Override
				public Map<Collection<String>, InstrumentationResult> call() throws Exception {
					Map<Collection<String>, InstrumentationResult> map = new HashMap<>();
					for (ImmutableType type : types) {
						if (type.isInitialized() && type.isClass()) {
							ImmutableClassType immutableClassType = type.castToClass();
							InstrumentationResult instrumentationResult = instrumentationCreator.createInstrumentationResult(immutableClassType, environment);
							if (null != instrumentationResult) {
								map.put(immutableClassType.getHashes(), instrumentationResult);
							}
						}
					}
					return map;
				}
			});
		} catch (Exception e) {
			log.error("Error occurred while trying to collect instrumentation results from the class cache.", e);
			return Collections.emptyMap();
		}
	}

}
