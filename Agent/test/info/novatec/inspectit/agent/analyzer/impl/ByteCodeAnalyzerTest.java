package info.novatec.inspectit.agent.analyzer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.AbstractLogSupport;
import info.novatec.inspectit.agent.analyzer.IClassHashHelper;
import info.novatec.inspectit.agent.analyzer.classes.TestClass;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IIdManager;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.agent.hooking.IHookDispatcherMapper;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.exception.BusinessException;

import java.io.IOException;
import java.util.Collections;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ByteCodeAnalyzerTest extends AbstractLogSupport {

	private ByteCodeAnalyzer byteCodeAnalyzer;

	@Mock
	private IIdManager idManager;

	@Mock
	private IConnection connection;

	@Mock
	private IHookDispatcherMapper hookDispatcherMapper;

	@Mock
	private InstrumentationResult instrumentationResult;

	@Mock
	private RegisteredSensorConfig registeredSensorConfig;

	@Mock
	private IConfigurationStorage configurationStorage;

	@Mock
	private IClassHashHelper classHashHelper;

	private final Long platformId = 10L;

	@BeforeMethod(dependsOnMethods = { "initMocks" })
	public void initTestClass() throws IdNotAvailableException, ServerUnavailableException {
		byteCodeAnalyzer = new ByteCodeAnalyzer();
		byteCodeAnalyzer.idManager = idManager;
		byteCodeAnalyzer.connection = connection;
		byteCodeAnalyzer.hookDispatcherMapper = hookDispatcherMapper;
		byteCodeAnalyzer.classHashHelper = classHashHelper;
		byteCodeAnalyzer.configurationStorage = configurationStorage;
		byteCodeAnalyzer.log = LoggerFactory.getLogger(ByteCodeAnalyzer.class);

		when(idManager.getPlatformId()).thenReturn(platformId);
	}

	private byte[] getByteCode(String className) throws IOException {
		// get byte-code via ASM
		ClassReader reader = new ClassReader(className);
		ClassWriter writer = new ClassWriter(reader, 0);
		reader.accept(writer, 0);
		return writer.toByteArray();
	}

	@Test
	public void nullByteCode() {
		assertThat(byteCodeAnalyzer.analyzeAndInstrument(null, "Class", getClass().getClassLoader()), is(nullValue()));
	}

	@Test
	public void notToBeSentNoInstrumentation() throws IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		when(classHashHelper.isSent(hashCaptor.capture())).thenReturn(true);
		when(classHashHelper.getInstrumentationResult(hashCaptor.capture())).thenReturn(null);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// we did not send the class type
		assertThat(instrumentedByteCode, is(nullValue()));
		verify(classHashHelper, times(1)).isSent(hashCaptor.getValue());
		verifyZeroInteractions(idManager, connection, hookDispatcherMapper);

		// but we asked for the instrumentation result
		verify(classHashHelper, times(1)).getInstrumentationResult(hashCaptor.getValue());
		verifyZeroInteractions(hookDispatcherMapper);
		verifyNoMoreInteractions(classHashHelper);
	}

	@Test
	public void notToBeSentCachedInstrumentation() throws IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		when(classHashHelper.isSent(hashCaptor.capture())).thenReturn(true);
		when(classHashHelper.getInstrumentationResult(hashCaptor.capture())).thenReturn(instrumentationResult);

		when(instrumentationResult.getRegisteredSensorConfigs()).thenReturn(Collections.singleton(registeredSensorConfig));
		when(instrumentationResult.isClassLoadingDelegation()).thenReturn(false);
		long rscId = 13L;
		when(registeredSensorConfig.getId()).thenReturn(rscId);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// we did not send the class type
		assertThat(instrumentedByteCode, is(not(nullValue())));
		verify(classHashHelper, times(1)).isSent(hashCaptor.getValue());
		// but we asked for the instrumentation result and instrumented
		verify(classHashHelper, times(1)).getInstrumentationResult(hashCaptor.getValue());
		verify(hookDispatcherMapper, times(1)).addMapping(rscId, registeredSensorConfig);
		verifyNoMoreInteractions(hookDispatcherMapper, connection, classHashHelper, idManager);
	}

	@Test
	public void noInstrumentation() throws ServerUnavailableException, BusinessException, IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ClassType> classCaptor = ArgumentCaptor.forClass(ClassType.class);
		when(classHashHelper.isSent(hashCaptor.capture())).thenReturn(false);
		when(connection.analyzeAndInstrument(eq(platformId.longValue()), anyString(), classCaptor.capture())).thenReturn(null);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we get a null object
		assertThat(instrumentedByteCode, is(nullValue()));

		verify(connection, times(1)).analyzeAndInstrument(platformId.longValue(), hashCaptor.getValue(), classCaptor.getValue());
		verify(classHashHelper, times(1)).isSent(hashCaptor.getValue());
		verify(classHashHelper, times(1)).registerSent(hashCaptor.getValue(), null);
		verifyZeroInteractions(hookDispatcherMapper);
		verifyNoMoreInteractions(connection, classHashHelper);
	}

	@Test
	public void instrumentation() throws ServerUnavailableException, BusinessException, IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ClassType> classCaptor = ArgumentCaptor.forClass(ClassType.class);
		when(classHashHelper.isSent(hashCaptor.capture())).thenReturn(false);
		when(connection.analyzeAndInstrument(eq(platformId.longValue()), anyString(), classCaptor.capture())).thenReturn(instrumentationResult);
		when(instrumentationResult.getRegisteredSensorConfigs()).thenReturn(Collections.singleton(registeredSensorConfig));
		when(instrumentationResult.isClassLoadingDelegation()).thenReturn(false);
		long rscId = 13L;
		when(registeredSensorConfig.getId()).thenReturn(rscId);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we get a null object
		assertThat(instrumentedByteCode, is(not(nullValue())));

		verify(connection, times(1)).analyzeAndInstrument(platformId.longValue(), hashCaptor.getValue(), classCaptor.getValue());
		verify(classHashHelper, times(1)).isSent(hashCaptor.getValue());
		verify(classHashHelper, times(1)).registerSent(hashCaptor.getValue(), instrumentationResult);
		verify(hookDispatcherMapper, times(1)).addMapping(rscId, registeredSensorConfig);
		verifyNoMoreInteractions(hookDispatcherMapper, connection, classHashHelper);
	}

	@Test
	public void classLoadingDelegation() throws ServerUnavailableException, BusinessException, IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ClassType> classCaptor = ArgumentCaptor.forClass(ClassType.class);
		when(classHashHelper.isSent(hashCaptor.capture())).thenReturn(false);
		when(connection.analyzeAndInstrument(eq(platformId.longValue()), anyString(), classCaptor.capture())).thenReturn(instrumentationResult);
		when(instrumentationResult.getRegisteredSensorConfigs()).thenReturn(Collections.<RegisteredSensorConfig> emptyList());
		when(instrumentationResult.isClassLoadingDelegation()).thenReturn(true);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we get a null object
		assertThat(instrumentedByteCode, is(not(nullValue())));

		verify(connection, times(1)).analyzeAndInstrument(platformId.longValue(), hashCaptor.getValue(), classCaptor.getValue());
		verify(classHashHelper, times(1)).isSent(hashCaptor.getValue());
		verify(classHashHelper, times(1)).registerSent(hashCaptor.getValue(), instrumentationResult);
		verifyNoMoreInteractions(connection, classHashHelper);
		verifyZeroInteractions(hookDispatcherMapper);
	}
}
