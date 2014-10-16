package info.novatec.inspectit.agent.hooking.impl;

import info.novatec.inspectit.agent.config.impl.MethodSensorTypeConfig;
import info.novatec.inspectit.agent.hooking.IHook;
import info.novatec.inspectit.agent.hooking.IHookSupplier;
import info.novatec.inspectit.agent.sensor.exception.ExceptionSensor;
import info.novatec.inspectit.agent.sensor.method.IMethodSensor;
import info.novatec.inspectit.agent.sensor.method.invocationsequence.InvocationSequenceSensor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Default implementation of the {@link IHookSupplier}.
 * <p>
 * Note that hooks are stored in the normal hash maps and not concurrent ones. The content of the
 * maps stays the same after the initialization, thus the concurrent gets are no problem at all.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class HookSupplier implements IHookSupplier {

	/**
	 * The sensor type configuration object of the invocation sequence tracer.
	 */
	private MethodSensorTypeConfig invocationSequenceSensorTypeConfig = null;

	/**
	 * The sensor type configuration object of the exception sensor.
	 */
	private MethodSensorTypeConfig exceptionSensorTypeConfig = null;

	/**
	 * Map for fast getting of the MethodSensorTypeConfigs.
	 */
	private Map<Long, MethodSensorTypeConfig> sensorTypeConfigsMap = new HashMap<Long, MethodSensorTypeConfig>();

	/**
	 * The map used by the hooks in the source code to execute the after methods.
	 */
	private Map<Long, IHook> methodHookMap = new HashMap<Long, IHook>();

	/**
	 * {@inheritDoc}
	 */
	public void initialize(Collection<MethodSensorTypeConfig> sensorTypeConfigs) {
		for (MethodSensorTypeConfig sensorTypeConfig : sensorTypeConfigs) {

			// save invocation and exception sensor separately
			if (InvocationSequenceSensor.class.getName().equals(sensorTypeConfig.getClassName())) {
				invocationSequenceSensorTypeConfig = sensorTypeConfig;
			}

			if (ExceptionSensor.class.getName().equals(sensorTypeConfig.getClassName())) {
				exceptionSensorTypeConfig = sensorTypeConfig;
			}

			this.sensorTypeConfigsMap.put(sensorTypeConfig.getId(), sensorTypeConfig);
			this.methodHookMap.put(sensorTypeConfig.getId(), ((IMethodSensor) sensorTypeConfig.getSensorType()).getHook());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public MethodSensorTypeConfig getMethodSensorTypeConfig(long id) {
		return sensorTypeConfigsMap.get(Long.valueOf(id));
	}

	/**
	 * {@inheritDoc}
	 */
	public IHook getMethodHook(long id) {
		return methodHookMap.get(Long.valueOf(id));
	}

	/**
	 * {@inheritDoc}
	 */
	public MethodSensorTypeConfig getInvocationSequenceSensorTypeConfig() {
		return invocationSequenceSensorTypeConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	public MethodSensorTypeConfig getExceptionSensorTypeConfig() {
		return exceptionSensorTypeConfig;
	}

}