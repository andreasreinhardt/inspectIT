package info.novatec.inspectit.agent.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 
 * @author Ivan Senic
 * 
 */
public class ClassLoaderDelegationMethodInstrumenter extends AdviceAdapter {

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
	 * Method descriptor of the load class method in the IAgent class.
	 */
	private static final String LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(Object[].class));

	/**
	 * Parameters of the method.
	 */
	private Type[] argumentTypes;


	/**
	 * @param mv
	 *            Super method visitor.
	 * @param access
	 *            Method access code.
	 * @param name
	 *            Method name.
	 * @param desc
	 *            Method description.
	 */
	public ClassLoaderDelegationMethodInstrumenter(MethodVisitor mv, int access, String name, String desc) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.argumentTypes = Type.getArgumentTypes(desc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMethodEnter() {
		loadAgent();

		// then push parameters
		pushParameters();

		// now invoke loadClass(Object[] params) method (no parameters here)
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IAGENT_INTERNAL_NAME, "loadClass", LOAD_CLASS_METHOD_DESCRIPTOR, true);

		dup();

		Label label = new Label();
		ifNull(label);

		returnValue();

		visitLabel(label);
	}

	/**
	 * Loads agent on the stack so that methods can be executed on it.
	 * <p>
	 * Default access so we can change in tests.
	 */
	void loadAgent() {
		// load first the Agent.agent static field
		mv.visitFieldInsn(Opcodes.GETSTATIC, AGENT_INTERNAL_NAME, "agent", IAGENT_DESCRIPTOR);
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
