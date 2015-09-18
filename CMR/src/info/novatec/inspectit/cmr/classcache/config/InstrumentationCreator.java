package info.novatec.inspectit.cmr.classcache.config;

import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.ExceptionSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.MethodSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.TimerMethodSensorAssignment;
import info.novatec.inspectit.ci.context.AbstractContextCapture;
import info.novatec.inspectit.ci.sensor.method.IMethodSensorConfig;
import info.novatec.inspectit.ci.sensor.method.impl.InvocationSequenceSensorConfig;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.MethodType;
import info.novatec.inspectit.classcache.MethodType.Character;
import info.novatec.inspectit.cmr.classcache.config.filter.ClassSensorAssignmentFilter;
import info.novatec.inspectit.cmr.classcache.config.filter.MethodSensorAssignmentFilter;
import info.novatec.inspectit.cmr.service.IRegistrationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * INstrumentation creator is responsible for creating and removing the instrumentation points in
 * the class cache.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class InstrumentationCreator {

	/**
	 * FQn of the java class loader classes needed for the class loading delegation.
	 */
	private static final String JAVA_CLASS_LAODER_FQN = "java.lang.ClassLoader";

	/**
	 * {@link MethodSensorAssignmentFilter}.
	 */
	MethodSensorAssignmentFilter methodFilter = new MethodSensorAssignmentFilter();

	/**
	 * {@link ClassSensorAssignmentFilter}.
	 */
	ClassSensorAssignmentFilter classFilter = new ClassSensorAssignmentFilter();

	/**
	 * Registration service used.
	 */
	@Autowired
	IRegistrationService registrationService;

	/**
	 * {@link ConfigurationResolver}.
	 */
	@Autowired
	ConfigurationResolver configurationResolver;

	/**
	 * Defines if the given {@link ImmutableClassType} should be used for the class loading
	 * delegation instrumentation.
	 * <p>
	 * This occurs only if the {@link Environment#isClassLoadingDelegation()} is set to true and
	 * class is itself {@value #JAVA_CLASS_LAODER_FQN} or it's sub-class.
	 * 
	 * @param classType
	 *            Type to process
	 * @param environment
	 *            {@link Environment} holding configuration.
	 * @return <code>true</code> if class loading delegation should be applied
	 */
	public boolean analyzeForClassLoadingDelegation(ImmutableClassType classType, Environment environment) {
		if (!environment.isClassLoadingDelegation()) {
			return false;
		}

		return JAVA_CLASS_LAODER_FQN.equals(classType.getFQN()) || classType.isSubClassOf(JAVA_CLASS_LAODER_FQN);
	}

	/**
	 * Runs the complete configuration against collection of the {@link ClassType} to check for the
	 * possible instrumentation points to add. Note that all {@link MethodSensorAssignment}s and
	 * {@link ExceptionSensorAssignment}s that are contained in the {@link Environment} will be
	 * processed.
	 * 
	 * @param agentConfiguration
	 *            Agent configuration currently used
	 * @param environment
	 *            {@link Environment} holding configuration.
	 * @param classTypes
	 *            {@link ClassType}s to process.
	 * @return Returns collection of the {@link ClassType}s that have at least one instrumentation
	 *         added as a result of this method.
	 */
	public Collection<ClassType> addInstrumentationPoints(AgentConfiguration agentConfiguration, Environment environment, Collection<ClassType> classTypes) {
		if (CollectionUtils.isEmpty(classTypes)) {
			return Collections.emptyList();
		}
		Collection<ClassType> changedClassTypes = new ArrayList<>();
		Collection<MethodSensorAssignment> methodSensorAssignments = configurationResolver.getAllMethodSensorAssignments(environment);
		Collection<ExceptionSensorAssignment> exceptionSensorAssignments = configurationResolver.getAllExceptionSensorAssignments(environment);

		for (ClassType classType : classTypes) {
			if (addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments)) {
				changedClassTypes.add(classType);
			}
		}

		return changedClassTypes;
	}

	/**
	 * Runs the complete configuration against the {@link ClassType} to check for the possible
	 * instrumentation points to add. Note that all {@link MethodSensorAssignment}s and
	 * {@link ExceptionSensorAssignment}s that are contained in the {@link Environment} will be
	 * processed.
	 * 
	 * @param agentConfiguration
	 *            Agent configuration currently used
	 * @param environment
	 *            {@link Environment} holding configuration.
	 * @param classType
	 *            {@link ClassType} to process.
	 * @return <code>true</code> if at least one instrumentation point was added, <code>false</code>
	 *         otherwise
	 */
	public boolean addInstrumentationPoints(AgentConfiguration agentConfiguration, Environment environment, ClassType classType) {
		Collection<MethodSensorAssignment> methodSensorAssignments = configurationResolver.getAllMethodSensorAssignments(environment);
		Collection<ExceptionSensorAssignment> exceptionSensorAssignments = configurationResolver.getAllExceptionSensorAssignments(environment);

		return addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments);
	}

	/**
	 * Runs the given assignments against the {@link ClassType} to check for the possible
	 * instrumentation points to add. Note that only given assignments will be processed.
	 * 
	 * @param agentConfiguration
	 *            Agent configuration currently used
	 * @param environment
	 *            {@link Environment} holding configuration.
	 * @param classType
	 *            {@link ClassType} to process.
	 * @param methodSensorAssignments
	 *            {@link MethodSensorAssignment}s to process.
	 * @param exceptionSensorAssignments
	 *            {@link ExceptionSensorAssignment}s to process.
	 * @return <code>true</code> if at least one instrumentation point was added, <code>false</code>
	 *         otherwise
	 */
	public boolean addInstrumentationPoints(AgentConfiguration agentConfiguration, Environment environment, ClassType classType, Collection<MethodSensorAssignment> methodSensorAssignments,
			Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		boolean result = false;

		// check for method assignments
		if (CollectionUtils.isNotEmpty(methodSensorAssignments)) {
			for (MethodSensorAssignment assignment : methodSensorAssignments) {
				if (classFilter.matches(assignment, classType)) {
					for (MethodType methodType : classType.getMethods()) {
						if (methodFilter.matches(assignment, methodType)) {
							RegisteredSensorConfig registeredSensorConfig = getOrCreateRegisteredSensorConfig(agentConfiguration, classType, methodType);
							applyMethodAssignment(agentConfiguration, environment, assignment, registeredSensorConfig);
							result = true;
						}
					}
				}
			}
		}

		// then for exception ones
		if (classType.isException() && CollectionUtils.isNotEmpty(exceptionSensorAssignments)) {
			for (ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
				if (classFilter.matches(exceptionSensorAssignment, classType)) {
					for (MethodType methodType : classType.getMethods()) {
						if (Character.CONSTRUCTOR.equals(methodType.getMethodCharacter())) {
							RegisteredSensorConfig registeredSensorConfig = getOrCreateRegisteredSensorConfig(agentConfiguration, classType, methodType);
							applyExceptionAssignemt(agentConfiguration, exceptionSensorAssignment, registeredSensorConfig);
							result = true;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Removes all instrumentation points from the given {@link ClassType}.
	 * 
	 * @param classType
	 *            {@link ClassType} to remove instrumentation from.
	 * @return <code>true</code> if any instrumentation point was removed, <code>false</code> if no
	 *         removal was done
	 */
	public boolean removeInstrumentationPoints(ClassType classType) {
		if (!classType.hasInstrumentationPoints()) {
			return false;
		}

		// just iterate over all method types and set RSC to null
		for (MethodType methodType : classType.getMethods()) {
			methodType.setRegisteredSensorConfig(null);
		}
		return true;
	}

	/**
	 * Removes all instrumentation points that might be created as result of given method and
	 * exception sensor assignment from a {@link ClassType}.
	 * 
	 * @param classType
	 *            {@link ClassType} to remove instrumentation from. * @param methodSensorAssignments
	 *            {@link MethodSensorAssignment}s to process.
	 * @param exceptionSensorAssignments
	 *            {@link ExceptionSensorAssignment}s to process.
	 * @return <code>true</code> if any instrumentation point was removed, <code>false</code> if no
	 *         removal was done
	 */
	public boolean removeInstrumentationPoints(ClassType classType, Collection<MethodSensorAssignment> methodSensorAssignments, Collection<ExceptionSensorAssignment> exceptionSensorAssignments) {
		boolean result = false;

		// check for method assignments
		if (CollectionUtils.isNotEmpty(methodSensorAssignments)) {
			for (MethodSensorAssignment assignment : methodSensorAssignments) {
				if (classFilter.matches(assignment, classType)) {
					for (MethodType methodType : classType.getMethods()) {
						if (methodFilter.matches(assignment, methodType)) {
							methodType.setRegisteredSensorConfig(null);
							result = true;
						}
					}
				}
			}
		}

		// then for exception ones
		if (classType.isException() && CollectionUtils.isNotEmpty(exceptionSensorAssignments)) {
			for (ExceptionSensorAssignment exceptionSensorAssignment : exceptionSensorAssignments) {
				if (classFilter.matches(exceptionSensorAssignment, classType)) {
					for (MethodType methodType : classType.getMethods()) {
						if (Character.CONSTRUCTOR.equals(methodType.getMethodCharacter())) {
							methodType.setRegisteredSensorConfig(null);
							result = true;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Checks if the {@link RegisteredSensorConfig} exists in the {@link MethodType}. If not new one
	 * is created, registered with registration service and saved in the {@link MethodType}.
	 * 
	 * @param agentConfiguration
	 *            {@link AgentConfiguration} to read platform id.
	 * @param classType
	 *            ClassType method belongs to.
	 * @param methodType
	 *            {@link MethodType} in question.
	 * @return {@link RegisteredSensorConfig} for the {@link MethodType}.
	 */
	private RegisteredSensorConfig getOrCreateRegisteredSensorConfig(AgentConfiguration agentConfiguration, ClassType classType, MethodType methodType) {
		// check for existing
		RegisteredSensorConfig registeredSensorConfig = methodType.getRegisteredSensorConfig();

		// if not create new one
		if (null == registeredSensorConfig) {
			// if not create new and register

			// extract package and class name
			String fqn = classType.getFQN();
			int index = fqn.lastIndexOf('.');
			String packageName = fqn.substring(0, index);
			String className = fqn.substring(index + 1);
			String methodName = Character.CONSTRUCTOR.equals(methodType.getMethodCharacter()) ? className : methodType.getName();

			long id = registrationService.registerMethodIdent(agentConfiguration.getPlatformId(), packageName, className, methodName, methodType.getParameters(), methodType.getReturnType(),
					methodType.getModifiers());

			registeredSensorConfig = new RegisteredSensorConfig();
			registeredSensorConfig.setId(id);
			registeredSensorConfig.setTargetClassFqn(fqn);
			registeredSensorConfig.setTargetMethodName(methodType.getName());
			registeredSensorConfig.setReturnType(methodType.getReturnType());
			registeredSensorConfig.setParameterTypes(new ArrayList<>(methodType.getParameters()));

			methodType.setRegisteredSensorConfig(registeredSensorConfig);
		}

		return registeredSensorConfig;
	}

	/**
	 * Applies {@link ExceptionSensorTypeConfig} to the {@link RegisteredSensorConfig}.
	 * 
	 * @param agentConfiguration
	 *            {@link AgentConfiguration}.
	 * @param assignment
	 *            {@link MethodSensorAssignment}
	 * @param rsc
	 *            {@link RegisteredSensorConfig}
	 */
	private void applyExceptionAssignemt(AgentConfiguration agentConfiguration, ExceptionSensorAssignment assignment, RegisteredSensorConfig rsc) {
		// there can be only one exception sensor so I just take the id
		ExceptionSensorTypeConfig exceptionSensorTypeConfig = agentConfiguration.getExceptionSensorTypeConfig();
		long sensorId = exceptionSensorTypeConfig.getId();

		if (rsc.addSensorId(sensorId, exceptionSensorTypeConfig.getPriority())) {
			// if this is new id for the sensor config then register mapping
			registrationService.addSensorTypeToMethod(sensorId, rsc.getId());
		}

		// add all settings
		rsc.addSettings(assignment.getSettings());

	}

	/**
	 * Applies {@link MethodSensorAssignment} to the {@link RegisteredSensorConfig}.
	 * 
	 * @param agentConfiguration
	 *            {@link AgentConfiguration}.
	 * @param environment
	 *            {@link Environment}
	 * @param assignment
	 *            {@link MethodSensorAssignment}
	 * @param rsc
	 *            {@link RegisteredSensorConfig}
	 */
	private void applyMethodAssignment(AgentConfiguration agentConfiguration, Environment environment, MethodSensorAssignment assignment, RegisteredSensorConfig rsc) {
		// first deal with sensor id
		MethodSensorTypeConfig methodSensorTypeConfig = getSensorTypeConfigFromConfiguration(agentConfiguration, environment, assignment);
		long sensorId = methodSensorTypeConfig.getId();
		if (rsc.addSensorId(sensorId, methodSensorTypeConfig.getPriority())) {
			// if this is new id for the sensor config then register mapping
			registrationService.addSensorTypeToMethod(sensorId, rsc.getId());
		}

		// add all settings
		rsc.addSettings(assignment.getSettings());

		// additional work if it's timer sensor assignment
		if (assignment instanceof TimerMethodSensorAssignment) {
			TimerMethodSensorAssignment timerAssignment = (TimerMethodSensorAssignment) assignment;

			// check for invocation starts
			if (timerAssignment.isStartsInvocation()) {
				// find the id of invocation sensor and only mark if one is found
				IMethodSensorConfig invocationSensorConfig = environment.getMethodSensorTypeConfig(InvocationSequenceSensorConfig.class);
				if (null != invocationSensorConfig) {
					MethodSensorTypeConfig invocationSensorTypeConfig = agentConfiguration.getMethodSensorTypeConfig(invocationSensorConfig.getClassName());
					if (rsc.addSensorId(invocationSensorTypeConfig.getId(), invocationSensorTypeConfig.getPriority())) {
						// if this is new id for the invocation sensor config then register mapping
						registrationService.addSensorTypeToMethod(invocationSensorTypeConfig.getId(), rsc.getId());
						rsc.setStartsInvocation(true);
					}
				}
			}

			// deal with context captures
			if (CollectionUtils.isNotEmpty(timerAssignment.getContextCaptures())) {
				for (AbstractContextCapture contextCapture : timerAssignment.getContextCaptures()) {
					rsc.addPropertyAccessor(contextCapture.getPropertyPathStart());
				}
			}
		}
	}

	/**
	 * Finds the proper sensor id from the agent configuration and the environment used for the
	 * {@link MethodSensorAssignment}.
	 * 
	 * @param agentConfiguration
	 *            {@link AgentConfiguration}
	 * @param environment
	 *            {@link Environment}
	 * @param assignment
	 *            {@link MethodSensorAssignment}
	 * @return {@link MethodSensorTypeConfig} for the given assignemnt.
	 */
	private MethodSensorTypeConfig getSensorTypeConfigFromConfiguration(AgentConfiguration agentConfiguration, Environment environment, MethodSensorAssignment assignment) {
		IMethodSensorConfig methodSensorConfig = environment.getMethodSensorTypeConfig(assignment.getSensorConfigClass());
		return agentConfiguration.getMethodSensorTypeConfig(methodSensorConfig.getClassName());
	}

}
