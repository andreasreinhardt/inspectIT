package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Profile update job that runs for profile update against one environment/class cache.
 * 
 * 
 * @author Ivan Senic
 * 
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy
public class ProfileUpdateJob extends AbstractConfigurationChangeJob {

	/**
	 * Removed {@link MethodSensorAssignment}s during profile update.
	 */
	private Collection<MethodSensorAssignment> removedMethodSensorAssignments = Collections.emptyList();

	/**
	 * Removed {@link ExceptionSensorAssignment}s during profile update.
	 */
	private Collection<ExceptionSensorAssignment> removedExceptionSensorAssignments = Collections.emptyList();

	/**
	 * Added {@link MethodSensorAssignment}s during profile update.
	 */
	private Collection<MethodSensorAssignment> addedMethodSensorAssignments = Collections.emptyList();

	/**
	 * Added {@link ExceptionSensorAssignment}s during profile update.
	 */
	private Collection<ExceptionSensorAssignment> addedExceptionSensorAssignments = Collections.emptyList();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		// first process all removed assignments
		super.processRemovedAssignments(removedMethodSensorAssignments, removedExceptionSensorAssignments);

		// then process all added assignments
		super.processAddedAssignments(addedMethodSensorAssignments, addedExceptionSensorAssignments);
	}

	/**
	 * Sets {@link #removedMethodSensorAssignments}.
	 * 
	 * @param removedMethodSensorAssignments
	 *            New value for {@link #removedMethodSensorAssignments}
	 */
	public void setRemovedMethodSensorAssignments(Collection<MethodSensorAssignment> removedMethodSensorAssignments) {
		this.removedMethodSensorAssignments = removedMethodSensorAssignments;
	}

	/**
	 * Sets {@link #removedExceptionSensorAssignments}.
	 * 
	 * @param removedExceptionSensorAssignments
	 *            New value for {@link #removedExceptionSensorAssignments}
	 */
	public void setRemovedExceptionSensorAssignments(Collection<ExceptionSensorAssignment> removedExceptionSensorAssignments) {
		this.removedExceptionSensorAssignments = removedExceptionSensorAssignments;
	}

	/**
	 * Sets {@link #addedMethodSensorAssignments}.
	 * 
	 * @param addedMethodSensorAssignments
	 *            New value for {@link #addedMethodSensorAssignments}
	 */
	public void setAddedMethodSensorAssignments(Collection<MethodSensorAssignment> addedMethodSensorAssignments) {
		this.addedMethodSensorAssignments = addedMethodSensorAssignments;
	}

	/**
	 * Sets {@link #addedExceptionSensorAssignments}.
	 * 
	 * @param addedExceptionSensorAssignments
	 *            New value for {@link #addedExceptionSensorAssignments}
	 */
	public void setAddedExceptionSensorAssignments(Collection<ExceptionSensorAssignment> addedExceptionSensorAssignments) {
		this.addedExceptionSensorAssignments = addedExceptionSensorAssignments;
	}

}
