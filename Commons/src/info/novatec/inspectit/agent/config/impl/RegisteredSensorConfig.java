package info.novatec.inspectit.agent.config.impl;

import info.novatec.inspectit.agent.config.PriorityEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

/**
 * Registered sensor config used with the server-side instrumentation.
 * 
 * @author Ivan Senic
 * 
 */
public class RegisteredSensorConfig {

	/**
	 * The method id.
	 */
	private long id;

	/**
	 * List of sensor ids to run on the method.
	 */
	private long[] sensorIds = new long[0];

	/**
	 * Backing array for figuring the priority oder of the sensor ids. Bytes are enough to store
	 * values till 128 so we are safe here.
	 * 
	 * @see #addSensorId(long, PriorityEnum)
	 */
	private transient byte[] sensorPriorities = new byte[0];

	/**
	 * If the invocation should be started.
	 */
	private boolean startsInvocation;

	/**
	 * The FQN name of the target class.
	 */
	private String targetClassFqn;

	/**
	 * The name of the target method.
	 */
	private String targetMethodName;

	/**
	 * The return type of the method.
	 */
	private String returnType;

	/**
	 * The parameter types (as the fully qualified name) of the method.
	 */
	private List<String> parameterTypes;

	/**
	 * Additional settings are stored in this map.
	 */
	private Map<String, Object> settings;
	
	/**
	 * If <code>propertyAccess</code> is set to true, then this list contains at least one element.
	 * The contents is of type {@link PropertyPathStart}.
	 */
	// TODO This can be a Set, we don't want to have equal paths
	// requires hash and equals on the PropertyPathStart
	private List<PropertyPathStart> propertyAccessorList;

	/**
	 * Gets {@link #id}.
	 * 
	 * @return {@link #id}
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets {@link #id}.
	 * 
	 * @param id
	 *            New value for {@link #id}
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets {@link #sensorIds}.
	 * <p>
	 * Note that the ids will be sorted by the sensor priority enumeration, meaning sensor id on
	 * index 0 has the highest priority.
	 * 
	 * @return {@link #sensorIds}
	 */
	public long[] getSensorIds() {
		return sensorIds;
	}

	/**
	 * Adds sensor Id if one does not exists already and properly sorts the id in the
	 * {@link #sensorIds} array based on the priority.
	 * 
	 * @param sensorId
	 *            id to add
	 * @param priorityEnum
	 *            {@link PriorityEnum} of the sensor.
	 * @return true if sensor id has been added, false otherwise
	 */
	public boolean addSensorId(long sensorId, PriorityEnum priorityEnum) {
		// don't add existing ones
		if (containsSensorId(sensorId)) {
			return false;
		}

		// check insert index by priority
		// we want sensor with highest priority to be first, thus we need to negate the ordinal
		// add in addition -1 to avoid having negative zero
		byte priority = (byte) (-1 - priorityEnum.ordinal());
		int index = Math.abs(Arrays.binarySearch(sensorPriorities, priority) + 1);

		// update both arrays
		int length = sensorIds.length;

		long[] updateIds = new long[length + 1];
		System.arraycopy(sensorIds, 0, updateIds, 0, index);
		System.arraycopy(sensorIds, index, updateIds, index + 1, length - index);
		updateIds[index] = sensorId;

		byte[] updatePriority = new byte[length + 1];
		System.arraycopy(sensorPriorities, 0, updatePriority, 0, index);
		System.arraycopy(sensorPriorities, index, updatePriority, index + 1, length - index);
		updatePriority[index] = priority;

		sensorIds = updateIds;
		sensorPriorities = updatePriority;

		return true;
	}

	/**
	 * If sensor if is contained in this {@link RegisteredSensorConfig}.
	 * 
	 * @param sensorId
	 *            sensor id to check
	 * @return <code>true</code> if given sensor id is contained in the
	 *         {@link RegisteredSensorConfig}
	 */
	public boolean containsSensorId(long sensorId) {
		return ArrayUtils.contains(sensorIds, sensorId);
	}

