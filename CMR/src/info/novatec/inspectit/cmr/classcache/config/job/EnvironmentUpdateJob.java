package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * For for an environment update.
 * 
 * @author Ivan Senic
 * 
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy
public class EnvironmentUpdateJob extends AbstractConfigurationChangeJob {

	/**
	 * Environment being updated.
	 */
	private Environment environment;

	/**
	 * List of added profiles.
	 */
	private Collection<Profile> addedProfiles;

	/**
	 * List of removed profiles.
	 */
	private Collection<Profile> removedProfiles;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		// first create new configuration based on new Environment
		updateConfiguration(environment);

		boolean instrumentationUpdated = false;

		// then first process all removed profiles
		if (CollectionUtils.isNotEmpty(removedProfiles)) {
			Collection<MethodSensorAssignment> removedMethodSensorAssignments = new ArrayList<>();
			Collection<ExceptionSensorAssignment> removedExceptionSensorAssignments = new ArrayList<>();

			for (Profile profile : removedProfiles) {
				if (CollectionUtils.isNotEmpty(profile.getMethodSensorAssignments())) {
					removedMethodSensorAssignments.addAll(profile.getMethodSensorAssignments());
				}
				if (CollectionUtils.isNotEmpty(profile.getExceptionSensorAssignments())) {
					removedExceptionSensorAssignments.addAll(profile.getExceptionSensorAssignments());
				}
			}

			instrumentationUpdated |= super.processRemovedAssignments(removedMethodSensorAssignments, removedExceptionSensorAssignments);
		}

		// then process all added profiles
		if (CollectionUtils.isNotEmpty(addedProfiles)) {
			Collection<MethodSensorAssignment> addedMethodSensorAssignments = new ArrayList<>();
			Collection<ExceptionSensorAssignment> addedExceptionSensorAssignments = new ArrayList<>();

			for (Profile profile : addedProfiles) {
				if (CollectionUtils.isNotEmpty(profile.getMethodSensorAssignments())) {
					addedMethodSensorAssignments.addAll(profile.getMethodSensorAssignments());
				}
				if (CollectionUtils.isNotEmpty(profile.getExceptionSensorAssignments())) {
					addedExceptionSensorAssignments.addAll(profile.getExceptionSensorAssignments());
				}
			}

			instrumentationUpdated |= super.processAddedAssignments(addedMethodSensorAssignments, addedExceptionSensorAssignments);
		}


		// if there are instrumentation changes, update time-stamp
		if (instrumentationUpdated) {
			long instrumentationLastModified = System.currentTimeMillis();
			getAgentConfiguration().setInstrumentationLastModified(instrumentationLastModified);
		}
	}

	/**
	 * Sets {@link #environment}.
	 * 
	 * @param environment
	 *            New value for {@link #environment}
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Sets {@link #addedProfiles}.
	 * 
	 * @param addedProfiles
	 *            New value for {@link #addedProfiles}
	 */
	public void setAddedProfiles(Collection<Profile> addedProfiles) {
		this.addedProfiles = addedProfiles;
	}

	/**
	 * Sets {@link #removedProfiles}.
	 * 
	 * @param removedProfiles
	 *            New value for {@link #removedProfiles}
	 */
	public void setRemovedProfiles(Collection<Profile> removedProfiles) {
		this.removedProfiles = removedProfiles;
	}

}
