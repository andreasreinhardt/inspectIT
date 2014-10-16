package info.novatec.inspectit.cmr.classcache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.PriorityEnum;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.ExceptionSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.MethodSensorTypeConfig;
import info.novatec.inspectit.agent.config.impl.PropertyPathStart;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.AbstractClassSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.TimerMethodSensorAssignment;
import info.novatec.inspectit.ci.context.AbstractContextCapture;
import info.novatec.inspectit.ci.sensor.method.IMethodSensorConfig;
import info.novatec.inspectit.ci.sensor.method.impl.InvocationSequenceSensorConfig;
import info.novatec.inspectit.ci.strategy.IStrategyConfig;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.MethodType;
import info.novatec.inspectit.classcache.MethodType.Character;
import info.novatec.inspectit.cmr.classcache.config.filter.ClassSensorAssignmentFilter;
import info.novatec.inspectit.cmr.classcache.config.filter.MethodSensorAssignmentFilter;
import info.novatec.inspectit.cmr.service.IRegistrationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class InstrumentationCreatorTest {

	private InstrumentationCreator creator;

	@Mock
	private Environment environment;

	@Mock
	private IRegistrationService registrationService;

	@Mock
	private ClassType classType;

	@Mock
	private MethodType methodType;

	@Mock
	private MethodSensorAssignmentFilter methodFilter;

	@Mock
	private ClassSensorAssignmentFilter classFilter;

	@Mock
	private ConfigurationResolver configurationResolver;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);
		creator = new InstrumentationCreator();
		creator.registrationService = registrationService;
		creator.methodFilter = methodFilter;
		creator.classFilter = classFilter;
		creator.configurationResolver = configurationResolver;

		// mock strategies
		when(environment.getSendingStrategyConfig()).thenReturn(mock(IStrategyConfig.class));
		when(environment.getBufferStrategyConfig()).thenReturn(mock(IStrategyConfig.class));

		// filters to true by default
		when(methodFilter.matches(Mockito.<MethodSensorAssignment> any(), Mockito.<MethodType> any())).thenReturn(true);
		when(classFilter.matches(Mockito.<AbstractClassSensorAssignment<?>> any(), Mockito.<ClassType> any())).thenReturn(true);

		// class to return one method
		when(classType.getMethods()).thenReturn(Collections.singleton(methodType));
	}

	@Test
	public void methodSensorAssignment() throws Exception {
		long agentId = 13L;
		long sensorId = 15L;
		long methodId = 17L;
		String sensorClassName = "sensorClassName";
		when(registrationService.registerMethodIdent(eq(agentId), anyString(), anyString(), anyString(), Mockito.<List<String>> any(), anyString(), anyInt())).thenReturn(methodId);

		MethodSensorTypeConfig methodSensorTypeConfig = mock(MethodSensorTypeConfig.class);
		when(methodSensorTypeConfig.getId()).thenReturn(sensorId);
		when(methodSensorTypeConfig.getPriority()).thenReturn(PriorityEnum.NORMAL);

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(agentConfiguration.getPlatformId()).thenReturn(agentId);
		when(agentConfiguration.getMethodSensorTypeConfig(sensorClassName)).thenReturn(methodSensorTypeConfig);

		Map<String, Object> settings = Collections.<String, Object> singletonMap("key", "value");
		MethodSensorAssignment methodSensorAssignment = mock(MethodSensorAssignment.class);
		when(methodSensorAssignment.getSettings()).thenReturn(settings);
		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(Collections.singletonList(methodSensorAssignment));

		IMethodSensorConfig methodSensorConfig = mock(IMethodSensorConfig.class);
		when(methodSensorConfig.getClassName()).thenReturn(sensorClassName);
		when(environment.getMethodSensorTypeConfig(Mockito.<Class<? extends IMethodSensorConfig>> any())).thenReturn(methodSensorConfig);

		String packageName = "my.favorite.package";
		String className = "ClassName";
		String methodName = "methodName";
		String returnType = "returnType";
		List<String> parameters = Arrays.asList(new String[] { "p1", "p2" });
		int mod = 10;
		when(classType.getFQN()).thenReturn(packageName + '.' + className);
		when(methodType.getName()).thenReturn(methodName);
		when(methodType.getParameters()).thenReturn(parameters);
		when(methodType.getReturnType()).thenReturn(returnType);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getModifiers()).thenReturn(mod);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);

		// verify registration service
		verify(registrationService, times(1)).registerMethodIdent(agentId, packageName, className, methodName, parameters, returnType, mod);
		verify(registrationService, times(1)).addSensorTypeToMethod(sensorId, methodId);
		verifyNoMoreInteractions(registrationService);

		// check RSC
		ArgumentCaptor<RegisteredSensorConfig> captor = ArgumentCaptor.forClass(RegisteredSensorConfig.class);
		verify(methodType, times(1)).setRegisteredSensorConfig(captor.capture());

		RegisteredSensorConfig rsc = captor.getValue();
		assertThat(rsc.getId(), is(methodId));
		assertThat(rsc.getSensorIds().length, is(1));
		assertThat(rsc.getSensorIds()[0], is(sensorId));
		assertThat(rsc.getTargetClassFqn(), is(packageName + '.' + className));
		assertThat(rsc.getTargetMethodName(), is(methodName));
		assertThat(rsc.getReturnType(), is(returnType));
		assertThat(rsc.getParameterTypes(), is(parameters));
		assertThat(rsc.getSettings(), is(settings));
	}

	@Test
	public void timerSensorAssignment() throws Exception {
		long agentId = 13L;
		long sensorId = 15L;
		long methodId = 17L;
		long invocationSensorId = 19L;
		String sensorClassName = "sensorClassName";
		String invocClassName = "invocClassName";
		when(registrationService.registerMethodIdent(eq(agentId), anyString(), anyString(), anyString(), Mockito.<List<String>> any(), anyString(), anyInt())).thenReturn(methodId);

		MethodSensorTypeConfig methodSensorTypeConfig = mock(MethodSensorTypeConfig.class);
		when(methodSensorTypeConfig.getId()).thenReturn(sensorId);
		when(methodSensorTypeConfig.getPriority()).thenReturn(PriorityEnum.NORMAL);

		MethodSensorTypeConfig invocSensorTypeConfig = mock(MethodSensorTypeConfig.class);
		when(invocSensorTypeConfig.getId()).thenReturn(invocationSensorId);
		when(invocSensorTypeConfig.getPriority()).thenReturn(PriorityEnum.INVOC);

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(agentConfiguration.getPlatformId()).thenReturn(agentId);
		when(agentConfiguration.getMethodSensorTypeConfig(sensorClassName)).thenReturn(methodSensorTypeConfig);
		when(agentConfiguration.getMethodSensorTypeConfig(invocClassName)).thenReturn(invocSensorTypeConfig);

		TimerMethodSensorAssignment methodSensorAssignment = mock(TimerMethodSensorAssignment.class);
		when(methodSensorAssignment.isStartsInvocation()).thenReturn(true);
		AbstractContextCapture contextCapture = mock(AbstractContextCapture.class);
		PropertyPathStart propertyPathStart = mock(PropertyPathStart.class);
		when(contextCapture.getPropertyPathStart()).thenReturn(propertyPathStart);
		when(methodSensorAssignment.getContextCaptures()).thenReturn(Collections.singletonList(contextCapture));
		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(Collections.<MethodSensorAssignment> singletonList(methodSensorAssignment));

		IMethodSensorConfig methodSensorConfig = mock(IMethodSensorConfig.class);
		when(methodSensorConfig.getClassName()).thenReturn(sensorClassName);
		when(environment.getMethodSensorTypeConfig(Mockito.<Class<? extends IMethodSensorConfig>> any())).thenReturn(methodSensorConfig);

		IMethodSensorConfig invocSensorConfig = mock(IMethodSensorConfig.class);
		when(invocSensorConfig.getClassName()).thenReturn(invocClassName);
		when(environment.getMethodSensorTypeConfig(InvocationSequenceSensorConfig.class)).thenReturn(invocSensorConfig);

		String packageName = "my.favorite.package";
		String className = "ClassName";
		String methodName = "methodName";
		String returnType = "returnType";
		List<String> parameters = Arrays.asList(new String[] { "p1", "p2" });
		int mod = 10;
		when(classType.getFQN()).thenReturn(packageName + '.' + className);
		when(methodType.getName()).thenReturn(methodName);
		when(methodType.getParameters()).thenReturn(parameters);
		when(methodType.getReturnType()).thenReturn(returnType);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		when(methodType.getModifiers()).thenReturn(mod);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);

		// verify registration service
		verify(registrationService, times(1)).registerMethodIdent(agentId, packageName, className, methodName, parameters, returnType, mod);
		verify(registrationService, times(1)).addSensorTypeToMethod(sensorId, methodId);
		verify(registrationService, times(1)).addSensorTypeToMethod(invocationSensorId, methodId);
		verifyNoMoreInteractions(registrationService);

		// check RSC
		ArgumentCaptor<RegisteredSensorConfig> captor = ArgumentCaptor.forClass(RegisteredSensorConfig.class);
		verify(methodType, times(1)).setRegisteredSensorConfig(captor.capture());

		// just check related to the timer sensor stuff
		RegisteredSensorConfig rsc = captor.getValue();
		assertThat(rsc.getId(), is(methodId));
		assertThat(rsc.getSensorIds().length, is(2));
		assertThat(rsc.getSensorIds()[0], is(sensorId));
		assertThat(rsc.getSensorIds()[1], is(invocationSensorId));
		assertThat(rsc.isStartsInvocation(), is(true));
		assertThat(rsc.isPropertyAccess(), is(true));
		assertThat(rsc.getPropertyAccessorList(), hasItem(propertyPathStart));

	}

	@Test
	public void exceptionSensorAssignment() throws Exception {
		long agentId = 13L;
		long sensorId = 15L;
		long methodId = 17L;
		when(registrationService.registerMethodIdent(eq(agentId), anyString(), anyString(), anyString(), Mockito.<List<String>> any(), anyString(), anyInt())).thenReturn(methodId);

		ExceptionSensorTypeConfig exceptionSensorTypeConfig = mock(ExceptionSensorTypeConfig.class);
		when(exceptionSensorTypeConfig.getId()).thenReturn(sensorId);
		when(exceptionSensorTypeConfig.getPriority()).thenReturn(PriorityEnum.NORMAL);

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(agentConfiguration.getPlatformId()).thenReturn(agentId);
		when(agentConfiguration.getExceptionSensorTypeConfig()).thenReturn(exceptionSensorTypeConfig);

		Map<String, Object> settings = Collections.<String, Object> singletonMap("key", "value");
		ExceptionSensorAssignment exceptionSensorAssignment = mock(ExceptionSensorAssignment.class);
		when(exceptionSensorAssignment.getSettings()).thenReturn(settings);
		when(configurationResolver.getAllExceptionSensorAssignments(environment)).thenReturn(Collections.singletonList(exceptionSensorAssignment));

		String packageName = "my.favorite.package";
		String className = "ClassName";
		String methodName = "<init>";
		String returnType = "returnType";
		List<String> parameters = Arrays.asList(new String[] {});
		int mod = 10;
		when(classType.getFQN()).thenReturn(packageName + '.' + className);
		when(methodType.getName()).thenReturn(methodName);
		when(methodType.getParameters()).thenReturn(parameters);
		when(methodType.getReturnType()).thenReturn(returnType);
		when(methodType.getMethodCharacter()).thenReturn(Character.CONSTRUCTOR);
		when(methodType.getModifiers()).thenReturn(mod);
		when(classType.isException()).thenReturn(true);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);

		// verify registration service
		// for constructors the registered method name is class name
		verify(registrationService, times(1)).registerMethodIdent(agentId, packageName, className, className, parameters, returnType, mod);
		verify(registrationService, times(1)).addSensorTypeToMethod(sensorId, methodId);
		verifyNoMoreInteractions(registrationService);

		// check RSC
		ArgumentCaptor<RegisteredSensorConfig> captor = ArgumentCaptor.forClass(RegisteredSensorConfig.class);
		verify(methodType, times(1)).setRegisteredSensorConfig(captor.capture());

		RegisteredSensorConfig rsc = captor.getValue();
		assertThat(rsc.getId(), is(methodId));
		assertThat(rsc.getSensorIds().length, is(1));
		assertThat(rsc.getSensorIds()[0], is(sensorId));
		assertThat(rsc.getTargetClassFqn(), is(packageName + '.' + className));
		assertThat(rsc.getTargetMethodName(), is(methodName));
		assertThat(rsc.getReturnType(), is(returnType));
		assertThat(rsc.getParameterTypes(), is(parameters));
		assertThat(rsc.getSettings(), is(settings));
	}

	@Test
	public void rscAndSensorAlreadyExist() throws Exception {
		long agentId = 13L;
		long sensorId = 15L;
		String sensorClassName = "sensorClassName";

		MethodSensorTypeConfig methodSensorTypeConfig = mock(MethodSensorTypeConfig.class);
		when(methodSensorTypeConfig.getId()).thenReturn(sensorId);
		when(methodSensorTypeConfig.getPriority()).thenReturn(PriorityEnum.NORMAL);

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(agentConfiguration.getPlatformId()).thenReturn(agentId);
		when(agentConfiguration.getMethodSensorTypeConfig(sensorClassName)).thenReturn(methodSensorTypeConfig);

		Map<String, Object> settings = Collections.<String, Object> singletonMap("key", "value");
		MethodSensorAssignment methodSensorAssignment = mock(MethodSensorAssignment.class);
		when(methodSensorAssignment.getSettings()).thenReturn(settings);
		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(Collections.singletonList(methodSensorAssignment));

		IMethodSensorConfig methodSensorConfig = mock(IMethodSensorConfig.class);
		when(methodSensorConfig.getClassName()).thenReturn(sensorClassName);
		when(environment.getMethodSensorTypeConfig(Mockito.<Class<? extends IMethodSensorConfig>> any())).thenReturn(methodSensorConfig);

		RegisteredSensorConfig rsc = mock(RegisteredSensorConfig.class);
		when(rsc.getSensorIds()).thenReturn(new long[] { sensorId });
		when(methodType.getRegisteredSensorConfig()).thenReturn(rsc);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);

		// verify no interaction
		verifyNoMoreInteractions(registrationService);
		verify(methodType, times(0)).setRegisteredSensorConfig(Mockito.<RegisteredSensorConfig> any());
	}

	@Test
	public void assignmentDoesNotMatchClassFilter() throws Exception {
		MethodSensorAssignment methodSensorAssignment = mock(MethodSensorAssignment.class);
		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(Collections.singletonList(methodSensorAssignment));

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(classFilter.matches(methodSensorAssignment, classType)).thenReturn(false);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);
		verifyZeroInteractions(registrationService, methodType);
	}

	@Test
	public void assignmentDoesNotMatchMethodFilter() throws Exception {
		MethodSensorAssignment methodSensorAssignment = mock(MethodSensorAssignment.class);
		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(Collections.singletonList(methodSensorAssignment));

		AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
		when(methodFilter.matches(methodSensorAssignment, methodType)).thenReturn(false);

		creator.addInstrumentationPoints(agentConfiguration, environment, classType);
		verifyZeroInteractions(registrationService, methodType);
	}

	@Test
	public void removeAllInstrumentationPoints() {
		when(classType.getMethods()).thenReturn(Collections.singleton(methodType));

		// something to remove
		when(classType.hasInstrumentationPoints()).thenReturn(true);
		assertThat(creator.removeInstrumentationPoints(classType), is(true));
		verify(methodType, times(1)).setRegisteredSensorConfig(null);

		// nothing to remove
		when(classType.hasInstrumentationPoints()).thenReturn(false);
		assertThat(creator.removeInstrumentationPoints(classType), is(false));
		verifyNoMoreInteractions(methodType);
	}

	@Test
	public void removeMethodInstrumentationPoints() {
		MethodSensorAssignment methodSensorAssignment = mock(MethodSensorAssignment.class);
		when(classType.hasInstrumentationPoints()).thenReturn(true);
		when(classType.getMethods()).thenReturn(Collections.singleton(methodType));

		assertThat(creator.removeInstrumentationPoints(classType, null, null), is(false));

		// matches
		when(classFilter.matches(methodSensorAssignment, classType)).thenReturn(true);
		when(methodFilter.matches(methodSensorAssignment, methodType)).thenReturn(true);
		assertThat(creator.removeInstrumentationPoints(classType, Collections.singleton(methodSensorAssignment), null), is(true));

		// does not matches
		when(classFilter.matches(methodSensorAssignment, classType)).thenReturn(true);
		when(methodFilter.matches(methodSensorAssignment, methodType)).thenReturn(false);
		assertThat(creator.removeInstrumentationPoints(classType, Collections.singleton(methodSensorAssignment), null), is(false));

		when(classFilter.matches(methodSensorAssignment, classType)).thenReturn(false);
		when(methodFilter.matches(methodSensorAssignment, methodType)).thenReturn(true);
		assertThat(creator.removeInstrumentationPoints(classType, Collections.singleton(methodSensorAssignment), null), is(false));

		verify(methodType, times(1)).setRegisteredSensorConfig(null);
	}

	@Test
	public void removeExceptionInstrumentationPoints() {
		ExceptionSensorAssignment exceptionSensorAssignment = mock(ExceptionSensorAssignment.class);
		when(classType.hasInstrumentationPoints()).thenReturn(true);
		when(classType.isException()).thenReturn(true);
		when(classType.getMethods()).thenReturn(Collections.singleton(methodType));
		
		assertThat(creator.removeInstrumentationPoints(classType, null, null), is(false));

		// matches
		when(classFilter.matches(exceptionSensorAssignment, classType)).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.CONSTRUCTOR);
		assertThat(creator.removeInstrumentationPoints(classType, null, Collections.singleton(exceptionSensorAssignment)), is(true));

		// does not matches
		when(classFilter.matches(exceptionSensorAssignment, classType)).thenReturn(true);
		when(methodType.getMethodCharacter()).thenReturn(Character.METHOD);
		assertThat(creator.removeInstrumentationPoints(classType, null, Collections.singleton(exceptionSensorAssignment)), is(false));

		when(classFilter.matches(exceptionSensorAssignment, classType)).thenReturn(false);
		when(methodType.getMethodCharacter()).thenReturn(Character.CONSTRUCTOR);
		assertThat(creator.removeInstrumentationPoints(classType, null, Collections.singleton(exceptionSensorAssignment)), is(false));

		verify(methodType, times(1)).setRegisteredSensorConfig(Mockito.<RegisteredSensorConfig> any());
	}
}
