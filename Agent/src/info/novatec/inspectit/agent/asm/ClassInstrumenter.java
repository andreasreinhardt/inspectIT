package info.novatec.inspectit.agent.asm;

import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.hooking.IHookDispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Used to instrument classes via ASM.
 * 
 * @author Ivan Senic
 * 
 */
public class ClassInstrumenter extends ClassVisitor {

	/**
	 * If enhanced exception sensor is ON.
	 */
	private boolean enhancedExceptionSensor;

	/**
	 * If class should be checked and instrumented for the class loading delegation.
	 */
	boolean classLoadingDelegation;

	/**
	 * If class has been added any kind of instrumentation.
	 */
	private boolean byteCodeAdded = false;

	/**
	 * All register sensor configs that should be added as instrumentation points.
	 */
	Collection<RegisteredSensorConfig> registeredSensorConfigs;

	/**
	 * Simple constructor. Can be used in testing.
	 * 
	 * @param classVisitor
	 *            Parent class visitor.
	 */
	ClassInstrumenter(ClassVisitor classVisitor) {
		super(Opcodes.ASM5, classVisitor);
	}

	/**
	 * Default constructor.
	 * 
	 * @param classVisitor
	 *            Parent class visitor.
	 * @param registeredSensorConfigs
	 *            Instrumentation points.
	 * @param enhancedExceptionSensor
	 *            If enhanced exception sensor is ON.
	 * @param classLoadingDelegation
	 *            If class loading delegation should be checked for this class.
	 */
	public ClassInstrumenter(ClassVisitor classVisitor, Collection<RegisteredSensorConfig> registeredSensorConfigs, boolean enhancedExceptionSensor, boolean classLoadingDelegation) {
		super(Opcodes.ASM5, classVisitor);
		this.enhancedExceptionSensor = enhancedExceptionSensor;
		this.classLoadingDelegation = classLoadingDelegation;
		this.registeredSensorConfigs = new ArrayList<RegisteredSensorConfig>(registeredSensorConfigs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// calling super to ensure the visitor pattern
		MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

		// using JSR inliner adapter in order to remove JSR/RET instructions
		// see http://mail-archive.ow2.org/asm/2008-11/msg00008.html
		methodVisitor = new JSRInlinerAdapter(methodVisitor, access, name, desc, signature, exceptions);

		RegisteredSensorConfig rsc = shouldInstrument(name, desc);
		if (null != rsc) {
			long id = rsc.getId();
			boolean constructor = "<init>".equals(name);

			if (constructor) {
				methodVisitor = getConstructorInstrumenter(methodVisitor, access, name, desc, id, isEnhancedExceptionSensor());
			} else {
				methodVisitor = getMethodInstrumenter(methodVisitor, access, name, desc, id, isEnhancedExceptionSensor());
			}
			byteCodeAdded = true;
		}

		if (isClassLoadingDelegationMethod(name, desc)) {
			methodVisitor = getClassLoaderDelegationMethodInstrumenter(methodVisitor, access, name, desc);
			byteCodeAdded = true;
		}

		return methodVisitor;
	}

	/**
	 * If method should be instrumented. If there is appropriate {@link RegisteredSensorConfig} that
	 * denotes that method should be instrumented this will be removed from the
	 * {@link #registeredSensorConfigs} and returned as a result.
	 * 
	 * @param name
	 *            Name of the method.
	 * @param desc
	 *            ASM description of the method.
	 * @return {@link RegisteredSensorConfig} if method should be instrumented, otherwise
	 *         <code>null</code>
	 */
	RegisteredSensorConfig shouldInstrument(String name, String desc) {
		for (Iterator<RegisteredSensorConfig> it = registeredSensorConfigs.iterator(); it.hasNext();) {
			RegisteredSensorConfig rsc = it.next();

			if (matches(name, desc, rsc)) {
				it.remove();
				return rsc;
			}
		}

		return null;
	}

	/**
	 * If method name and description matches the {@link RegisteredSensorConfig}.
	 * 
	 * @param name
	 *            method name
	 * @param desc
	 *            method ASM description
	 * @param rsc
	 *            {@link RegisteredSensorConfig}
	 * @return <code>true</code> if name and desc matches the rsc
	 */
	private boolean matches(String name, String desc, RegisteredSensorConfig rsc) {
		if (!name.equals(rsc.getTargetMethodName())) {
			return false;
		}

		Type methodType = Type.getMethodType(desc);
		if (!methodType.getReturnType().getClassName().equals(rsc.getReturnType())) {
			return false;
		}

		Type[] argumentTypes = methodType.getArgumentTypes();
		List<String> parameterTypes = rsc.getParameterTypes();

		// if both are empty return true (null safety)
		if (CollectionUtils.isEmpty(parameterTypes) && ArrayUtils.isEmpty(argumentTypes)) {
			return true;
		}

		// if not same size return false
		if (argumentTypes.length != parameterTypes.size()) {
			return false;
		}

		// check then one by one
		for (int i = 0; i < argumentTypes.length; i++) {
			if (!argumentTypes[i].getClassName().equals(parameterTypes.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if method is class loading delegation one.
	 * 
	 * @param name
	 *            name of the method
	 * @param desc
	 *            description.
	 * @return <code>true</code> if {@link ClassLoaderDelegationMethodInstrumenter} should be
	 *         applied on the method
	 */
	boolean isClassLoadingDelegationMethod(String name, String desc) {
		return classLoadingDelegation && ClassLoaderDelegationMethodInstrumenter.isLoadClassMethod(name, desc);
	}

	/**
	 * Is exception sensor active.
	 * 
	 * @return Is exception sensor active.
	 */
	boolean isEnhancedExceptionSensor() {
		return enhancedExceptionSensor;
	}

	/**
	 * Returns proper {@link MethodInstrumenter}.
	 * <p>
	 * Tests can override this method for easier testing.
	 * 
	 * @param superMethodVisitor
	 *            Super method visitor.
	 * @param access
	 *            Method access code.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method description.
	 * @param methodId
	 *            Method id that will be passed to {@link IHookDispatcher}.
	 * @param enhancedExceptionSensor
	 *            Marker declaring if enhanced exception sensor is active.
	 * @return Returns proper {@link MethodInstrumenter}.
	 */
	MethodInstrumenter getMethodInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc, long methodId, boolean enhancedExceptionSensor) {
		return new MethodInstrumenter(superMethodVisitor, access, name, desc, methodId, enhancedExceptionSensor);
	}

	/**
	 * Returns proper {@link ConstructorInstrumenter}.
	 * <p>
	 * Tests can override this method for easier testing.
	 * 
	 * @param superMethodVisitor
	 *            Super method visitor.
	 * @param access
	 *            Method access code.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method description.
	 * @param constructorId
	 *            Method id that will be passed to {@link IHookDispatcher}.
	 * @param enhancedExceptionSensor
	 *            Marker declaring if enhanced exception sensor is active.
	 * @return Returns proper {@link ConstructorInstrumenter}.
	 */
	ConstructorInstrumenter getConstructorInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc, long constructorId, boolean enhancedExceptionSensor) {
		return new ConstructorInstrumenter(superMethodVisitor, access, name, desc, constructorId, enhancedExceptionSensor);
	}

	/**
	 * Returns proper {@link ClassLoaderDelegationMethodInstrumenter}.
	 * <p>
	 * Tests can override this method for easier testing.
	 * 
	 * @param superMethodVisitor
	 *            Super method visitor.
	 * @param access
	 *            Method access code.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method description.
	 * @return Returns proper {@link ClassLoaderDelegationMethodInstrumenter}.
	 */
	ClassLoaderDelegationMethodInstrumenter getClassLoaderDelegationMethodInstrumenter(MethodVisitor superMethodVisitor, int access, String name, String desc) {
		return new ClassLoaderDelegationMethodInstrumenter(superMethodVisitor, access, name, desc);
	}

	/**
	 * Gets {@link #byteCodeAdded}.
	 * 
	 * @return {@link #byteCodeAdded}
	 */
	public boolean isByteCodeAdded() {
		return byteCodeAdded;
	}

}
