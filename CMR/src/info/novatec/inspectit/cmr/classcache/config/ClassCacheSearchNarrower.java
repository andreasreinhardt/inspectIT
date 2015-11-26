package info.novatec.inspectit.cmr.classcache.config;

import info.novatec.inspectit.ci.assignment.AbstractClassSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ImmutableAnnotationType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableInterfaceType;
import info.novatec.inspectit.classcache.ImmutableType;
import info.novatec.inspectit.classcache.ImmutableTypeWithAnnotations;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.pattern.WildcardMatchPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * This class help in getting the sub-set of class type that might be instrumented by method or
 * exception assignment.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class ClassCacheSearchNarrower {

	/**
	 * Helps narrowing the search for the possible {@link ImmutableClassType} that might be
	 * instrumented with given {@link MethodSensorAssignment}. Note that this is just a narrow
	 * method, that can select in fast manner the possible candidates. It does not mean that all
	 * candidates are fulfilling all the criteria, nor that the class types contain correct methods
	 * to be instrumented.
	 * 
	 * Search order is following:
	 * 
	 * 1. If direct class name is given, then by name <br>
	 * 2. If annotation is given, then by annotation<br>
	 * 3. If nothing of above, then by the wild card name search.
	 * 
	 * @param classCache
	 *            {@link ClassCache} to look in.
	 * @param methodSensorAssignment
	 *            {@link MethodSensorAssignment}.
	 * @return All initialized {@link ImmutableClassType} that might match given
	 *         {@link MethodSensorAssignment}. Note that this is only narrow process, full check
	 *         must be performed on the returned results.
	 */
	public Collection<? extends ImmutableClassType> narrowByMethodSensorAssignment(ClassCache classCache, MethodSensorAssignment methodSensorAssignment) {
		return narrowByClassSensorAssignment(classCache, methodSensorAssignment, false);
	}

	/**
	 * Helps narrowing the search for the possible {@link ImmutableClassType} that might be
	 * instrumented with given {@link ExceptionSensorAssignment}. Note that this is just a narrow
	 * method, that can select in fast manner the possible candidates. It does not mean that all
	 * candidates are fulfilling all the criteria, nor that the class types contain correct methods
	 * to be instrumented.
	 * 
	 * Search order is following:
	 * 
	 * 1. If direct class name is given, then by name <br>
	 * 2. If annotation is given, then by annotation<br>
	 * 3. If nothing of above, then by the wild card name search.
	 * 
	 * @param classCache
	 *            {@link ClassCache} to look in.
	 * @param exceptionSensorAssignment
	 *            {@link MethodSensorAssignment}.
	 * @return All initialized {@link ImmutableClassType} that might match given
	 *         {@link ExceptionSensorAssignment} and are exception types. Note that this is only
	 *         narrow process, full check must be performed on the returned results.
	 */
	public Collection<? extends ImmutableClassType> narrowByExceptionSensorAssignment(ClassCache classCache, ExceptionSensorAssignment exceptionSensorAssignment) {
		return narrowByClassSensorAssignment(classCache, exceptionSensorAssignment, true);
	}

	/**
	 * Private narrow method for joined functionality.
	 * 
	 * @param classCache
	 *            {@link ClassCache} to look in.
	 * @param classSensorAssignment
	 *            {@link AbstractClassSensorAssignment}
	 * @param onlyExceptions
	 *            if only exception types should be included
	 * @return All initialized {@link ImmutableClassType} that might match given
	 *         {@link AbstractClassSensorAssignment}. Note that this is only narrow process, full
	 *         check must be performed on the returned results.
	 */
	private Collection<? extends ImmutableClassType> narrowByClassSensorAssignment(ClassCache classCache, AbstractClassSensorAssignment<?> classSensorAssignment, boolean onlyExceptions) {
		if (!WildcardMatchPattern.isPattern(classSensorAssignment.getClassName())) {
			// if we don't have a pattern that just load
			return narrowByNameSearch(classCache, classSensorAssignment.getClassName(), classSensorAssignment.isInterf(), classSensorAssignment.isSuperclass(), onlyExceptions);
		}

		if (null != classSensorAssignment.getAnnotation()) {
			return narrowByAnnotationSearch(classCache, classSensorAssignment.getAnnotation(), onlyExceptions);
		}

		// if nothing works then we have wild-card search in name
		return narrowByNameSearch(classCache, classSensorAssignment.getClassName(), classSensorAssignment.isInterf(), classSensorAssignment.isSuperclass(), onlyExceptions);
	}

	/**
	 * Narrows the search by the class name defined in the {@link AbstractClassSensorAssignment} and
	 * includes the interface/super-class options as well.
	 * 
	 * @param classCache
	 *            {@link ClassCache} to look in.
	 * @param className
	 *            Class name to search for
	 * @param isInterface
	 *            if class name is related to interface
	 * @param isSuperClass
	 *            if class name is releated to superclass
	 * @param onlyExceptions
	 *            if only exception types should be included
	 * @return All initialized {@link ImmutableClassType} that might match given
	 *         {@link MethodSensorAssignment}. Note that this is only narrow process, full check
	 *         must be performed on the returned results.
	 */
	private Collection<? extends ImmutableClassType> narrowByNameSearch(ClassCache classCache, String className, boolean isInterface, boolean isSuperClass, boolean onlyExceptions) {
		if (isInterface) {
			// if definition is for interface, load all matching interfaces
			Collection<? extends ImmutableInterfaceType> interfaceTypes = classCache.getLookupService().findInterfaceTypesByPattern(className, false);

			if (CollectionUtils.isEmpty(interfaceTypes)) {
				return Collections.emptyList();
			}

			// then load initialized realizing classes from all interfaces
			Collection<ImmutableClassType> results = new ArrayList<>();
			for (ImmutableInterfaceType interfaceType : interfaceTypes) {
				collectClassesFromInterfaceAndSubInterfaces(results, interfaceType, onlyExceptions);
			}
			return results;
		} else if (isSuperClass) {
			// if definition is for superclass, load all matching classes
			Collection<? extends ImmutableClassType> superClassTypes = classCache.getLookupService().findClassTypesByPattern(className, false);

			if (CollectionUtils.isEmpty(superClassTypes)) {
				return Collections.emptyList();
			}

			// then load initialized sub-classes from all super types
			Collection<ImmutableClassType> results = new ArrayList<>();
			for (ImmutableClassType superClassType : superClassTypes) {
				collectClassesFromSuperClassAndSubSuperClasses(results, superClassType, onlyExceptions);
			}
			return results;
		} else {
			// otherwise just search for classes
			if (onlyExceptions) {
				return classCache.getLookupService().findExceptionTypesByPattern(className, true);
			} else {
				return classCache.getLookupService().findClassTypesByPattern(className, true);
			}
		}
	}

	/**
	 * Narrows the search by the annotation defined in the assignment.
	 * 
	 * @param classCache
	 *            {@link ClassCache} to look in.
	 * @param annotation
	 *            Annotation FQN
	 * @param onlyExceptions
	 *            if only exception types should be included
	 * @return All initialized {@link ImmutableClassType} that might match given annotation. Note
	 *         that this is only narrow process, full check must be performed on the returned
	 *         results.
	 */
	private Collection<? extends ImmutableClassType> narrowByAnnotationSearch(ClassCache classCache, String annotation, boolean onlyExceptions) {
		if (null == annotation) {
			return Collections.emptyList();
		}

		Collection<? extends ImmutableAnnotationType> annotationTypes = classCache.getLookupService().findAnnotationTypesByPattern(annotation, false);

		if (CollectionUtils.isEmpty(annotationTypes)) {
			return Collections.emptyList();
		}

		// then load initialized sub-classes from all super types
		Collection<ImmutableClassType> results = new ArrayList<>();
		for (ImmutableAnnotationType annotationType : annotationTypes) {
			for (ImmutableTypeWithAnnotations typeWithAnnotations : annotationType.getImmutableAnnotatedTypes()) {
				if (typeWithAnnotations instanceof ImmutableType) {
					ImmutableType immutableType = (ImmutableType) typeWithAnnotations;
					if (immutableType.isClass()) {
						ImmutableClassType immutableClassType = immutableType.castToClass();
						if (immutableClassType.isInitialized() && !(onlyExceptions && !immutableClassType.isException())) {
							results.add(immutableClassType);
						}
					}
				}
			}
		}

		return results;
	}

	/**
	 * Collects all realizing classes that implement given interface or any of its sub-interfaces
	 * and adds them to the given results list. This method is recursive.
	 * 
	 * @param results
	 *            List to store classes to.
	 * @param interfaceType
	 *            Type to check.
	 * @param onlyExceptions
	 *            if only exception types should be included
	 */
	private void collectClassesFromInterfaceAndSubInterfaces(Collection<ImmutableClassType> results, ImmutableInterfaceType interfaceType, boolean onlyExceptions) {
		for (ImmutableClassType classType : interfaceType.getImmutableRealizingClasses()) {
			if (classType.isInitialized() && !(onlyExceptions && !classType.isException())) {
				results.add(classType);
			}
		}

		for (ImmutableInterfaceType superInterfaceType : interfaceType.getImmutableSubInterfaces()) {
			collectClassesFromInterfaceAndSubInterfaces(results, superInterfaceType, onlyExceptions);
		}
	}

	/**
	 * Collects all realizing classes that implement given interface or any of its sub-interfaces
	 * and adds them to the given results list. This method is recursive.
	 * 
	 * @param results
	 *            List to store classes to.
	 * @param classType
	 *            Type to check.
	 * @param onlyExceptions
	 *            if only exception types should be included
	 */
	private void collectClassesFromSuperClassAndSubSuperClasses(Collection<ImmutableClassType> results, ImmutableClassType classType, boolean onlyExceptions) {
		for (ImmutableClassType superClassType : classType.getImmutableSubClasses()) {
			if (superClassType.isInitialized() && !(onlyExceptions && !superClassType.isException())) {
				results.add(superClassType);
			}
			collectClassesFromSuperClassAndSubSuperClasses(results, superClassType, onlyExceptions);
		}
	}

}
