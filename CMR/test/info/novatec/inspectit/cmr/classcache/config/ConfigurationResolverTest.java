package info.novatec.inspectit.cmr.classcache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.ci.AgentMapping;
import info.novatec.inspectit.ci.AgentMappings;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.cmr.ci.ConfigurationInterfaceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ConfigurationResolverTest {

	private ConfigurationResolver configurationResolver;

	@Mock
	ConfigurationInterfaceManager configurationInterfaceManager;

	@Mock
	AgentMappings agentMappings;
	
	private String agentName = "inspectit";
	
	private List<String> definedIPs = Collections.singletonList("127.0.0.1");

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);

		configurationResolver = new ConfigurationResolver();
		configurationResolver.configurationInterfaceManager = configurationInterfaceManager;
	
		when(configurationInterfaceManager.getAgentMappings()).thenReturn(agentMappings);
	}
	
	@Test(expectedExceptions = { Exception.class })
	public void noMappings() throws Exception {
		when(agentMappings.getMappings()).thenReturn(Collections.<AgentMapping> emptyList());
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}

	@Test(expectedExceptions = { Exception.class })
	public void noMatchingMappingsName() throws Exception {
		AgentMapping mapping = mock(AgentMapping.class);
		when(agentMappings.getMappings()).thenReturn(Collections.singletonList(mapping));

		when(mapping.getAgentName()).thenReturn("something else");
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}

	@Test(expectedExceptions = { Exception.class })
	public void noMatchingMappingsNameWildcard() throws Exception {
		AgentMapping mapping = mock(AgentMapping.class);
		when(agentMappings.getMappings()).thenReturn(Collections.singletonList(mapping));

		when(mapping.getAgentName()).thenReturn("ins*TT");
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}
	
	@Test(expectedExceptions = { Exception.class })
	public void noMatchingMappingsIp() throws Exception {
		AgentMapping mapping = mock(AgentMapping.class);
		when(agentMappings.getMappings()).thenReturn(Collections.singletonList(mapping));

		when(mapping.getAgentName()).thenReturn("*");
		when(mapping.getIpAddress()).thenReturn("128.0.0.1");
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}
	
	@Test(expectedExceptions = { Exception.class })
	public void noMatchingMappingsIpWildcard() throws Exception {
		AgentMapping mapping = mock(AgentMapping.class);
		when(agentMappings.getMappings()).thenReturn(Collections.singletonList(mapping));

		when(mapping.getAgentName()).thenReturn("*");
		when(mapping.getIpAddress()).thenReturn("127.*.2");
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}

	@Test(expectedExceptions = { Exception.class })
	public void twoMatchingMappings() throws Exception {
		AgentMapping mapping1 = mock(AgentMapping.class);
		AgentMapping mapping2 = mock(AgentMapping.class);
		List<AgentMapping> mappings = new ArrayList<>();
		mappings.add(mapping1);
		mappings.add(mapping2);
		when(agentMappings.getMappings()).thenReturn(mappings);

		when(mapping1.getAgentName()).thenReturn("*");
		when(mapping1.getIpAddress()).thenReturn("*");
		when(mapping2.getAgentName()).thenReturn("*");
		when(mapping2.getIpAddress()).thenReturn("*");
		configurationResolver.getEnvironmentForAgent(definedIPs, agentName);
	}

	@Test
	public void oneMatchingMapping() throws Exception {
		AgentMapping mapping1 = mock(AgentMapping.class);
		AgentMapping mapping2 = mock(AgentMapping.class);
		List<AgentMapping> mappings = new ArrayList<>();
		mappings.add(mapping1);
		mappings.add(mapping2);
		when(agentMappings.getMappings()).thenReturn(mappings);

		when(mapping1.getAgentName()).thenReturn("ins*");
		when(mapping1.getIpAddress()).thenReturn("*");
		when(mapping2.getAgentName()).thenReturn("something else");
		when(mapping1.getEnvironmentId()).thenReturn("env1");
		Environment environment = mock(Environment.class);
		when(configurationInterfaceManager.getEnvironment("env1")).thenReturn(environment);

		assertThat(configurationResolver.getEnvironmentForAgent(definedIPs, agentName), is(environment));
	}
}
