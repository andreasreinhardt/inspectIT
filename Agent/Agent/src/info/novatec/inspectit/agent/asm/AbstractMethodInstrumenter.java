package info.novatec.inspectit.agent.asm;

import info.novatec.inspectit.agent.hooking.IHookDispatcher;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Base class for both method instrumenter and constructor instrumenter.
 * 
 * @author Ivan Senic
 * 
 */
public abstract class AbstractMethodInstrumenter extends AdviceAdapter {

	/**
	 * Class name where our IAgent exists as a field.
	 */
	private static final String AGENT_INTERNAL_NAME = "info/novatec/inspectit/agent/Agent";

	/**
	 * Internal name of our IAgent.
	 */
	private static final String IAGENT_INTERNAL_NAME = "info/novatec/inspectit/agent/IAgent";

	/**
	 * Descriptor of our IAgent.
	 */
	private static final String IAGENT_DESCRIPTOR = "L" + IAGENT_INTERNAL_NAME + ";";

	/**
	 * Internal name of our IHookDispatcher.
	 */
	private static final String GET_IHOOK_DISPATCHER_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(IHookDispatcher.class));

	/**
	 * Internal name of our IHookDispatcher.
	 */
	protected static final String IHOOK_DISPATCHER_INTERNAL_NAME = Type.getInternalName(IHookDispatcher.class);

	/**
	 * {@link Throwable} internal name.
	 */
	protected static final String THROWABLE_INTERNAL_NAME = Type.getInternalName(Throwable.class);

	/**
	 * Id of the method. This id will be passed to the dispatcher.
	 */
	protected long methodId;

	/**
	 * Marker declaring if enhanced exception sensor is active.
	 */
	protected boolean enhancedExceptionSensor;

	/**
	 * Parameters of the method.
	 */
	private Type[] argumentTypes;

	/**
	 * The label for the start of the try/finally or try/catch/finally block that we are adding.
	 */
	protected Label tryBlockStart = new Label();

	/**
	 * The label for the start of the catch block in try/catch/finally.
	 */
	protected Label catchHandler = new Label();

	/**
	 * The label for the start of the finally block in try/finally or try/catch/finally.
	 */
	protected Label finallyHandler = new Label();

	/**
	 * Set of labels that denote the start of catch blocks in the method that are not ours.
	 */
	private Set<Label> handlers = new HashSet<Label>(1);

	/**
	 * @param mv
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
	 */
	protected AbstractMethodInstrumenter(MethodVisitor mv, int access, String name, String desc, long methodId, boolean enhancedExceptionSensor) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.methodId = methodId;
		this.enhancedExceptionSensor = enhancedExceptionSensor;
		this.argumentTypes = Type.getArgumentTypes(desc);
	}

	/**
	 * Generates code for before catch call. Calling this method expects an exception on the stack
	 * that can be consumed.
	 */
	protected abstract void generateBeforeCatchCall();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);

		// if enhanced sensor is on we must add beforeCatch call to start of each catch block
		// thus we are saving labels that denote start of handler blocks
		// note we are skipping the finally blocks
		// and skipping one handler that denotes our catch block
		if (enhancedExceptionSensor && handler != catchHandler && null != type) {
			handlers.add(handler);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);

		// any label is saved to the handlers set we must add call to beforeCatch
		// note that these are catch handling blocks so exception is on stack
		if (handlers.contains(label)) {
			// duplicate the exception on the stack and call
			dup();
			generateBeforeCatchCall();
		}
	}

	/**
	 * Loads hook dispatcher on the stack so that methods can be executed on it.
	 * <p>
	 * Default access so we can change in tests.
	 */
	void loadHookDispatcher() {
		// load first the Agent.agent static field
		mv.visitFieldInsn(Opcodes.GETSTATIC, AGENT_INTERNAL_NAME, "agent", IAGENT_DESCRIPTOR);

		// now invoke getHookDispatcher() method (no parameters here)
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IAGENT_INTERNAL_NAME, "getHookDispatcher", GET_IHOOK_DISPATCHER_DESCRIPTOR, true);
	}

	/**
	 * Pushes null to stack.
	 */
	protected void pushNull() {
		mv.visitInsn(Opcodes.ACONST_NULL);
	}

	/**
	 * Creates Object[] array on stack containing values of all parameters. Primitive types are
	 * boxed.
	 */
	protected void pushParameters() {
		int size = argumentTypes.length;

		// push size of new array to the stack;
		push(size);

		// create a new array for storing all of the parameters in this
		newArray(Type.getType(Object.class));

		for (int i = 0; i < size; i++) {
			// duplicate the array so that we can push elements into
			dup();
			// push the current value of the iteration to the stack for the array index to be used
			push(i);
			// load the method parameter with the specific index
			loadArg(i);
			// optionally: box the primitive to the object
			box(argumentTypes[i]);
			// execute the array store
			mv.visitInsn(AASTORE);
		}
	}

}