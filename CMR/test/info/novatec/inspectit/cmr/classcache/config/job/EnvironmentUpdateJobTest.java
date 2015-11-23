package info.novatec.inspectit.cmr.classcache.config.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
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
import info.novatec.inspectit.cmr.classcache.config.InstrumentationPointsUtil;
import info.novatec.inspectit.exception.BusinessException;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
	private InstrumentationPointsUtil instrumentationPointsUtil;

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
		job.instrumentationPointsUtil = instrumentationPointsUtil;
		job.configurationCreator = configurationCreator;
		job.log = LoggerFactory.getLogger(EnvironmentUpdateJob.class);
		job.setAgentCacheEntry(agentCacheEntry);

		when(agentCacheEntry.getAgentConfiguration()).thenReturn(agentConfiguration);
		when(agentCacheEntry.getEnvironment()).thenReturn(environment);
		when(agentCacheEntry.getClassCache()).thenReturn(classCache);
		when(agentCacheEntry.getId()).thenReturn(PLATFORM_ID);

		when(configurationCreator.environmentToConfiguration(updateEnvironment, PLATFORM_ID)).thenReturn(updateConfiguration);

		job.setEnvironment(updateEnvironment);
	}

	@Test
	public void onlyConfigurationUpdate() throws RemoteException, BusinessException {
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		verify(agentConfiguration, times(0)).setInitialInstrumentationResults(Mockito.anyMap());
		verify(agentConfiguration, times(0)).setClassCacheExistsOnCmr(Mockito.anyBoolean());

		verifyZeroInteractions(classCache, environment, classCacheSearchNarrower, instrumentationPointsUtil);
	}

	@Test
	public void profileAddedWithMethodSensor() throws RemoteException, BusinessException {
		Collection<ClassType> types = Collections.singleton(classType);

		doReturn(types).when(classCacheSearchNarrower).narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		doReturn(types).when(instrumentationPointsUtil).addInstrumentationPoints(eq(types), eq(classCache), eq(agentConfiguration), eq(environment),
				Mockito.<Collection<MethodSensorAssignment>> any(), Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getMethodSensorAssignments()).thenReturn(Collections.singletonList(methodSensorAssignment));

		job.setAddedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationPointsUtil, times(1)).addInstrumentationPoints(eq(types), eq(classCache), eq(agentConfiguration), eq(environment), captor.capture(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());

		assertThat((Collection<MethodSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<MethodSensorAssignment>) captor.getValue()).iterator().next(), is(methodSensorAssignment));

		verify(instrumentationPointsUtil, times(1)).collectInstrumentationResultsWithHashes(classCache, environment);
		verifyNoMoreInteractions(instrumentationPointsUtil);
		verify(agentConfiguration, times(1)).setInitialInstrumentationResults(Mockito.anyMap());
		verify(agentConfiguration, times(1)).setClassCacheExistsOnCmr(true);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileAddedWithExceptionSensor() throws RemoteException, BusinessException {
		Collection<ClassType> types = Collections.singleton(classType);

		doReturn(types).when(classCacheSearchNarrower).narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		doReturn(types).when(instrumentationPointsUtil).addInstrumentationPoints(eq(types), eq(classCache), eq(agentConfiguration), eq(environment),
				Mockito.<Collection<MethodSensorAssignment>> any(), Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getExceptionSensorAssignments()).thenReturn(Collections.singletonList(exceptionSensorAssignment));

		job.setAddedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationPointsUtil, times(1)).addInstrumentationPoints(eq(types), eq(classCache), eq(agentConfiguration), eq(environment), Mockito.<Collection<MethodSensorAssignment>> any(),
				captor.capture());

		assertThat((Collection<ExceptionSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ExceptionSensorAssignment>) captor.getValue()).iterator().next(), is(exceptionSensorAssignment));

		verify(instrumentationPointsUtil, times(1)).collectInstrumentationResultsWithHashes(classCache, environment);
		verifyNoMoreInteractions(instrumentationPointsUtil);
		verify(agentConfiguration, times(1)).setInitialInstrumentationResults(Mockito.anyMap());
		verify(agentConfiguration, times(1)).setClassCacheExistsOnCmr(true);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileRemovedWithMethodSensor() throws RemoteException, BusinessException {
		Collection<ClassType> types = Collections.singleton(classType);

		doReturn(types).when(classCacheSearchNarrower).narrowByMethodSensorAssignment(classCache, methodSensorAssignment);
		doReturn(types).when(instrumentationPointsUtil).removeInstrumentationPoints(eq(types), eq(classCache), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getMethodSensorAssignments()).thenReturn(Collections.singletonList(methodSensorAssignment));

		job.setRemovedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationPointsUtil, times(1)).removeInstrumentationPoints(eq(types), eq(classCache), captor.capture(), Mockito.<Collection<ExceptionSensorAssignment>> any());

		assertThat((Collection<MethodSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<MethodSensorAssignment>) captor.getValue()).iterator().next(), is(methodSensorAssignment));

		verify(instrumentationPointsUtil, times(1)).addAllInstrumentationPoints(captor.capture(), eq(classCache), eq(agentConfiguration), eq(environment));
		assertThat((Collection<ClassType>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ClassType>) captor.getValue()).iterator().next(), is(classType));

		verify(instrumentationPointsUtil, times(1)).collectInstrumentationResultsWithHashes(classCache, environment);
		verifyNoMoreInteractions(instrumentationPointsUtil);
		verify(agentConfiguration, times(1)).setInitialInstrumentationResults(Mockito.anyMap());
		verify(agentConfiguration, times(1)).setClassCacheExistsOnCmr(true);
		verifyZeroInteractions(environment, configurationCreator);
	}

	@Test
	public void profileRemovedWithExceptionSensor() throws RemoteException, BusinessException {
		Collection<ClassType> types = Collections.singleton(classType);

		doReturn(types).when(classCacheSearchNarrower).narrowByExceptionSensorAssignment(classCache, exceptionSensorAssignment);
		doReturn(types).when(instrumentationPointsUtil).removeInstrumentationPoints(eq(types), eq(classCache), Mockito.<Collection<MethodSensorAssignment>> any(),
				Mockito.<Collection<ExceptionSensorAssignment>> any());
		when(profile.getExceptionSensorAssignments()).thenReturn(Collections.singletonList(exceptionSensorAssignment));

		job.setRemovedProfiles(Collections.singleton(profile));
		job.run();

		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);

		ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
		verify(instrumentationPointsUtil, times(1)).removeInstrumentationPoints(eq(types), eq(classCache), Mockito.<Collection<MethodSensorAssignment>> any(), captor.capture());

		assertThat((Collection<ExceptionSensorAssignment>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ExceptionSensorAssignment>) captor.getValue()).iterator().next(), is(exceptionSensorAssignment));

		verify(instrumentationPointsUtil, times(1)).addAllInstrumentationPoints(captor.capture(), eq(classCache), eq(agentConfiguration), eq(environment));
		assertThat((Collection<ClassType>) captor.getValue(), hasSize(1));
		assertThat(((Collection<ClassType>) captor.getValue()).iterator().next(), is(classType));

		verify(instrumentationPointsUtil, times(1)).collectInstrumentationResultsWithHashes(classCache, environment);
		verifyNoMoreInteractions(instrumentationPointsUtil);
		verify(agentConfiguration, times(1)).setInitialInstrumentationResults(Mockito.anyMap());
		verify(agentConfiguration, times(1)).setClassCacheExistsOnCmr(true);
		verifyZeroInteractions(environment, configurationCreator);
	}

}
