package com.spring;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.spring.PropertyAccessor.PropertyPathStart;

import javassist.CtBehavior;
import javassist.Modifier;

/**
 * After a sensor is registered at the CMR, this class is used to store all the information as the
 * {@link UnregisteredSensorConfig} contains information which is not needed anymore. Every
 * {@link RegisteredSensorConfig} class maps directly to one specific class and method with its
 * parameters.
 * 
 * @author Patrice Bouillet
 * @author Eduard Tudenhoefner
 * 
 */
public class RegisteredSensorConfig  {

	/**
	 * The {@link CtBehavior} object corresponding to this sensor configuration.
	 */
	private CtBehavior ctBehavior;

	/**
	 * The hash value.
	 */
	private long id;
	
	private List<PropertyPathStart> propertyAccessorList = new CopyOnWriteArrayList<PropertyPathStart>();

	/**
	 * The return type of the method.
	 */
	private String returnType = "";
	
	private Map<String, Object> settings = new HashMap<String, Object>();

	/**
	 * This list contains all configurations of the sensor types for this sensor configuration.
	 */
	private List<MethodSensorTypeConfig> sensorTypeConfigs = new ArrayList<MethodSensorTypeConfig>();

	/**
	 * The sensor type configuration object of the invocation sequence tracer.
	 */
	private MethodSensorTypeConfig invocationSequenceSensorTypeConfig = null;

	/**
	 * The sensor type configuration object of the exception sensor.
	 */
	private MethodSensorTypeConfig exceptionSensorTypeConfig = null;

	
	private boolean propertyAccess = false;
	/**
	 * The method visibility.
	 */
	private int modifiers;

	/**
	 * Sets the {@link CtBehavior} object.
	 * 
	 * @param behavior
	 *            The {@link CtBehavior} object.
	 */
	public void setCtBehavior(CtBehavior behavior) {
		this.ctBehavior = behavior;
	}

	/**
	 * Returns the {@link CtBehavior} object of this sensor configuration.
	 * 
	 * @return The {@link CtBehavior} object of this sensor configuration.
	 */
	public CtBehavior getCtBehavior() {
		return ctBehavior;
	}

	/**
	 * The unique id.
	 * 
	 * @return The unique id.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set the unique id.
	 * 
	 * @param id
	 *            The unique id.
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns the return type of the method.
	 * 
	 * @return The method return type.
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Sets the return type of the method.
	 * 
	 * @param returnType
	 *            The return type to set.
	 */
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	/**
	 * Returns the list containing all the sensor type configurations.
	 * 
	 * @return The list of sensor type configurations.
	 */
	public List<MethodSensorTypeConfig> getSensorTypeConfigs() {
		return sensorTypeConfigs;
	}

	/**
	 * Adds a sensor type to this configuration.
	 * 
	 * @param sensorTypeConfig
	 *            The sensor type to add.
	 */
	public void addSensorTypeConfig(MethodSensorTypeConfig sensorTypeConfig) {
		// check for the invocation sequence sensor type
		if (sensorTypeConfig.getClassName().endsWith("InvocationSequenceSensor")) {
			invocationSequenceSensorTypeConfig = sensorTypeConfig;
		}

		if (sensorTypeConfig.getClassName().endsWith("ExceptionSensor")) {
			exceptionSensorTypeConfig = sensorTypeConfig;
		}

		if (!sensorTypeConfigs.contains(sensorTypeConfig)) {
			sensorTypeConfigs.add(sensorTypeConfig);

			sortSensorTypeConfigs();
		

			// TODO
			// getCoreService().getIdManager().addSensorTypeToMethod(
			// sensorTypeConfig.getId(), getId());
		}
	}

	/**
	 * Remove a sensor type from this configuration.
	 * 
	 * @param sensorTypeConfig
	 *            The sensor type to remove.
	 */
	public void removeSensorTypeConfig(MethodSensorTypeConfig sensorTypeConfig) {
		if (sensorTypeConfigs.contains(sensorTypeConfig)) {
			sensorTypeConfigs.remove(sensorTypeConfig);

			

			// TODO remove at the server
		}
	}

