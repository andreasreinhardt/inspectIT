package info.novatec.inspectit.agent.asm;

import info.novatec.inspectit.agent.hooking.IHookDispatcher;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Used to instrument constructors only.
 * 
 * @author Ivan Senic
 * 
 */
public class ConstructorInstrumenter extends AbstractMethodInstrumenter {

	/**
	 * {@link IHookDispatcher#dispatchConstructorBeforeBody(long, Object[])} descriptor.
	 */
	private static final String DISPATCH_CONSTRUCTOR_BEFORE_BODY_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE, Type.getType(Object[].class));

	/**
	 * {@link IHookDispatcher#dispatchConstructorAfterBody(long, Object, Object[])} descriptor.
	 */
	private static final String DISPATCH_CONSTRUCTOR_AFTER_BODY_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE, Type.getType(Object.class), Type.getType(Object[].class));

	/**
	 * {@link IHookDispatcher#dispatchConstructorBeforeCatch(long, Object)} descriptor.
	 */
	private static final String DISPATCH_CONSTRUCTOR_BEFORE_CATCH_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE, Type.getType(Object.class));

	/**
	 * {@link IHookDispatcher#dispatchConstructorOnThrowInBody(long, Object, Object[], Object)}
	 * descriptor.
	 */
	private static final String DISPATCH_CONSTRUCTOR_ON_THROW_BODY_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE, Type.getType(Object.class), Type.getType(Object[].class),
			Type.getType(Object.class));

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
	public ConstructorInstrumenter(MethodVisitor mv, int access, String name, String desc, long methodId, boolean enhancedExceptionSensor) {
		super(mv, access, name, desc, methodId, enhancedExceptionSensor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMethodEnter() {
		generateBeforeBodyCall();

		// start our try block
		visitLabel(tryBlockStart);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMethodExit(int opcode) {
		// we wont add any byte-code prior to athrow return
		// if exception is thrown we will execute calls in the finally block we are adding
		if (opcode == ATHROW) {
			// exception return
			return;
		}

		// after constructor we can load this object
		// just load on stack and generate call
		// this object or null if's static
		if (isStatic) {
			pushNull();
		} else {
			loadThis();
		}

		// generate code for calling first and second
		generateAfterBodyCall();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// the definition of the end of the try block
		Label tryBlockEnd = new Label();
		visitLabel(tryBlockEnd);

		// only add catch block if exception sensor is active
		if (enhancedExceptionSensor) {
			super.visitTryCatchBlock(tryBlockStart, tryBlockEnd, catchHandler, THROWABLE_INTERNAL_NAME);
			visitLabel(catchHandler);

			// duplicate exception and call
			dup();
			generateThrowInBodyCall();
		}

		// setup for the finally block
		super.visitTryCatchBlock(tryBlockStart, tryBlockEnd, finallyHandler, null);
		visitLabel(finallyHandler);

		// generate code for calling after
		// push created object and call
		// this object or null if's static
		if (isStatic) {
			pushNull();
		} else {
			loadThis();
		}
		generateAfterBodyCall();

		mv.visitInsn(ATHROW);

		// update the max stack stuff
		super.visitMaxs(maxStack, maxLocals);
	}

	/**
	 * Generates code for calling
	 * {@link IHookDispatcher#dispatchConstructorBeforeBody(long, Object[])}.
	 */
	private void generateBeforeBodyCall() {
		// load hook dispatcher
		loadHookDispatcher();

		// first push method id
		push(methodId);

		// then parameters
		loadArgArray();

		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IHOOK_DISPATCHER_INTERNAL_NAME, "dispatchConstructorBeforeBody", DISPATCH_CONSTRUCTOR_BEFORE_BODY_DESCRIPTOR, true);
	}

	/**
	 * Generates code for calling
	 * {@link IHookDispatcher#dispatchConstructorAfterBody(long, Object, Object[])}. This method
	 * expect that created instance is on the stack and can be consumed.
	 */
	private void generateAfterBodyCall() {
		// prepare for calls
		// we expect created object on stack so we must swap as object is argument after method id
		// in the call
		loadHookDispatcher();
		swap();

		// first push method id
		push(methodId);
		// can not just swap because method id is long, thus a bit of gymnastic
		// r-l-l2
		dup2X1();
		// l-l2-r-l-l2
		pop2();
		// l-l2-r :)

		// then parameters, no need to swap
		loadArgArray();

		// execute after body
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IHOOK_DISPATCHER_INTERNAL_NAME, "dispatchConstructorAfterBody", DISPATCH_CONSTRUCTOR_AFTER_BODY_DESCRIPTOR, true);
	}

	/**
	 * Generates call for {@link IHookDispatcher#dispatchConstructorBeforeCatch(long, Object)}.
	 * Expects caught exception on the stack that can be consumed.
	 */
	protected void generateBeforeCatchCall() {
		// prepare for calls
		// we expect exception on stack so we must swap as exception is last argument in the call
		loadHookDispatcher();
		swap();

		// first push method id
		push(methodId);
		// can not just swap because method id is long, thus a bit of gymnastic
		// r-l-l2
		dup2X1();
		// l-l2-r-l-l2
		pop2();
		// l-l2-r :)

		// execute before catch
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IHOOK_DISPATCHER_INTERNAL_NAME, "dispatchConstructorBeforeCatch", DISPATCH_CONSTRUCTOR_BEFORE_CATCH_DESCRIPTOR, true);
	}

	/**
	 * Generates call for
	 * {@link IHookDispatcher#dispatchConstructorOnThrowInBody(long, Object, Object[], Object)}.
	 * Expects caught exception on the stack that can be consumed.
	 */
	private void generateThrowInBodyCall() {
		// prepare for calls
		// we expect exception on stack so we must swap as exception is last argument in the call
		loadHookDispatcher();
		swap();

		// first push method id
		push(methodId);
		// can not just swap because method id is long, thus a bit of gymnastic
		// r-l-l2
		dup2X1();
		// l-l2-r-l-l2
		pop2();
		// l-l2-r :)

		// then this object or null if's static
		if (isStatic) {
			pushNull();
		} else {
			loadThis();
		}
		swap();

		// then parameters
		loadArgArray();
		swap();

		// execute after body
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IHOOK_DISPATCHER_INTERNAL_NAME, "dispatchConstructorOnThrowInBody", DISPATCH_CONSTRUCTOR_ON_THROW_BODY_DESCRIPTOR, true);
	}

}
