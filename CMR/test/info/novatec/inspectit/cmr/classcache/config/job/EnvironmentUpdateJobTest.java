package info.novatec.inspectit.cmr.classcache.config.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.Profile;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.config.AgentCacheEntry;
import info.novatec.inspectit.cmr.classcache.config.ClassCacheSearchNarrower;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationCreator;
import info.novatec.inspectit.cmr.classcache.config.InstrumentationCreator;
import info.novatec.inspectit.cmr.service.exception.ServiceException;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings({ "PMD", "all" })
public class EnvironmentUpdateJobTest {

	private static final long PLATFORM_ID = 10L;

	private EnvironmentUpdateJob job;

	@Mock
	private ClassCacheSearchNarrower classCacheSearchNarrower;

	@Mock
	private InstrumentationCreator instrumentationCreator;

	@Mock
	private ConfigurationCreator configurationCreator;

	@Mock
	private AgentCacheEntry agentCacheEntry;

	@Mock
	private AgentConfiguration agentConfiguration;

	@Mock
	private AgentConfiguration updateConfiguration;

	@Mock
	private Environment environment;

	@Mock
	private Environment updateEnvironment;

	@Mock
	private ClassCache classCache;

	@Mock
	private MethodSensorAssignment methodSensorAssignment;

	@Mock
	private ExceptionSensorAssignment exceptionSensorAssignment;

	@Mock
	private ClassType classType;

	@Mock
	private Profile profile;

	@BeforeMethod
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);

		job = new EnvironmentUpdateJob();
		job.classCacheSearchNarrower = classCacheSearchNarrower;
		job.instrumentationCreator = instrumentationCreator;
		job.configurationCreator = configurationCreator;
		job.log = LoggerFactory.getLogger(ProfileUpdateJob.class);
		job.setAgentCacheEntry(agentCacheEntry);

		when(agentCacheEntry.getAgentConfiguration()).thenReturn(agentConfiguration);
		when(agentCacheEntry.getEnvironment()).thenReturn(environment);
		when(agentCacheEntry.getClassCache()).thenReturn(classCache);
		when(agentCacheEntry.getId()).thenReturn(PLATFORM_ID);

		Answer<Object> callableAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Callable<?> callable = (Callable<?>) invocation.getArguments()[0];
				return callable.call();
			}
		};
		doAnswer(callableAnswer).when(classCache).executeWithReadLock(Mockito.<Callable<?>> anyObject());
		doAnswer(callableAnswer).when(classCache).executeWithWriteLock(Mockito.<Callable<?>> anyObject());

		when(configurationCreator.environmentToConfiguration(updateEnvironment, PLATFORM_ID)).thenReturn(updateConfiguration);
		
		job.setEnvironment(updateEnvironment);
	}

	@Test
	public void onlyConfigurationUpdate() throws RemoteException, ServiceException {
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentConfiguration, times(0)).setInstrumentationLastModified(anyLong());
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		verifyZeroInteractions(classCache, environment, classCacheSearchNarrower, instrumentationCreator);
	}

	@Test
	public void profileAddedWithMethodSensor() throws RemoteException, ServiceException {
		doReturn(Collections.singleton(classType)).when(classCacheSearchNarrower).narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		doReturn(true).when(instrumentationCreator).addInstrumentationPoints(eq(agentConfiguration), eq(environment), eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getMethodSensorAssignments()).thenReturn(Collections.singletonList(methodSensorAssignment));

		job.setAddedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentConfiguration, times(1)).setInstrumentationLastModified(anyLong());
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationCreator, times(1)).addInstrumentationPoints(eq(agentConfiguration), eq(environment), eq(classType), captor.capture(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());

		assertThat((Collection<MethodSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<MethodSensorAssignment>) captor.getValue()).iterator().next(), is(methodSensorAssignment));

		verifyNoMoreInteractions(instrumentationCreator);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileAddedWithExceptionSensor() throws RemoteException, ServiceException {
		doReturn(Collections.singleton(classType)).when(classCacheSearchNarrower).narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		doReturn(true).when(instrumentationCreator).addInstrumentationPoints(eq(agentConfiguration), eq(environment), eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getExceptionSensorAssignments()).thenReturn(Collections.singletonList(exceptionSensorAssignment));

		job.setAddedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentConfiguration, times(1)).setInstrumentationLastModified(anyLong());
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationCreator, times(1)).addInstrumentationPoints(eq(agentConfiguration), eq(environment), eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(), captor.capture());

		assertThat((Collection<ExceptionSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ExceptionSensorAssignment>) captor.getValue()).iterator().next(), is(exceptionSensorAssignment));

		verifyNoMoreInteractions(instrumentationCreator);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileRemovedWithMethodSensor() throws RemoteException, ServiceException {
		doReturn(Collections.singleton(classType)).when(classCacheSearchNarrower).narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		doReturn(true).when(instrumentationCreator).removeInstrumentationPoints(eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getMethodSensorAssignments()).thenReturn(Collections.singletonList(methodSensorAssignment));

		job.setRemovedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentConfiguration, times(1)).setInstrumentationLastModified(anyLong());
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationCreator, times(1)).removeInstrumentationPoints(eq(classType), captor.capture(), Mockito.<Collection<ExceptionSensorAssignment>> any());

		assertThat((Collection<MethodSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<MethodSensorAssignment>) captor.getValue()).iterator().next(), is(methodSensorAssignment));

		verify(instrumentationCreator, times(1)).addInstrumentationPoints(eq(agentConfiguration), eq(environment), captor.capture());
		assertThat((Collection<ClassType>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ClassType>) captor.getValue()).iterator().next(), is(classType));

		verifyNoMoreInteractions(instrumentationCreator);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileRemovedWithExceptionSensor() throws RemoteException, ServiceException {
		doReturn(Collections.singleton(classType)).when(classCacheSearchNarrower).narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		doReturn(true).when(instrumentationCreator).removeInstrumentationPoints(eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getExceptionSensorAssignments()).thenReturn(Collections.singletonList(exceptionSensorAssignment));

		job.setRemovedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentConfiguration, times(1)).setInstrumentationLastModified(anyLong());
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationCreator, times(1)).removeInstrumentationPoints(eq(classType), Mockito.<Collection<MethodSensorAssignment>> any(), captor.capture());

		assertThat((Collection<ExceptionSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ExceptionSensorAssignment>) captor.getValue()).iterator().next(), is(exceptionSensorAssignment));

		verify(instrumentationCreator, times(1)).addInstrumentationPoints(eq(agentConfiguration), eq(environment), captor.capture());
		assertThat((Collection<ClassType>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ClassType>) captor.getValue()).iterator().next(), is(classType));

		verifyNoMoreInteractions(instrumentationCreator);
		verifyZeroInteractions(environment, configurationCreator);
	}

}