	/**
	 * Gets {@link #startsInvocation}.
	 * 
	 * @return {@link #startsInvocation}
	 */
	public boolean isStartsInvocation() {
		return startsInvocation;
	}

	/**
	 * Sets {@link #startsInvocation}.
	 * 
	 * @param startsInvocation
	 *            New value for {@link #startsInvocation}
	 */
	public void setStartsInvocation(boolean startsInvocation) {
		this.startsInvocation = startsInvocation;
	}

	/**
	 * Gets {@link #targetClassFqn}.
	 * 
	 * @return {@link #targetClassFqn}
	 */
	public String getTargetClassFqn() {
		return targetClassFqn;
	}

	/**
	 * Sets {@link #targetClassFqn}.
	 * 
	 * @param targetClassFqn
	 *            New value for {@link #targetClassFqn}
	 */
	public void setTargetClassFqn(String targetClassFqn) {
		this.targetClassFqn = targetClassFqn;
	}

	/**
	 * Gets {@link #targetMethodName}.
	 * 
	 * @return {@link #targetMethodName}
	 */
	public String getTargetMethodName() {
		return targetMethodName;
	}

	/**
	 * Sets {@link #targetMethodName}.
	 * 
	 * @param targetMethodName
	 *            New value for {@link #targetMethodName}
	 */
	public void setTargetMethodName(String targetMethodName) {
		this.targetMethodName = targetMethodName;
	}

	/**
	 * Gets {@link #returnType}.
	 * 
	 * @return {@link #returnType}
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Sets {@link #returnType}.
	 * 
	 * @param returnType
	 *            New value for {@link #returnType}
	 */
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	/**
	 * Gets {@link #parameterTypes}.
	 * 
	 * @return {@link #parameterTypes}
	 */
	public List<String> getParameterTypes() {
		return parameterTypes;
	}

	/**
	 * Sets {@link #parameterTypes}.
	 * 
	 * @param parameterTypes
	 *            New value for {@link #parameterTypes}
	 */
	public void setParameterTypes(List<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * Gets {@link #settings}.
	 * 
	 * @return {@link #settings}
	 */
	public Map<String, Object> getSettings() {
		return settings;
	}

	/**
	 * Sets {@link #settings}.
	 * 
	 * @param settings
	 *            New value for {@link #settings}
	 */
	public void setSettings(Map<String, Object> settings) {
		this.settings = settings;
	}

	/**
	 * Adds all given settings to the settings map.
	 * 
	 * @param settings
	 *            Map of settings to add.
	 */
	public void addSettings(Map<String, Object> settings) {
		if (null == this.settings) {
			this.settings = new HashMap<String, Object>(settings.size());
		}
		this.settings.putAll(settings);
	}

	/**
	 * Gets {@link #propertyAccessorList}.
	 * 
	 * @return {@link #propertyAccessorList}
	 */
	public List<PropertyPathStart> getPropertyAccessorList() {
		return propertyAccessorList;
	}

	/**
	 * Sets {@link #propertyAccessorList}.
	 * 
	 * @param propertyAccessorList
	 *            New value for {@link #propertyAccessorList}
	 */
	public void setPropertyAccessorList(List<PropertyPathStart> propertyAccessorList) {
		this.propertyAccessorList = propertyAccessorList;
	}
	
	/**
	 * Adds one {@link PropertyPathStart} to the list of the property acc list.
	 * 
	 * @param propertyPathStart
	 *            {@link PropertyPathStart} to add.
	 */
	public void addPropertyAccessor(PropertyPathStart propertyPathStart) {
		if (null == this.propertyAccessorList) {
			this.propertyAccessorList = new ArrayList<PropertyPathStart>(1);
		}
		this.propertyAccessorList.add(propertyPathStart);
	}

	/**
	 * Defines if this sensor configuration contains one or many definitions for a property access
	 * (class field / method parameter) to save.
	 * 
	 * @return <code>true</code> if there is at least one property access
	 */
	public boolean isPropertyAccess() {
		return CollectionUtils.isNotEmpty(propertyAccessorList);
	}

}
