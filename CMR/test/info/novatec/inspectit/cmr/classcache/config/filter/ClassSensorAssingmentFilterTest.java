package info.novatec.inspectit.cmr.classcache.config.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.ci.assignment.AbstractClassSensorAssignment;
import info.novatec.inspectit.classcache.ImmutableAnnotationType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableInterfaceType;

import java.util.Collections;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for the {@link ClassSensorAssignmentFilter}.
 * 
 * @author Ivan Senic
 * 
 */
public class ClassSensorAssingmentFilterTest {

	private ClassSensorAssignmentFilter filter;

	@Mock
	private AbstractClassSensorAssignment<?> assignment;

	@Mock
	private ImmutableClassType classType;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);
		filter = new ClassSensorAssignmentFilter();
	}

	@Test
	public void matchesName() {
		String name = "name";
		when(assignment.getClassName()).thenReturn(name);
		when(classType.getFQN()).thenReturn(name);
		assertThat(filter.matches(assignment, classType), is(true));

		when(classType.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesNameWildCard() {
		String wildCard = "nam*";
		when(assignment.getClassName()).thenReturn(wildCard);
		when(classType.getFQN()).thenReturn("name");
		assertThat(filter.matches(assignment, classType), is(true));

		when(classType.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesSuperClassName() {
		String name = "name";
		when(assignment.getClassName()).thenReturn(name);
		when(assignment.isSuperclass()).thenReturn(true);

		
		ImmutableClassType superClass = Mockito.mock(ImmutableClassType.class);
		when(superClass.getFQN()).thenReturn(name);
		
		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(superClass)).when(classType).getImmutableSuperClasses();
		assertThat(filter.matches(assignment, classType), is(true));

		when(superClass.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesInterfaceName() {
		String name = "name";
		when(assignment.getClassName()).thenReturn(name);
		when(assignment.isInterf()).thenReturn(true);

		ImmutableInterfaceType interf = Mockito.mock(ImmutableInterfaceType.class);
		when(interf.getFQN()).thenReturn(name);

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(interf)).when(classType).getImmutableRealizedInterfaces();
		assertThat(filter.matches(assignment, classType), is(true));

		when(interf.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesSuperSuperClassWildCard() {
		String name = "*name";
		when(assignment.getClassName()).thenReturn(name);
		when(assignment.isSuperclass()).thenReturn(true);

		ImmutableClassType superSuperClass = Mockito.mock(ImmutableClassType.class);
		when(superSuperClass.getFQN()).thenReturn("name");

		ImmutableClassType superClass = Mockito.mock(ImmutableClassType.class);
		when(superClass.getFQN()).thenReturn("someOtherSuperName");
		doReturn(Collections.singleton(superSuperClass)).when(superClass).getImmutableSuperClasses();

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(superClass)).when(classType).getImmutableSuperClasses();

		assertThat(filter.matches(assignment, classType), is(true));

		when(superSuperClass.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesAnnotation() {
		String name = "name";
		when(assignment.getClassName()).thenReturn("*");
		when(assignment.getAnnotation()).thenReturn(name);

		ImmutableAnnotationType annotation = Mockito.mock(ImmutableAnnotationType.class);
		when(annotation.getFQN()).thenReturn(name);

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(annotation)).when(classType).getImmutableAnnotations();
		assertThat(filter.matches(assignment, classType), is(true));

		when(annotation.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesAnnotationWildCard() {
		String name = "n*me";
		when(assignment.getClassName()).thenReturn("*");
		when(assignment.getAnnotation()).thenReturn(name);

		ImmutableAnnotationType annotation = Mockito.mock(ImmutableAnnotationType.class);
		when(annotation.getFQN()).thenReturn(name);

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(annotation)).when(classType).getImmutableAnnotations();
		assertThat(filter.matches(assignment, classType), is(true));

		when(annotation.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesSuperClassAnnotation() {
		String name = "name";
		when(assignment.getClassName()).thenReturn("*");
		when(assignment.getAnnotation()).thenReturn(name);

		ImmutableAnnotationType annotation = Mockito.mock(ImmutableAnnotationType.class);
		when(annotation.getFQN()).thenReturn(name);

		ImmutableClassType superClass = Mockito.mock(ImmutableClassType.class);
		when(superClass.getFQN()).thenReturn("someOtherName");

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(superClass)).when(classType).getImmutableSuperClasses();
		doReturn(Collections.singleton(annotation)).when(superClass).getImmutableAnnotations();
		assertThat(filter.matches(assignment, classType), is(true));

		when(annotation.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void matchesInterfaceAnnotation() {
		String name = "name";
		when(assignment.getClassName()).thenReturn("*");
		when(assignment.getAnnotation()).thenReturn(name);

		ImmutableAnnotationType annotation = Mockito.mock(ImmutableAnnotationType.class);
		when(annotation.getFQN()).thenReturn(name);

		ImmutableInterfaceType interf = Mockito.mock(ImmutableInterfaceType.class);
		when(interf.getFQN()).thenReturn("someOtherName");

		when(classType.getFQN()).thenReturn("someOtherName");
		doReturn(Collections.singleton(interf)).when(classType).getImmutableRealizedInterfaces();
		doReturn(Collections.singleton(annotation)).when(interf).getImmutableAnnotations();
		assertThat(filter.matches(assignment, classType), is(true));

		when(annotation.getFQN()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void assignmentNull() {
		when(assignment.getClassName()).thenReturn(null);
		when(classType.getFQN()).thenReturn("name");
		assertThat(filter.matches(assignment, classType), is(false));
	}

	@Test
	public void nameNull() {
		when(assignment.getClassName()).thenReturn("name");
		when(classType.getFQN()).thenReturn(null);
		assertThat(filter.matches(assignment, classType), is(false));
	}
}
