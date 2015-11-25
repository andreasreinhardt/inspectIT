package info.novatec.inspectit.agent.asm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.IAgent;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.hooking.IHookDispatcher;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springframework.asm.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ClassInstrumenterTest extends AbstractInstrumentationTest {

	private static final String TEST_CLASS_FQN = "info.novatec.inspectit.agent.asm.InstrumentationTestClass";

	private static final String EXCEPTION_TEST_CLASS_FQN = "info.novatec.inspectit.agent.asm.InstrumentationExceptionTestClass";

	private static final String TEST_CLASS_LOADER_FQN = "info.novatec.inspectit.agent.asm.MyTestClassLoader";

	public static IHookDispatcher dispatcher;

	@Mock
	private IHookDispatcher hookDispatcher;

	public static IAgent a;

	@Mock
	private IAgent agent;

	@Mock
	private RegisteredSensorConfig rsc;

	@Mock
	private RegisteredSensorConfig rsc2;

	private ClassInstrumenter classInstrumenter;

	private ClassWriter classWriter;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);

		dispatcher = hookDispatcher;
		a = agent;
	}

	// should instrument

	@Test
	public void matchesAll() {
		rsc = new RegisteredSensorConfig();
		rsc.setTargetMethodName("methodName");
		rsc.setReturnType("void");
		List<String> parameters = new ArrayList<String>();
		parameters.add("java.lang.Object");
		parameters.add("int");
		parameters.add("long[]");
		rsc.setParameterTypes(parameters);

		rsc2 = new RegisteredSensorConfig();
		rsc2.setTargetMethodName("methodName2");
		rsc2.setReturnType("double");
		rsc2.setParameterTypes(Collections.<String> emptyList());

		List<RegisteredSensorConfig> rscList = new ArrayList<RegisteredSensorConfig>();
		rscList.add(rsc);
		rscList.add(rsc2);

		classInstrumenter = new ClassInstrumenter(null, rscList, false, false);

		assertThat(classInstrumenter.shouldInstrument("methodName", "(Ljava/lang/Object;I[J)V"), is(rsc));
		assertThat(classInstrumenter.shouldInstrument("methodName2", "()D"), is(rsc2));
	}

	@Test
	public void doesNotMatchName() {
		rsc = new RegisteredSensorConfig();
		rsc.setTargetMethodName("methodName");

		classInstrumenter = new ClassInstrumenter(null, Collections.singletonList(rsc), false, false);

		assertThat(classInstrumenter.shouldInstrument("someName", "(Ljava/lang/Object;I)V"), is(nullValue()));
	}

	@Test
	public void doesNotMatchReturnType() {
		rsc = new RegisteredSensorConfig();
		rsc.setTargetMethodName("methodName");
		rsc.setReturnType("double");

		classInstrumenter = new ClassInstrumenter(null, Collections.singletonList(rsc), false, false);

		assertThat(classInstrumenter.shouldInstrument("methodName", "()I"), is(nullValue()));
		assertThat(classInstrumenter.shouldInstrument("methodName", "()V"), is(nullValue()));
	}

	@Test
	public void doesNotMatchParameters() {
		rsc = new RegisteredSensorConfig();
		rsc.setTargetMethodName("methodName");
		rsc.setReturnType("double");
		List<String> parameters = new ArrayList<String>();
		parameters.add("int");
		parameters.add("long");
		rsc.setParameterTypes(parameters);

		classInstrumenter = new ClassInstrumenter(null, Collections.singletonList(rsc), false, false);

		// size does not match
		assertThat(classInstrumenter.shouldInstrument("methodName", "()D"), is(nullValue()));
		assertThat(classInstrumenter.shouldInstrument("methodName", "(III)D"), is(nullValue()));

		// types do not match
		assertThat(classInstrumenter.shouldInstrument("methodName", "(ID)D"), is(nullValue()));
		assertThat(classInstrumenter.shouldInstrument("methodName", "(JI)D"), is(nullValue()));

		rsc.setParameterTypes(Collections.<String> emptyList());
		assertThat(classInstrumenter.shouldInstrument("methodName", "(I)D"), is(nullValue()));
	}

	// no instrumentation

	@Test
	public void noInstrumenatation() throws Exception {
		String methodName = "stringNullParameter";
		long methodId = 3L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(null);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(false));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		this.callMethod(testClass, methodName, null);

		verifyZeroInteractions(hookDispatcher);
	}

	// return, params, static

	@Test
	public void methodHookNoStatic() throws Exception {
		String methodName = "stringNullParameter";
		long methodId = 3L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], "stringNullParameter");
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], "stringNullParameter");
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void methodHookStatic() throws Exception {
		String methodName = "voidNullParameterStatic";
		long methodId = 7L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, null, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, null, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, null, new Object[0], null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void stringNullParameter() throws Exception {
		String methodName = "stringNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], "stringNullParameter");
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], "stringNullParameter");
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void intNullParameter() throws Exception {
		String methodName = "intNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], 3);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], 3);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void doubleNullParameter() throws Exception {
		String methodName = "doubleNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], 5.3D);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], 5.3D);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void floatNullParameter() throws Exception {
		String methodName = "floatNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], Float.MAX_VALUE);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], Float.MAX_VALUE);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void byteNullParameter() throws Exception {
		String methodName = "byteNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], (byte) 127);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], (byte) 127);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void shortNullParameter() throws Exception {
		String methodName = "shortNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], (short) 16345);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], (short) 16345);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void booleanNullParameter() throws Exception {
		String methodName = "booleanNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], false);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], false);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void charNullParameter() throws Exception {
		String methodName = "charNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], '\u1234');
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], '\u1234');
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void voidNullParameterStatic() throws Exception {
		String methodName = "voidNullParameterStatic";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, null, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, null, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, null, new Object[0], null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void stringNullParameterStatic() throws Exception {
		String methodName = "stringNullParameterStatic";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, null, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, null, new Object[0], "stringNullParameterStatic");
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, null, new Object[0], "stringNullParameterStatic");
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void voidOneParameter() throws Exception {
		String methodName = "voidOneParameter";
		Object[] parameters = { "java.lang.String" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, parameters);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, parameters);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, parameters, null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, parameters, null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void stringOneParameter() throws Exception {
		String methodName = "stringOneParameter";
		Object[] parameters = { "java.lang.String" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, parameters);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, parameters);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, parameters, "stringOneParameter");
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, parameters, "stringOneParameter");
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void voidTwoParameters() throws Exception {
		String methodName = "voidTwoParameters";
		Object[] parameters = { "java.lang.String", "java.lang.Object" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, parameters);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, parameters);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, parameters, null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, parameters, null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void mixedTwoParameters() throws Exception {
		String methodName = "mixedTwoParameters";
		Object[] parameters = { "int", "boolean" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, parameters);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, parameters);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, parameters, null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, parameters, null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void intArrayNullParameter() throws Exception {
		String methodName = "intArrayNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], new int[] { 1, 2, 3 });
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], new int[] { 1, 2, 3 });
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void stringArrayNullParameter() throws Exception {
		String methodName = "stringArrayNullParameter";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], new String[] { "test123", "bla" });
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], new String[] { "test123", "bla" });
		verifyNoMoreInteractions(hookDispatcher);
	}

	// exception no enhanced

	@Test
	public void unexpectedExceptionTrowingNoEnhanced() throws Exception {
		String methodName = "unexpectedExceptionThrowing";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		try {
			this.callMethod(testClass, methodName, null);
		} catch (Throwable t) {
		}

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void unexpectedExceptionNotTrowingNoEnhanced() throws Exception {
		String methodName = "unexpectedExceptionNotThrowing";
		Object[] parameters = { "java.lang.Object" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		try {
			this.callMethod(testClass, methodName, parameters);
		} catch (Throwable t) {
		}

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, parameters);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, parameters, null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, parameters, null);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void exceptionHandledResultReturned() throws Exception {
		String methodName = "exceptionHandledResultReturned";
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], 3);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], 3);
		verifyNoMoreInteractions(hookDispatcher);
	}

	// constructors

	@Test
	public void constructorNullParameter() throws Exception {
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// create instance
		Object instance = this.createInstance(TEST_CLASS_FQN, b);

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, new Object[0]);
		verify(hookDispatcher).dispatchConstructorAfterBody(methodId, instance, new Object[0]);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void constructorStringOneParameter() throws Exception {
		Object[] parameters = { "java.lang.String" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { String.class });
		Object instance = constructor.newInstance(parameters);

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, parameters);
		verify(hookDispatcher).dispatchConstructorAfterBody(methodId, instance, parameters);
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void nestedConstructorBooleanOneParameter() throws Exception {
		Object[] parameters = { Boolean.TRUE };
		Object[] nestedParameters = { "delegate" };
		long methodId = 9L;
		long nestedMethodId = 13L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument("<init>", "(Z)V")).thenReturn(rsc);

		when(rsc2.getId()).thenReturn(nestedMethodId);
		when(classInstrumenter.shouldInstrument("<init>", "(Ljava/lang/String;)V")).thenReturn(rsc2);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { Boolean.TYPE });
		Object testClass = constructor.newInstance(parameters);

		verify(hookDispatcher).dispatchConstructorBeforeBody(nestedMethodId, nestedParameters);
		verify(hookDispatcher).dispatchConstructorAfterBody(nestedMethodId, testClass, nestedParameters);

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, parameters);
		verify(hookDispatcher).dispatchConstructorAfterBody(methodId, testClass, parameters);
		verifyNoMoreInteractions(hookDispatcher);
	}

	// constructor exception no enhanced

	@Test
	public void constructorUnexpectedExceptionTrowingNoEnhanced() throws Exception {
		Object[] parameters = { 3 };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { int.class });
		try {
			constructor.newInstance(parameters);
		} catch (Throwable t) {

		}

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, parameters);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchConstructorAfterBody(eq(methodId), captor.capture(), eq(parameters));

		assertThat(captor.getValue().getClass().getName(), is(TEST_CLASS_FQN));
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void constructorUnexpectedExceptionNotTrowingNoEnhanced() throws Exception {
		Object[] parameters = { "test" };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { Object.class });
		try {
			constructor.newInstance(parameters);
		} catch (Throwable t) {

		}

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, parameters);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchConstructorAfterBody(eq(methodId), captor.capture(), eq(parameters));

		assertThat(captor.getValue().getClass().getName(), is(TEST_CLASS_FQN));

		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void constructorExceptionHandledResultReturned() throws Exception {
		Object[] parameters = { 11L };
		long methodId = 9L;

		ClassReader cr = new ClassReader(TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { long.class });
		Object instance = constructor.newInstance(parameters);

		verify(hookDispatcher).dispatchConstructorBeforeBody(methodId, parameters);
		verify(hookDispatcher).dispatchConstructorAfterBody(methodId, instance, parameters);
		verifyNoMoreInteractions(hookDispatcher);
	}

	// exception enhanced

	@Test
	public void exceptionThrowerIsInstrumented() throws Exception {
		String methodName = "throwsAndHandlesException";
		long methodId = 9L;

		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);
		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(EXCEPTION_TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], null);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchBeforeCatch(eq(methodId), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void exceptionThrowerIsInstrumentedWhenConstructor() throws Exception {
		Object[] params = { "test" };
		long constructorId = 11L;
		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(constructorId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);
		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		Class<?> clazz = createClass(EXCEPTION_TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { String.class });
		Object instance = constructor.newInstance(params);

		verify(hookDispatcher).dispatchConstructorBeforeBody(constructorId, params);
		verify(hookDispatcher).dispatchConstructorAfterBody(constructorId, instance, params);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchConstructorBeforeCatch(eq(constructorId), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void exceptionThrowerAndHandlerAreInstrumented() throws Exception {
		String methodName = "callsMethodWithException";
		String innerMethodName = "throwsAnException";
		long methodId = 9L;
		long innerMethodId = 11L;

		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		when(rsc2.getId()).thenReturn(innerMethodId);
		when(classInstrumenter.shouldInstrument(eq(innerMethodName), Mockito.<String> any())).thenReturn(rsc2);

		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(EXCEPTION_TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		// first method
		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], null);

		// inner method
		verify(hookDispatcher).dispatchMethodBeforeBody(innerMethodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(innerMethodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(innerMethodId, testClass, new Object[0], null);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchOnThrowInBody(eq(innerMethodId), eq(testClass), (Object[]) anyObject(), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verify(hookDispatcher).dispatchBeforeCatch(eq(methodId), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void exceptionThrowerAndHandlerAreInstrumentedStatic() throws Exception {
		String methodName = "callsStaticMethodWithException";
		String innerMethodName = "staticThrowsAnException";
		long methodId = 9L;
		long innerMethodId = 11L;

		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		when(rsc2.getId()).thenReturn(innerMethodId);
		when(classInstrumenter.shouldInstrument(eq(innerMethodName), Mockito.<String> any())).thenReturn(rsc2);

		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(EXCEPTION_TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		// first method
		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, null, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, null, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, null, new Object[0], null);

		// inner method
		verify(hookDispatcher).dispatchMethodBeforeBody(innerMethodId, null, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(innerMethodId, null, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(innerMethodId, null, new Object[0], null);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchOnThrowInBody(eq(innerMethodId), eq(null), (Object[]) anyObject(), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verify(hookDispatcher).dispatchBeforeCatch(eq(methodId), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void exceptionMethodThrowingConstructorPassing() throws Exception {
		Object[] params = new Object[] { 3 };
		String innerMethodName = "throwsAnException";
		long innerMethodId = 9L;
		long constructorId = 11L;

		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(constructorId);
		when(classInstrumenter.shouldInstrument(eq("<init>"), Mockito.<String> any())).thenReturn(rsc);

		when(rsc2.getId()).thenReturn(innerMethodId);
		when(classInstrumenter.shouldInstrument(eq(innerMethodName), Mockito.<String> any())).thenReturn(rsc2);
		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Class<?> clazz = createClass(EXCEPTION_TEST_CLASS_FQN, b);
		Constructor<?> constructor = clazz.getConstructor(new Class[] { int.class });
		try {
			constructor.newInstance(params);
		} catch (Throwable t) {
		}

		// first method
		verify(hookDispatcher).dispatchConstructorBeforeBody(constructorId, params);
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchConstructorAfterBody(eq(constructorId), captor.capture(), eq(params));

		assertThat(captor.getValue().getClass().getName(), is(EXCEPTION_TEST_CLASS_FQN));

		// inner method
		verify(hookDispatcher).dispatchMethodBeforeBody(eq(innerMethodId), anyObject(), eq(new Object[0]));
		verify(hookDispatcher).dispatchFirstMethodAfterBody(eq(innerMethodId), anyObject(), eq(new Object[0]), eq(null));
		verify(hookDispatcher).dispatchSecondMethodAfterBody(eq(innerMethodId), anyObject(), eq(new Object[0]), eq(null));

		verify(hookDispatcher).dispatchOnThrowInBody(eq(innerMethodId), anyObject(), eq(new Object[0]), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verify(hookDispatcher).dispatchConstructorOnThrowInBody(eq(constructorId), anyObject(), eq(params), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));
		verifyNoMoreInteractions(hookDispatcher);
	}

	@Test
	public void callsMethodWithExceptionAndTryCatchFinally() throws Exception {
		String methodName = "callsMethodWithExceptionAndTryCatchFinally";
		String innerMethodName = "throwsAnException";
		long methodId = 9L;
		long innerMethodId = 11L;

		ClassReader cr = new ClassReader(EXCEPTION_TEST_CLASS_FQN);
		prepareWriter(cr, null);

		when(rsc.getId()).thenReturn(methodId);
		when(classInstrumenter.shouldInstrument(eq(methodName), Mockito.<String> any())).thenReturn(rsc);

		when(rsc2.getId()).thenReturn(innerMethodId);
		when(classInstrumenter.shouldInstrument(eq(innerMethodName), Mockito.<String> any())).thenReturn(rsc2);
		when(classInstrumenter.isEnhancedExceptionSensor()).thenReturn(true);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(EXCEPTION_TEST_CLASS_FQN, b);
		this.callMethod(testClass, methodName, null);

		verify(hookDispatcher).dispatchMethodBeforeBody(methodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(methodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(methodId, testClass, new Object[0], null);

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchBeforeCatch(eq(methodId), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verify(hookDispatcher).dispatchMethodBeforeBody(innerMethodId, testClass, new Object[0]);
		verify(hookDispatcher).dispatchFirstMethodAfterBody(innerMethodId, testClass, new Object[0], null);
		verify(hookDispatcher).dispatchSecondMethodAfterBody(innerMethodId, testClass, new Object[0], null);

		captor = ArgumentCaptor.forClass(Object.class);
		verify(hookDispatcher).dispatchOnThrowInBody(eq(innerMethodId), eq(testClass), (Object[]) anyObject(), captor.capture());
		assertThat(captor.getValue().getClass().getName(), is(equalTo(MyTestException.class.getName())));

		verifyNoMoreInteractions(hookDispatcher);
	}

	// class loader delegation

	@Test
	public void classLoadingDelegationActiveLoadClass() throws Exception {
		Class<?> clazz = getClass();
		Object[] parameters = { "java.lang.String" };
		String methodName = "loadClass";

		doReturn(clazz).when(agent).loadClass(parameters);

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		classInstrumenter.classLoadingDelegation = true;

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_LOADER_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		Class<?> result = (Class<?>) this.callMethod(testClass, methodName, parameters);

		assertThat((Object) result, is(equalTo((Object) clazz)));

		verify(agent, times(1)).loadClass(parameters);
		verifyNoMoreInteractions(agent);
	}

	@Test
	public void classLoadingDelegationActiveDoesNotLoadClass() throws Exception {
		Object[] parameters = { "java.lang.String" };
		String methodName = "loadClass";

		doReturn(null).when(agent).loadClass(parameters);

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		classInstrumenter.classLoadingDelegation = true;

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_LOADER_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		Class<?> result = (Class<?>) this.callMethod(testClass, methodName, parameters);

		// it's delegated to super class loader so we should get the String class back
		assertThat((Object) result, is(equalTo((Object) String.class)));

		verify(agent, times(1)).loadClass(parameters);
		verifyNoMoreInteractions(agent);
	}

	@Test
	public void classLoadingDelegationActiveWrongMethod() throws Exception {
		Object[] parameters = { "java.lang.String" };
		String methodName = "getResource";

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		classInstrumenter.classLoadingDelegation = true;

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(true));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_LOADER_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		this.callMethod(testClass, methodName, parameters);

		verifyZeroInteractions(agent);
	}

	@Test
	public void classLoadingDelegationNotActive() throws Exception {
		Object[] parameters = { "java.lang.String" };
		String methodName = "loadClass";

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		classInstrumenter.classLoadingDelegation = false;

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		assertThat(classInstrumenter.isByteCodeAdded(), is(false));
		byte b[] = classWriter.toByteArray();

		// now call this method
		Object testClass = this.createInstance(TEST_CLASS_LOADER_FQN, b);
		// call this method via reflection as we would get a class cast
		// exception by casting to the concrete class.
		this.callMethod(testClass, methodName, parameters);

		verifyZeroInteractions(agent);
	}

	private void prepareWriter(ClassReader cr, ClassLoader classLoader) {
		classWriter = new LoaderAwareClassWriter(cr, ClassWriter.COMPUTE_FRAMES, classLoader);
		classInstrumenter = new ClassInstrumenter(classWriter);
		classInstrumenter = Mockito.spy(classInstrumenter);
		classInstrumenter.registeredSensorConfigs = Collections.emptyList();
		doAnswer(new Answer<MethodVisitor>() {
			public MethodInstrumenter answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				return getMethodInstrumenter((MethodVisitor) arguments[0], (Integer) arguments[1], (String) arguments[2], (String) arguments[3], (Long) arguments[4], (Boolean) arguments[5]);
			}
		}).when(classInstrumenter).getMethodInstrumenter(Mockito.<MethodVisitor> any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());
		doAnswer(new Answer<MethodVisitor>() {
			public ConstructorInstrumenter answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				return getConstructorInstrumenter((MethodVisitor) arguments[0], (Integer) arguments[1], (String) arguments[2], (String) arguments[3], (Long) arguments[4], (Boolean) arguments[5]);
			}
		}).when(classInstrumenter).getConstructorInstrumenter(Mockito.<MethodVisitor> any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());
		doAnswer(new Answer<MethodVisitor>() {
			public ClassLoaderDelegationMethodInstrumenter answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				return getClassLoaderDelegationMethodInstrumenter((MethodVisitor) arguments[0], (Integer) arguments[1], (String) arguments[2], (String) arguments[3]);
			}
		}).when(classInstrumenter).getClassLoaderDelegationMethodInstrumenter(Mockito.<MethodVisitor> any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());

	}

	private MethodInstrumenter getMethodInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc, long id, boolean enhancedExceptionSensor) {
		return new MethodInstrumenter(superMethodVisitor, access, name, desc, id, enhancedExceptionSensor) {
			@Override
			void loadHookDispatcher() {
				mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(ClassInstrumenterTest.class), "dispatcher", Type.getDescriptor(IHookDispatcher.class));
			}
		};
	}

	private ConstructorInstrumenter getConstructorInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc, long id, boolean enhancedExceptionSensor) {
		return new ConstructorInstrumenter(superMethodVisitor, access, name, desc, id, enhancedExceptionSensor) {
			@Override
			void loadHookDispatcher() {
				mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(ClassInstrumenterTest.class), "dispatcher", Type.getDescriptor(IHookDispatcher.class));
			}
		};
	}

	private ClassLoaderDelegationMethodInstrumenter getClassLoaderDelegationMethodInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc) {
		return new ClassLoaderDelegationMethodInstrumenter(superMethodVisitor, access, name, desc) {
			@Override
			void loadAgent() {
				mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(ClassInstrumenterTest.class), "a", Type.getDescriptor(IAgent.class));
			}
		};
	}

}
