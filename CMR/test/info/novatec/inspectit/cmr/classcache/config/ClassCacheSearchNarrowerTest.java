package info.novatec.inspectit.cmr.classcache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableAnnotationType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableInterfaceType;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.ClassCacheLookup;

import java.util.Collection;
import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ClassCacheSearchNarrowerTest {

	private ClassCacheSearchNarrower narrower;

	@Mock
	private MethodSensorAssignment methodSensorAssignment;

	@Mock
	private ExceptionSensorAssignment exceptionSensorAssignment;

	@Mock
	private ClassCache classCache;

	@Mock
	private ClassCacheLookup lookup;

	@Mock
	private ClassType classType;

	@Mock
	private ImmutableClassType superClassType;

	@Mock
	private ImmutableInterfaceType interfaceType;

	@Mock
	private ImmutableAnnotationType annotationType;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);
		when(classCache.getLookupService()).thenReturn(lookup);
		when(classType.isClass()).thenReturn(true);
		when(classType.castToClass()).thenReturn(classType);

		narrower = new ClassCacheSearchNarrower();
	}

	@Test
	public void classByDirectName() {
		String className = "info.novatec.MyClass";
		when(methodSensorAssignment.getClassName()).thenReturn(className);
		when(methodSensorAssignment.isInterf()).thenReturn(false);
		when(methodSensorAssignment.isSuperclass()).thenReturn(false);

		doReturn(Collections.singleton(classType)).when(lookup).findClassTypesByPattern(eq(className), anyBoolean());

		Collection<? extends ImmutableClassType> result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(1)).findClassTypesByPattern(className, true);
		verifyNoMoreInteractions(lookup);
	}

	@Test
	public void classByInterface() {
		String interfaceName = "info.novatec.MyClass";
		when(methodSensorAssignment.getClassName()).thenReturn(interfaceName);
		when(methodSensorAssignment.isInterf()).thenReturn(true);
		when(methodSensorAssignment.isSuperclass()).thenReturn(false);

		doReturn(Collections.singleton(interfaceType)).when(lookup).findInterfaceTypesByPattern(eq(interfaceName), anyBoolean());
		doReturn(Collections.singleton(classType)).when(interfaceType).getImmutableRealizingClasses();

		when(classType.isInitialized()).thenReturn(false);
		when(classType.isException()).thenReturn(false);
		assertThat(narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment), is(empty()));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(true);
		Collection<? extends ImmutableClassType> result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(false);
		result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(3)).findInterfaceTypesByPattern(interfaceName, false);
		verifyNoMoreInteractions(lookup);
	}

	@Test
	public void classBySuperClass() {
		String superClassName = "info.novatec.MyClass";
		when(methodSensorAssignment.getClassName()).thenReturn(superClassName);
		when(methodSensorAssignment.isInterf()).thenReturn(false);
		when(methodSensorAssignment.isSuperclass()).thenReturn(true);

		doReturn(Collections.singleton(superClassType)).when(lookup).findClassTypesByPattern(eq(superClassName), anyBoolean());
		doReturn(Collections.singleton(classType)).when(superClassType).getImmutableSubClasses();

		when(classType.isInitialized()).thenReturn(false);
		when(classType.isException()).thenReturn(false);
		assertThat(narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment), is(empty()));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(true);
		Collection<? extends ImmutableClassType> result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(false);
		result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(3)).findClassTypesByPattern(superClassName, false);
		verifyNoMoreInteractions(lookup);
	}

	@Test
	public void classByAnnotation() {
		String className = "*";
		String annotationName = "info.novatec.MyAnnotation";
		when(methodSensorAssignment.getClassName()).thenReturn(className);
		when(methodSensorAssignment.isInterf()).thenReturn(false);
		when(methodSensorAssignment.isSuperclass()).thenReturn(false);
		when(methodSensorAssignment.getAnnotation()).thenReturn(annotationName);

		doReturn(Collections.singleton(annotationType)).when(lookup).findAnnotationTypesByPattern(eq(annotationName), anyBoolean());
		doReturn(Collections.singleton(classType)).when(annotationType).getImmutableAnnotatedTypes();

		when(classType.isInitialized()).thenReturn(false);
		when(classType.isException()).thenReturn(false);
		assertThat(narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment), is(empty()));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(true);
		Collection<? extends ImmutableClassType> result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(false);
		result = narrower.narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(3)).findAnnotationTypesByPattern(annotationName, false);
		verifyNoMoreInteractions(lookup);
	}
	
	@Test
	public void exceptionByName() {
		String className = "info.novatec.MyException";
		when(exceptionSensorAssignment.getClassName()).thenReturn(className);
		when(exceptionSensorAssignment.isInterf()).thenReturn(false);
		when(exceptionSensorAssignment.isSuperclass()).thenReturn(false);

		doReturn(Collections.singleton(classType)).when(lookup).findExceptionTypesByPattern(eq(className), anyBoolean());

		Collection<? extends ImmutableClassType> result = narrower.narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(1)).findExceptionTypesByPattern(className, true);
		verifyNoMoreInteractions(lookup);
	}

	@Test
	public void exceptionByAnnotation() {
		String className = "*";
		String annotationName = "info.novatec.MyAnnotation";
		when(exceptionSensorAssignment.getClassName()).thenReturn(className);
		when(exceptionSensorAssignment.isInterf()).thenReturn(false);
		when(exceptionSensorAssignment.isSuperclass()).thenReturn(false);
		when(exceptionSensorAssignment.getAnnotation()).thenReturn(annotationName);

		doReturn(Collections.singleton(annotationType)).when(lookup).findAnnotationTypesByPattern(eq(annotationName), anyBoolean());
		doReturn(Collections.singleton(classType)).when(annotationType).getImmutableAnnotatedTypes();

		when(classType.isInitialized()).thenReturn(false);
		when(classType.isException()).thenReturn(true);
		assertThat(narrower.narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment), is(empty()));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(false);
		assertThat(narrower.narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment), is(empty()));

		when(classType.isInitialized()).thenReturn(true);
		when(classType.isException()).thenReturn(true);
		Collection<? extends ImmutableClassType> result = narrower.narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((ImmutableClassType) classType));

		verify(lookup, times(3)).findAnnotationTypesByPattern(annotationName, false);
		verifyNoMoreInteractions(lookup);
	}
}
