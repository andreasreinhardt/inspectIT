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
import info.novatec.inspectit.agent.analyzer.classes.TestClass;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IIdManager;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.agent.hooking.IHookDispatcherMapper;
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
	private SendingClassHashCache sendingClassCache;

	@Mock
	private InstrumentationResult instrumentationResult;

	@Mock
	private RegisteredSensorConfig registeredSensorConfig;

	private final Long platformId = 10L;

	@BeforeMethod(dependsOnMethods = { "initMocks" })
	public void initTestClass() throws IdNotAvailableException, ServerUnavailableException {
		byteCodeAnalyzer = new ByteCodeAnalyzer();
		byteCodeAnalyzer.idManager = idManager;
		byteCodeAnalyzer.connection = connection;
		byteCodeAnalyzer.hookDispatcherMapper = hookDispatcherMapper;
		byteCodeAnalyzer.sendingClassHashCache = sendingClassCache;
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
	public void notToBeSent() throws IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		when(sendingClassCache.isSending(hashCaptor.capture())).thenReturn(false);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we did not send the class
		assertThat(instrumentedByteCode, is(nullValue()));
		verify(sendingClassCache, times(1)).isSending(hashCaptor.getValue());
		verifyZeroInteractions(idManager, connection, hookDispatcherMapper);
		verifyNoMoreInteractions(sendingClassCache);
	}

	@Test
	public void noInstrumentation() throws ServerUnavailableException, BusinessException, IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		when(sendingClassCache.isSending(hashCaptor.capture())).thenReturn(true);
		when(connection.analyzeAndInstrument(eq(platformId.longValue()), anyString(), null)).thenReturn(null);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we get a null object
		assertThat(instrumentedByteCode, is(nullValue()));

		verify(connection, times(1)).analyzeAndInstrument(platformId.longValue(), hashCaptor.getValue(), null);
		verify(sendingClassCache, times(1)).isSending(hashCaptor.getValue());
		verify(sendingClassCache, times(1)).markSending(hashCaptor.getValue(), false);
		verifyZeroInteractions(hookDispatcherMapper);
		verifyNoMoreInteractions(connection, sendingClassCache);
	}

	@Test
	public void instrumentation() throws ServerUnavailableException, BusinessException, IOException {
		String className = TestClass.class.getName();
		ClassLoader classLoader = TestClass.class.getClassLoader();
		byte[] byteCode = getByteCode(className);

		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		when(sendingClassCache.isSending(hashCaptor.capture())).thenReturn(true);
		when(connection.analyzeAndInstrument(eq(platformId.longValue()), anyString(), null)).thenReturn(instrumentationResult);
		when(instrumentationResult.getRegisteredSensorConfigs()).thenReturn(Collections.singleton(registeredSensorConfig));
		long rscId = 13L;
		when(registeredSensorConfig.getId()).thenReturn(rscId);

		byte[] instrumentedByteCode = byteCodeAnalyzer.analyzeAndInstrument(byteCode, className, classLoader);

		// as no instrumentation happened, we get a null object
		assertThat(instrumentedByteCode, is(not(nullValue())));

		verify(connection, times(1)).analyzeAndInstrument(platformId.longValue(), hashCaptor.getValue(), null);
		verify(sendingClassCache, times(1)).isSending(hashCaptor.getValue());
		verify(sendingClassCache, times(1)).markSending(hashCaptor.getValue(), true);
		verify(hookDispatcherMapper, times(1)).addMapping(rscId, registeredSensorConfig);
		verifyNoMoreInteractions(hookDispatcherMapper, connection, sendingClassCache);
	}
}
