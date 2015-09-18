package info.novatec.inspectit.agent.config.impl;

import java.util.Collection;
import java.util.Collections;

/**
 * This simple data class is returned as the result from a server side instrumentation of a single
 * class.
 * 
 * @author Ivan Senic
 * 
 */
public class InstrumentationResult {

	/**
	 * Fully qualified class name that instrumentation results applies to.
	 */
	private String className;

	/**
	 * {@link RegisteredSensorConfig} that will be applied within the instrumented byte code.
	 */
	private Collection<RegisteredSensorConfig> registeredSensorConfigs = Collections.emptyList();

	/**
	 * If class should be instrumented with the class loading delegation.
	 */
	private boolean classLoadingDelegation;

	/**
	 * No arg-constructor.
	 */
	public InstrumentationResult() {
	}

	/**
	 * @param className
	 *            Fully qualified class name that instrumentation results applies to.
	 */
	public InstrumentationResult(String className) {
		this.className = className;
	}

	/**
	 * Gets {@link #className}.
	 * 
	 * @return {@link #className}
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Gets {@link #registeredSensorConfigs}.
	 * 
	 * @return {@link #registeredSensorConfigs}
	 */
	public Collection<RegisteredSensorConfig> getRegisteredSensorConfigs() {
		return registeredSensorConfigs;
	}

	/**
	 * Sets {@link #registeredSensorConfigs}.
	 * 
	 * @param registeredSensorConfigs
	 *            New value for {@link #registeredSensorConfigs}
	 */
	public void setRegisteredSensorConfigs(Collection<RegisteredSensorConfig> registeredSensorConfigs) {
		this.registeredSensorConfigs = registeredSensorConfigs;
	}

	/**
	 * Gets {@link #classLoadingDelegation}.
	 * 
	 * @return {@link #classLoadingDelegation}
	 */
	public boolean isClassLoadingDelegation() {
		return classLoadingDelegation;
	}

	/**
	 * Sets {@link #classLoadingDelegation}.
	 * 
	 * @param classLoadingDelegation
	 *            New value for {@link #classLoadingDelegation}
	 */
	public void setClassLoadingDelegation(boolean classLoadingDelegation) {
		this.classLoadingDelegation = classLoadingDelegation;
	}

}
