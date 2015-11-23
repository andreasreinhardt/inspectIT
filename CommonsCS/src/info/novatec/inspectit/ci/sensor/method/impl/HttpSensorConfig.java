package info.novatec.inspectit.ci.sensor.method.impl;

import info.novatec.inspectit.agent.config.PriorityEnum;
import info.novatec.inspectit.ci.sensor.StringConstraintSensorConfig;
import info.novatec.inspectit.ci.sensor.method.IMethodSensorConfig;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * HTTP sensor configuration.
 * 
 * @author Ivan Senic
 * 
 */
@XmlRootElement(name = "http-sensor-config")
public class HttpSensorConfig extends StringConstraintSensorConfig implements IMethodSensorConfig {

	/**
	 * Sensor name.
	 */
	private static final String SENSOR_NAME = "HTTP Sensor";

	/**
	 * Implementing class name.
	 */
	private static final String CLASS_NAME = "info.novatec.inspectit.agent.sensor.method.http.HttpSensor";


	/**
	 * No-args constructor.
	 */
	public HttpSensorConfig() {
		super(500);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return SENSOR_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PriorityEnum getPriority() {
		return PriorityEnum.MAX;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAdvanced() {
		return false;
	}

}