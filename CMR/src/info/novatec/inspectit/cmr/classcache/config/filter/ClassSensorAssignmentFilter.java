package info.novatec.inspectit.cmr.classcache.config.filter;

import info.novatec.inspectit.ci.assignment.AbstractClassSensorAssignment;
import info.novatec.inspectit.classcache.ImmutableAnnotationType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableInterfaceType;
import info.novatec.inspectit.pattern.EqualsMatchPattern;
import info.novatec.inspectit.pattern.IMatchPattern;
import info.novatec.inspectit.pattern.WildcardMatchPattern;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

/**
 * Filter that filters the class types based on the given assignment and vice versa.
 * 
 * @see #matches(AbstractClassSensorAssignment, ImmutableClassType)
 * @author Ivan Senic
 * 
 */
public class ClassSensorAssignmentFilter {

	/**
	 * Tests if the given {@link ImmutableClassType} matches the class sensor assignment.
	 * 
	 * @param classSensorAssignment
	 *            assignment.
	 * @param classType
	 *            classType
	 * @return <code>true</code> if class type matches the assignment.
	 */
	public boolean matches(AbstractClassSensorAssignment<?> classSensorAssignment, ImmutableClassType classType) {
		if (!matchesClassName(classSensorAssignment, classType)) {
			return false;
		}

		if (!matchesAnnotation(classSensorAssignment, classType)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if the {@link AbstractClassSensorAssignment} matches the given
	 * {@link ImmutableClassType} in terms of name specified in the
	 * {@link AbstractClassSensorAssignment}.
	 * 
	 * @param classSensorAssignment
	 *            Assignment defining the name.
	 * @param classType
	 *            Type to check.
	 * @return <code>true</code> if class/super-class/interface match the name pattern as specified
	 *         in the {@link AbstractClassSensorAssignment}.
	 */
	private boolean matchesClassName(AbstractClassSensorAssignment<?> classSensorAssignment, ImmutableClassType classType) {
		// TODO Idea: Save and read pattern directly form the assignment
		String classNamePattern = classSensorAssignment.getClassName();
		IMatchPattern pattern = WildcardMatchPattern.isPattern(classNamePattern) ? new WildcardMatchPattern(classNamePattern) : new EqualsMatchPattern(classNamePattern);

		if (classSensorAssignment.isSuperclass()) {
			// match any superclass
			for (ImmutableClassType superClassType : classType.getImmutableSuperClasses()) {
				if (checkClassAndSuperClassesForName(superClassType, pattern)) {
					return true;
				}
			}
		} else if (classSensorAssignment.isInterf()) {
			// match any interface
			// TODO not sure if realized interfaces include the super-interfaces?
			for (ImmutableInterfaceType interfaceType : classType.getImmutableRealizedInterfaces()) {
				if (pattern.match(interfaceType.getFQN())) {
					return true;
				}
			}
		} else {
			// else match this class
			return pattern.match(classType.getFQN());
		}

		return false;
	}

	/**
	 * Check if the class type or any of its super-classes matches the given name pattern.
	 * 
	 * @param classType
	 *            Type to check.
	 * @param namePattern
	 *            Pattern to test FQN with.
	 * @return <code>true</code> if class or any of the super-classes match the name pattern.
	 */
	private boolean checkClassAndSuperClassesForName(ImmutableClassType classType, IMatchPattern namePattern) {
		if (namePattern.match(classType.getFQN())) {
			return true;
		}

		for (ImmutableClassType superClassType : classType.getImmutableSuperClasses()) {
			if (checkClassAndSuperClassesForName(superClassType, namePattern)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the {@link AbstractClassSensorAssignment} matches the given
	 * {@link ImmutableClassType} in terms of annotation specified in the
	 * {@link AbstractClassSensorAssignment}.
	 * 
	 * @param classSensorAssignment
	 *            Assignment defining the annotations.
	 * @param classType
	 *            Type to check.
	 * @return <code>true</code> if class or any of it super-classes or realized interfaces are
	 *         implementing the specified annotation.
	 */
	private boolean matchesAnnotation(AbstractClassSensorAssignment<?> classSensorAssignment, ImmutableClassType classType) {
		// only check if we have annotation set
		if (StringUtils.isEmpty(classSensorAssignment.getAnnotation())) {
			return true;
		}

		// TODO Idea: Save and read pattern directly form the assignment
		String annotationPattern = classSensorAssignment.getAnnotation();
		IMatchPattern pattern = WildcardMatchPattern.isPattern(annotationPattern) ? new WildcardMatchPattern(annotationPattern) : new EqualsMatchPattern(annotationPattern);

		// check class and super classes first
		if (checkClassAndSuperClassForAnnotation(classType, pattern)) {
			return true;
		}

		// then all interfaces.
		// TODO not sure if realized interfaces include the super-interfaces?
		for (ImmutableInterfaceType interfaceType : classType.getImmutableRealizedInterfaces()) {
			if (checkAnnotations(interfaceType.getImmutableAnnotations(), pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given {@link ImmutableClassType} or any of it's super classes have an
	 * annotation that matches given annotation pattern.
	 * 
	 * @param classType
	 *            Type to check.
	 * @param annotationPattern
	 *            Pattern to test annotation FQNs with.
	 * @return <code>true</code> if class or any super-classes have annotation that matches the
	 *         pattern.
	 */
	private boolean checkClassAndSuperClassForAnnotation(ImmutableClassType classType, IMatchPattern annotationPattern) {
		if (checkAnnotations(classType.getImmutableAnnotations(), annotationPattern)) {
			return true;
		}

		for (ImmutableClassType superClassType : classType.getImmutableSuperClasses()) {
			if (checkClassAndSuperClassForAnnotation(superClassType, annotationPattern)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if any of given {@link ImmutableAnnotationType}s matches the given pattern.
	 * 
	 * @param annotations
	 *            Collection of annotations.
	 * @param pattern
	 *            Pattern to test annotation FQNs with.
	 * @return <code>true</code> if any of given annotations matches the pattern.
	 */
	private boolean checkAnnotations(Collection<? extends ImmutableAnnotationType> annotations, IMatchPattern pattern) {
		for (ImmutableAnnotationType annotationType : annotations) {
			if (pattern.match(annotationType.getFQN())) {
				return true;
			}
		}
		return false;
	}
}