	/**
	 * The sensor comparator is used to sort the sensor type configurations according to their
	 * priority.
	 */
	private Comparator<MethodSensorTypeConfig> sensorTypeConfigComparator = new SensorTypeConfigComparator();

	/**
	 * Sort the sensor type configurations according to their priority.
	 */
	private void sortSensorTypeConfigs() {
		if (sensorTypeConfigs.size() > 1) {
			Collections.sort(sensorTypeConfigs, sensorTypeConfigComparator);
		}
	}

	/**
	 * Returns the sorted method hooks as a {@link Map} with the hash value of the corresponding
	 * sensor type as the key and the {@link IMethodHook} implementation as the value.
	 * <p>
	 * The returned {@link Map} of this method is not locked for modification because of performance
	 * reasons.
	 * <p>
	 * This map is always generated along calling
	 * {@link #addSensorTypeConfig(MethodSensorTypeConfig)} and
	 * {@link #removeSensorTypeConfig(MethodSensorTypeConfig)}.
	 * 
	 * @return The sorted map of method hooks.
	 */
	

	/**
	 * Initialized the method hooks map by iterating over the list containing the sensorTypeConfigs.
	 */


	/**
	 * Inner class to sort the sensor list according to their priority.
	 * 
	 * @author Patrice Bouillet
	 * 
	 */
	private static class SensorTypeConfigComparator implements Comparator<MethodSensorTypeConfig>, Serializable {

		/**
		 * The generated serial version UID.
		 */
		private static final long serialVersionUID = -2156911328015024777L;

		/**
		 * {@inheritDoc}
		 */
		public int compare(MethodSensorTypeConfig sensorTypeConfig1, MethodSensorTypeConfig sensorTypeConfig2) {
			return sensorTypeConfig2.getPriority().compareTo(sensorTypeConfig1.getPriority());
		}

	}

	/**
	 * Checks if this sensor configuration starts an invocation sequence tracer. This has to be
	 * checked separately.
	 * 
	 * @return If this config starts an invocation sequence tracer.
	 */
	public boolean startsInvocationSequence() {
		return null != invocationSequenceSensorTypeConfig;
	}

	/**
	 * Returns the exception sensor type configuration object. Can be <code>null</code>.
	 * 
	 * @return The exception sensor type configuration.
	 */
	public MethodSensorTypeConfig getExceptionSensorTypeConfig() {
		return exceptionSensorTypeConfig;
	}

	/**
	 * Sets the exception sensor type configuration.
	 * 
	 * @param exceptionSensorTypeConfig
	 *            The exception sensor type configuration.
	 */
	public void setExceptionSensorTypeConfig(MethodSensorTypeConfig exceptionSensorTypeConfig) {
		this.exceptionSensorTypeConfig = exceptionSensorTypeConfig;

		// we need to add the sensor type config separately to the list, because
		// calling sortMethodHooks causes a ClassCastException
		if (!sensorTypeConfigs.contains(this.exceptionSensorTypeConfig)) {
			sensorTypeConfigs.add(this.exceptionSensorTypeConfig);
			sortSensorTypeConfigs();
		}
	}

	/**
	 * Returns the invocation sequence sensor type configuration object. Can be <code>null</code>.
	 * 
	 * @return The invocation sequence sensor type configuration.
	 */
	public MethodSensorTypeConfig getInvocationSequenceSensorTypeConfig() {
		return invocationSequenceSensorTypeConfig;
	}

	/**
	 * Sets the modifiers.
	 * 
	 * @param modifiers
	 *            The int value of the modifiers.
	 */
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * Returns the modifiers.
	 * 
	 * @return The modifiers.
	 */
	public int getModifiers() {
		return modifiers;
	}
	
	//Mine
	public boolean isPropertyAccess() {
		return propertyAccess;
	}
	
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}
	
	public Map<String, Object> getSettings() {
		return settings;
	}

	//Mine

	/**
	 * {@inheritDoc}
	 */
//	public String toString() {
		//return id + " :: " + Modifier.toString(modifiers) + " " + getTargetPackageName() + "." + getTargetClassName() + "#" + getTargetMethodName() + "(" + getParameterTypes() + ")";
	//}

}
