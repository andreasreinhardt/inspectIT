package info.novatec.inspectit.cmr.ci;

import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;

import java.util.Collection;

/**
 * Interface for listeners on the Configuration interface changes.
 * 
 * @author Ivan Senic
 * 
 */
public interface IConfigurationInterfaceChangeListener {

	/**
	 * 
	 * * Informs that the profile is being updated.
	 * 
	 * @param updated
	 *            Updated {@link Profile} instance.
	 * @param removedMethodSensorAssignments
	 *            Removed {@link MethodSensorAssignment}s form the profile.
	 * @param removedExceptionSensorAssignments
	 *            Removed {@link ExceptionSensorAssignment}s form the profile.
	 * @param addedMethodSensorAssignments
	 *            Added {@link MethodSensorAssignment}s form the profile.
	 * @param addedExceptionSensorAssignments
	 *            Added {@link ExceptionSensorAssignment}s form the profile.
	 */
	void profileUpdated(Profile updated, Collection<MethodSensorAssignment> removedMethodSensorAssignments, Collection<ExceptionSensorAssignment> removedExceptionSensorAssignments,
			Collection<MethodSensorAssignment> addedMethodSensorAssignments, Collection<ExceptionSensorAssignment> addedExceptionSensorAssignments);

	/**
	 * Informs that the environment is being updated.
	 * 
	 * @param updated
	 *            Updated {@link Environment} instance.
	 * @param removedProfiles
	 *            Removed {@link Profile}s from the environment.
	 * @param addedProfiles
	 *            Added {@link Profile}s from the environment.
	 */
	void environmentUpdated(Environment updated, Collection<Profile> removedProfiles, Collection<Profile> addedProfiles);

	/**
	 * Signals that the mappings have been updated.
	 */
	void agentMappingsUpdated();
}
