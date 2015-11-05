package info.novatec.inspectit.agent.core.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.AbstractLogSupport;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.RepositoryConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.RegistrationException;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.version.VersionService;

import java.net.ConnectException;

import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class IdManagerTest extends AbstractLogSupport {

	@Mock
	private IConfigurationStorage configurationStorage;

	@Mock
	private IConnection connection;

	@Mock
	private VersionService versionService;

	@Mock
	private AgentConfiguration agentConfiguration;

	private IdManager idManager;

	@BeforeMethod(dependsOnMethods = { "initMocks" })
	public void initTestClass() {
		idManager = new IdManager();
		idManager.configurationStorage = configurationStorage;
		idManager.connection = connection;
		idManager.versionService = versionService;
		idManager.log = LoggerFactory.getLogger(IdManager.class);
	}

	@Test
	public void connectAndRetrievePlatformId() throws Exception {
		String host = "localhost";
		int port = 1099;
		RepositoryConfig repositoryConfig = mock(RepositoryConfig.class);
		when(repositoryConfig.getHost()).thenReturn(host);
		when(repositoryConfig.getPort()).thenReturn(port);

		when(configurationStorage.getRepositoryConfig()).thenReturn(repositoryConfig);
		when(configurationStorage.getAgentName()).thenReturn("testAgent");
		when(versionService.getVersionAsString()).thenReturn("dummyVersion");

		long fakePlatformId = 7L;
		when(connection.isConnected()).thenReturn(false);
		when(connection.register("testAgent", "dummyVersion")).thenReturn(agentConfiguration);
		when(agentConfiguration.getPlatformId()).thenReturn(fakePlatformId);

		idManager.afterPropertiesSet();
		long platformId = idManager.getPlatformId();

		assertThat(platformId, is(equalTo(fakePlatformId)));
		verify(connection, times(1)).connect(host, port);
	}

	@Test
	public void retrievePlatformId() throws Exception {
		long fakePlatformId = 3L;
		when(connection.isConnected()).thenReturn(true);
		when(configurationStorage.getAgentName()).thenReturn("testAgent");
		when(versionService.getVersionAsString()).thenReturn("dummyVersion");
		when(connection.register("testAgent", "dummyVersion")).thenReturn(agentConfiguration);
		when(agentConfiguration.getPlatformId()).thenReturn(fakePlatformId);

		idManager.afterPropertiesSet();
		long platformId = idManager.getPlatformId();

		assertThat(platformId, is(equalTo(fakePlatformId)));
		verify(connection, times(0)).connect(anyString(), anyInt());
	}

	@Test(expectedExceptions = { IdNotAvailableException.class })
	public void platformIdNotAvailable() throws Exception {
		RepositoryConfig repositoryConfig = mock(RepositoryConfig.class);
		when(configurationStorage.getRepositoryConfig()).thenReturn(repositoryConfig);
		when(connection.isConnected()).thenReturn(false);
		doThrow(new ConnectException("fake")).when(connection).connect(anyString(), anyInt());

		idManager.afterPropertiesSet();
		idManager.getPlatformId();
	}

	/**
	 * Tests that unregister of platform is executed if connection to the server is established and
	 * registration is performed.
	 */
	@Test
	public void unregisterPlatform() throws Exception {
		// first simulate connect
		long fakePlatformId = 3L;
		when(connection.isConnected()).thenReturn(true);
		when(configurationStorage.getAgentName()).thenReturn("testAgent");
		when(versionService.getVersionAsString()).thenReturn("dummyVersion");
		when(connection.register("testAgent", "dummyVersion")).thenReturn(agentConfiguration);
		when(agentConfiguration.getPlatformId()).thenReturn(fakePlatformId);

		idManager.afterPropertiesSet();
		idManager.getPlatformId();
		idManager.unregisterPlatform();

		verify(connection, times(1)).unregister("testAgent");
	}

	/**
	 * Test that unregister will not be called if there is no active connection to the server and
	 * registration is not done at first place.
	 */
	@Test
	public void noUnregisterPlatform() throws RegistrationException, ServerUnavailableException {
		// no unregister if no connection
		when(connection.isConnected()).thenReturn(false);
		idManager.unregisterPlatform();

		// no unregister if registration is not done at the first place
		when(connection.isConnected()).thenReturn(true);
		idManager.unregisterPlatform();

		verify(connection, times(0)).unregister(anyString());
	}

	/**
	 * If unregister is called with shutdown initialized marker every next call to getPlatformId
	 * should throw an exception
	 */
	@Test(expectedExceptions = { IdNotAvailableException.class })
	public void unregisterPlatformAndInitShutdown() throws Exception {
		// first simulate connect
		long fakePlatformId = 3L;
		when(connection.isConnected()).thenReturn(true);
		when(configurationStorage.getAgentName()).thenReturn("testAgent");
		when(versionService.getVersionAsString()).thenReturn("dummyVersion");
		when(connection.register("testAgent", "dummyVersion")).thenReturn(agentConfiguration);
		when(agentConfiguration.getPlatformId()).thenReturn(fakePlatformId);

		idManager.afterPropertiesSet();
		idManager.getPlatformId();
		idManager.unregisterPlatform();
		idManager.getPlatformId();
	}

}
