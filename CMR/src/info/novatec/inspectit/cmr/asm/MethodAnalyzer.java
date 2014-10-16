package info.novatec.inspectit.cmr.asm;

import info.novatec.inspectit.classcache.AnnotationType;
import info.novatec.inspectit.classcache.MethodType;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Method analyzer for picking up annotations and arguments of a method.
 * 
 * @author Ivan Senic
 * 
 */
public class MethodAnalyzer extends MethodVisitor {

	/**
	 * Method type to store the read information to.
	 */
	private MethodType methodType;
	
	/**
	 * Should prior to setting the FQNs to the created type {@link String#intern()} be called to
	 * optimize the strings in memory. It's expected that this operation provide memory benefits but
	 * to cost additional processing time.
	 */
	private boolean internFQNs;

	/**
	 * Default constructor.
	 * 
	 * @param methodType
	 *            Method type to store the read information to.
	 * @param internFQNs
	 *            Should prior to setting the FQNs to the created type {@link String#intern()} be
	 *            called to optimize the strings in memory. It's expected that this operation
	 *            provide memory benefits but to cost additional processing time.
	 * @param mv
	 *            Parent method visitor.
	 */
	public MethodAnalyzer(MethodType methodType, boolean internFQNs, MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
		this.methodType = methodType;
		this.internFQNs = internFQNs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// calling super to ensure the visitor pattern
		AnnotationVisitor annotationVisitor = super.visitAnnotation(desc, visible);

		if (null == methodType) {
			return annotationVisitor;
		}

		String internalName = Type.getType(desc).getInternalName();
		methodType.addAnnotation(new AnnotationType(AsmUtil.getFqn(internalName, internFQNs)));

		return annotationVisitor;
	}

}