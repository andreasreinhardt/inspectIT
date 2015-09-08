package info.novatec.inspectit.agent.asm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import info.novatec.inspectit.agent.IAgent;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.springframework.asm.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class ClassLoaderDelegationMethodInstrumenterTest extends AbstractInstrumentationTest {

	private static final String TEST_CLASS_LOADER_FQN = "info.novatec.inspectit.agent.asm.MyTestClassLoader";

	public static IAgent a;

	@Mock
	private IAgent agent;

	private LoaderAwareClassWriter classWriter;

	private ClassVisitor classInstrumenter;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);

		a = agent;
	}

	@Test
	public void loadClass() throws Exception {
		Class<?> clazz = getClass();
		Object[] parameters = { "java.lang.String" };
		String methodName = "loadClass";

		doReturn(clazz).when(agent).loadClass(parameters);

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
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
	public void doesNotLoadClass() throws Exception {
		Object[] parameters = { "java.lang.String" };
		String methodName = "loadClass";

		doReturn(null).when(agent).loadClass(parameters);

		ClassReader cr = new ClassReader(TEST_CLASS_LOADER_FQN);
		prepareWriter(cr, null);

		cr.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
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

	private void prepareWriter(ClassReader cr, ClassLoader classLoader) {
		classWriter = new LoaderAwareClassWriter(cr, ClassWriter.COMPUTE_FRAMES, classLoader);
		classInstrumenter = new ClassVisitor(Opcodes.ASM5, classWriter) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

				// skip constructor
				if ("<init>".equals(name)) {
					return mv;
				}

				return new ClassLoaderDelegationMethodInstrumenter(mv, access, name, desc) {;
					@Override
					void loadAgent() {
						mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(ClassLoaderDelegationMethodInstrumenterTest.class), "a", Type.getDescriptor(IAgent.class));
					}
				};
			}
		};
	}
}
