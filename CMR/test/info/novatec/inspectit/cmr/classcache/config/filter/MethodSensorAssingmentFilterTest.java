package info.novatec.inspectit.cmr.classcache.config.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableMethodType;
import info.novatec.inspectit.classcache.MethodType.Character;
import info.novatec.inspectit.classcache.Modifiers;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for the {@link MethodSensorAssignmentFilter}.
 * 
 * @author Ivan Senic
 * 
 */
@SuppressWarnings("PMD")
public class MethodSensorAssingmentFilterTest {

	private MethodSensorAssignmentFilter filter;

	@Mock
	private MethodSensorAssignment assignment;

	@Mock
	private ImmutableClassType classType;

	@Mock
	private ImmutableMethodType methodType;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);
		filter = new MethodSensorAssignmentFilter();
	}

	@Test
	public void matchesConstructor() {
		// default
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));

		when(assignment.isConstructor()).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.CONSTRUCTOR);
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesNoConstructor() {
		when(assignment.isConstructor()).thenReturn(false);
		when(methodType.getMethodCharacter()).thenReturn(Character.CONSTRUCTOR);
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesName() {
		// default
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));

		String name = "name";
		when(assignment.getMethodName()).thenReturn(name);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn(name);
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getName()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesNameWildcard() {
		// default
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));

		String name = "n*";
		when(assignment.getMethodName()).thenReturn(name);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getName()).thenReturn("someOtherName");
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesPublicModifiers() {
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PRIVATE));
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesProtectedModifiers() {
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.isProtectedModifier()).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PROTECTED));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PRIVATE));
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesPrivateModifiers() {
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.isPrivateModifier()).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PRIVATE));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));
		assertThat(filter.matches(assignment, methodType), is(false));
	}
	
	@Test
	public void matchesPackageModifiers() {
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.isDefaultModifier()).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(0));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));
		assertThat(filter.matches(assignment, methodType), is(false));
	}

	@Test
	public void matchesParameters() {
		// default
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));

		String param = "param";
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.getParameters()).thenReturn(Collections.singletonList(param));
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");

		when(methodType.getParameters()).thenReturn(Collections.singletonList(param));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getParameters()).thenReturn(Collections.<String> emptyList());
		assertThat(filter.matches(assignment, methodType), is(false));

		when(methodType.getParameters()).thenReturn(Collections.singletonList("someOtherParam"));
		assertThat(filter.matches(assignment, methodType), is(false));

		when(methodType.getParameters()).thenReturn(Arrays.asList(new String[] { "param1", "param2" }));
		assertThat(filter.matches(assignment, methodType), is(false));
		
		when(assignment.getParameters()).thenReturn(Arrays.asList(new String[] { "param2", "param1" }));
		assertThat(filter.matches(assignment, methodType), is(false));

		when(assignment.getParameters()).thenReturn(null);
		assertThat(filter.matches(assignment, methodType), is(true));
	}

	@Test
	public void matchesParametersWildcard() {
		// default
		when(assignment.isPublicModifier()).thenReturn(true);
		when(methodType.getModifiers()).thenReturn(Modifiers.getModifiers(Modifier.PUBLIC));

		String param = "param*";
		when(assignment.getMethodName()).thenReturn("*");
		when(assignment.getParameters()).thenReturn(Collections.singletonList(param));
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getName()).thenReturn("name");

		when(methodType.getParameters()).thenReturn(Collections.singletonList("param1"));
		assertThat(filter.matches(assignment, methodType), is(true));

		when(methodType.getParameters()).thenReturn(Collections.<String> emptyList());
		assertThat(filter.matches(assignment, methodType), is(false));

		when(methodType.getParameters()).thenReturn(Collections.singletonList("someOtherParam"));
		assertThat(filter.matches(assignment, methodType), is(false));

		when(methodType.getParameters()).thenReturn(Arrays.asList(new String[] { "param1", "param2" }));
		assertThat(filter.matches(assignment, methodType), is(false));

		when(assignment.getParameters()).thenReturn(Arrays.asList(new String[] { "param2", "param1" }));
		assertThat(filter.matches(assignment, methodType), is(false));
	}
}
