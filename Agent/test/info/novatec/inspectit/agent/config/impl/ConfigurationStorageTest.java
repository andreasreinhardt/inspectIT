package info.novatec.inspectit.agent.config.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.AbstractLogSupport;
import info.novatec.inspectit.agent.config.StorageException;
import info.novatec.inspectit.pattern.IMatchPattern;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ConfigurationStorageTest extends AbstractLogSupport {

	@Mock
	private AgentConfiguration agentConfiguration;

	private ConfigurationStorage configurationStorage;

	/**
	 * This method will be executed before every method is executed in here. This ensures that some
	 * tests don't modify the contents of the configuration storage.
	 */
	@BeforeMethod(dependsOnMethods = { "initMocks" })
	public void initTestClass() throws StorageException {
		configurationStorage = new ConfigurationStorage();
		configurationStorage.log = LoggerFactory.getLogger(ConfigurationStorage.class);

		// name and repository
		configurationStorage.setAgentName("UnitTestAgent");
		configurationStorage.setRepository("localhost", 1099);
		configurationStorage.setAgentConfiguration(agentConfiguration);

	}

	@Test()
	public void agentNameCheck() {
		assertThat(configurationStorage.getAgentName(), is(equalTo("UnitTestAgent")));
	}

	@Test(expectedExceptions = { StorageException.class })
	public void setNullAgentName() throws StorageException {
		configurationStorage.setAgentName(null);
	}

	@Test(expectedExceptions = { StorageException.class })
	public void setEmptyAgentName() throws StorageException {
		configurationStorage.setAgentName("");
	}

	@Test
	public void repositoryCheck() {
		assertThat(configurationStorage.getRepositoryConfig().getHost(), is(equalTo("localhost")));
		assertThat(configurationStorage.getRepositoryConfig().getPort(), is(equalTo(1099)));
	}

	@Test(expectedExceptions = { StorageException.class })
	public void setNullRepositoryHost() throws StorageException {
		configurationStorage.setRepository(null, 1099);
	}

	@Test(expectedExceptions = { StorageException.class })
	public void setEmptyRepositoryHost() throws StorageException {
		configurationStorage.setRepository("", 1099);
	}

	@Test
	public void resetRepositoryNotAllowed() throws StorageException {
		configurationStorage.setRepository("localhost1", 1200);

		assertThat(configurationStorage.getRepositoryConfig().getHost(), is(equalTo("localhost")));
		assertThat(configurationStorage.getRepositoryConfig().getPort(), is(equalTo(1099)));
	}

	@Test
	public void resetAgentnameNotAllowed() throws StorageException {
		configurationStorage.setAgentName("agent1");

		assertThat(configurationStorage.getAgentName(), is(equalTo("UnitTestAgent")));
	}

	@Test
	public void sendingStrategyCheck() throws StorageException {
		StrategyConfig strategyConfig = mock(StrategyConfig.class);
		when(agentConfiguration.getSendingStrategyConfig()).thenReturn(strategyConfig);

		List<StrategyConfig> config = configurationStorage.getSendingStrategyConfigs();
		assertThat(config, hasSize(1));
		assertThat(config, hasItem(strategyConfig));
	}

	@Test(expectedExceptions = { StorageException.class })
	public void sendingStrategyNotDefined() throws StorageException {
		when(agentConfiguration.getSendingStrategyConfig()).thenReturn(null);
		configurationStorage.getSendingStrategyConfigs();
	}

	@Test
	public void bufferStrategyCheck() throws StorageException {
		StrategyConfig strategyConfig = mock(StrategyConfig.class);
		when(agentConfiguration.getBufferStrategyConfig()).thenReturn(strategyConfig);

		StrategyConfig config = configurationStorage.getBufferStrategyConfig();
		assertThat(config, is(strategyConfig));
	}

	@Test(expectedExceptions = { StorageException.class })
	public void bufferStrategyNotDefined() throws StorageException {
		when(agentConfiguration.getBufferStrategyConfig()).thenReturn(null);
		configurationStorage.getBufferStrategyConfig();
	}

	@Test
	public void methodSensorTypes() throws StorageException {
		MethodSensorTypeConfig methodSensorTypeConfig = mock(MethodSensorTypeConfig.class);
		when(agentConfiguration.getMethodSensorTypeConfigs()).thenReturn(Collections.singletonList(methodSensorTypeConfig));

		assertThat(configurationStorage.getMethodSensorTypes(), hasSize(1));
		assertThat(configurationStorage.getMethodSensorTypes(), hasItem(methodSensorTypeConfig));
	}

	@Test
	public void methodSensorTypesNotDefined() throws StorageException {
		when(agentConfiguration.getMethodSensorTypeConfigs()).thenReturn(null);

		assertThat(configurationStorage.getMethodSensorTypes(), is(empty()));
	}

	@Test
	public void exceptionSensorTypes() throws StorageException {
		ExceptionSensorTypeConfig exceptionSensorTypeConfig = mock(ExceptionSensorTypeConfig.class);
		when(agentConfiguration.getExceptionSensorTypeConfig()).thenReturn(exceptionSensorTypeConfig);

		assertThat(configurationStorage.getExceptionSensorTypes(), hasSize(1));
		assertThat(configurationStorage.getExceptionSensorTypes(), hasItem(exceptionSensorTypeConfig));
	}

	@Test
	public void exceptionSensorTypesNotDefined() throws StorageException {
		when(agentConfiguration.getExceptionSensorTypeConfig()).thenReturn(null);

		assertThat(configurationStorage.getExceptionSensorTypes(), is(empty()));
	}

	@Test
	public void platformSensorTypes() throws StorageException {
		PlatformSensorTypeConfig platformSensorTypeConfig = mock(PlatformSensorTypeConfig.class);
		when(agentConfiguration.getPlatformSensorTypeConfigs()).thenReturn(Collections.singletonList(platformSensorTypeConfig));

		assertThat(configurationStorage.getPlatformSensorTypes(), hasSize(1));
		assertThat(configurationStorage.getPlatformSensorTypes(), hasItem(platformSensorTypeConfig));
	}

	@Test
	public void platformSensorTypesNotDefined() throws StorageException {
		when(agentConfiguration.getPlatformSensorTypeConfigs()).thenReturn(null);

		assertThat(configurationStorage.getPlatformSensorTypes(), is(empty()));
	}

	@Test
	public void exceptionSensorInfo() throws StorageException {
		when(agentConfiguration.getExceptionSensorTypeConfig()).thenReturn(null);

		assertThat(configurationStorage.isExceptionSensorActivated(), is(false));
		assertThat(configurationStorage.isEnhancedExceptionSensorActivated(), is(false));

		ExceptionSensorTypeConfig exceptionSensorTypeConfig = mock(ExceptionSensorTypeConfig.class);
		when(agentConfiguration.getExceptionSensorTypeConfig()).thenReturn(exceptionSensorTypeConfig);

		when(exceptionSensorTypeConfig.isEnhanced()).thenReturn(false);
		assertThat(configurationStorage.isExceptionSensorActivated(), is(true));
		assertThat(configurationStorage.isEnhancedExceptionSensorActivated(), is(false));

		when(exceptionSensorTypeConfig.isEnhanced()).thenReturn(true);
		assertThat(configurationStorage.isExceptionSensorActivated(), is(true));
		assertThat(configurationStorage.isEnhancedExceptionSensorActivated(), is(true));
	}

	@Test
	public void ignoreClassesCheck() throws StorageException {
		IMatchPattern pattern = mock(IMatchPattern.class);
		when(agentConfiguration.getExcludeClassesPatterns()).thenReturn(Collections.singleton(pattern));

		Collection<IMatchPattern> ignorePatterns = configurationStorage.getIgnoreClassesPatterns();
		assertThat(ignorePatterns, is(notNullValue()));
		assertThat(ignorePatterns, hasItem(pattern));
	}
}
