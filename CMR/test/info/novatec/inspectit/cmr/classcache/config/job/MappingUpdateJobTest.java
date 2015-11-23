package info.novatec.inspectit.cmr.classcache.config.job;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.config.AgentCacheEntry;
import info.novatec.inspectit.cmr.classcache.config.ConfigurationCreator;
import info.novatec.inspectit.cmr.classcache.config.InstrumentationPointsUtil;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings({ "PMD" })
public class MappingUpdateJobTest {

	private static final long PLATFORM_ID = 10L;

	private MappingUpdateJob job;

	@Mock
	private InstrumentationPointsUtil instrumentationPointsUtil;

	@Mock
	private ConfigurationCreator configurationCreator;

	@Mock
	private AgentCacheEntry agentCacheEntry;

	@Mock
	private AgentConfiguration agentConfiguration;

	@Mock
	private Environment environment;

	@Mock
	private ClassCache classCache;

	@Mock
	private Environment updateEnvironment;

	@Mock
	private AgentConfiguration updateConfiguration;

	@BeforeMethod
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);

		job = new MappingUpdateJob();
		job.instrumentationPointsUtil = instrumentationPointsUtil;
		job.configurationCreator = configurationCreator;
		job.log = LoggerFactory.getLogger(MappingUpdateJob.class);
		job.setAgentCacheEntry(agentCacheEntry);

		when(agentCacheEntry.getAgentConfiguration()).thenReturn(agentConfiguration);
		when(agentCacheEntry.getEnvironment()).thenReturn(environment);
		when(agentCacheEntry.getClassCache()).thenReturn(classCache);
		when(agentCacheEntry.getId()).thenReturn(PLATFORM_ID);
	}

	@Test
	public void noEnvironment() {
		job.setEnvironment(null);
		job.run();

		verify(instrumentationPointsUtil, times(1)).removeAllInstrumentationPoints(classCache);

		verify(agentCacheEntry, times(1)).setAgentConfiguration(null);
		verify(agentCacheEntry, times(1)).setEnvironment(null);

		verifyNoMoreInteractions(instrumentationPointsUtil, configurationCreator, classCache);
	}

	@Test
	public void yesEnvironment() {
		doReturn(updateConfiguration).when(configurationCreator).environmentToConfiguration(updateEnvironment, PLATFORM_ID);
		doReturn(updateEnvironment).when(agentCacheEntry).getEnvironment();
		doReturn(updateConfiguration).when(agentCacheEntry).getAgentConfiguration();

		job.setEnvironment(updateEnvironment);
		job.run();

		verify(configurationCreator, times(1)).environmentToConfiguration(updateEnvironment, PLATFORM_ID);
		verify(agentCacheEntry, times(1)).setAgentConfiguration(updateConfiguration);
		verify(agentCacheEntry, times(1)).setEnvironment(updateEnvironment);

		verify(instrumentationPointsUtil, times(1)).removeAllInstrumentationPoints(classCache);
		verify(instrumentationPointsUtil, times(1)).addAllInstrumentationPoints(classCache, updateConfiguration, updateEnvironment);

		verify(instrumentationPointsUtil, times(1)).collectInstrumentationResultsWithHashes(classCache, updateEnvironment);
		verifyNoMoreInteractions(instrumentationPointsUtil, configurationCreator, classCache);
	}
}
