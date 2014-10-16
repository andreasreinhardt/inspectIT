package info.novatec.inspectit.classcache;

import info.novatec.inspectit.classcache.util.TypeSet;
import info.novatec.inspectit.classcache.util.TypeWithAnnotationsSet;
import info.novatec.inspectit.classcache.util.UpdateableSet;

import java.util.Collections;
import java.util.Set;

/**
 * Models an annotation.
 * 
 * @author Stefan Siegl
 * @author Ivan Senic
 */
public final class AnnotationType extends Type implements TypeWithAnnotations, ImmutableAnnotationType {

	/**
	 * A list of all annotations assigned to this type.
	 */
	private UpdateableSet<AnnotationType> annotations = null;

	/**
	 * A list of methods annotated with this interface.
	 */
	private UpdateableSet<TypeWithAnnotations> annotatedTypes = null;

	/**
	 * Creates a new <code> AnnotationType </code> without setting the hash and the modifiers. This
	 * constructor is usually used if you want to add the entity without the class being loaded.
	 * 
	 * @param fqn
	 *            fully qualified name.
	 */
	public AnnotationType(String fqn) {
		super(fqn);
	}

	/**
	 * Creates a new <code> AnnotationType </code>. This constructor is usually used if the
	 * annotation is loaded.
	 * 
	 * @param fqn
	 *            fully qualified name.
	 * @param hash
	 *            the hash of the byte code.
	 * @param modifiers
	 *            the modifiers of the annotation.
	 */
	public AnnotationType(String fqn, String hash, int modifiers) {
		super(fqn, hash, modifiers);
	}

	/**
	 * Adds a class that is annotated with this annotation and ensures that the back-reference on
	 * the referred entity is set as well.
	 * 
	 * @param type
	 *            the class that uses this annotation.
	 */
	public void addAnnotatedType(TypeWithAnnotations type) {
		addAnnotatedClassNoBidirectionalUpdate(type);
		type.addAnnotationNoBidirectionalUpdate(this);
	}

	/**
	 * Adds a class that is annotated with this annotation WITHOUT setting the back-reference.
	 * Please be aware that this method should only be called internally as this might mess up the
	 * bi-directional structure.
	 * 
	 * @param type
	 *            the class that uses this annotation.
	 */
	public void addAnnotatedClassNoBidirectionalUpdate(TypeWithAnnotations type) {
		if (null == annotatedTypes) {
			initAnnotatedTypes();
		}
		annotatedTypes.addOrUpdate(type);
	}

	/**
	 * Init {@link #annotatedTypes}.
	 */
	private void initAnnotatedTypes() {
		annotatedTypes = new TypeWithAnnotationsSet();
	}

	/**
	 * Gets {@link #annotatedMethods} as an unmodifiableList. If you want to add something to the
	 * list, use the provided adders, as they ensure that the bidirectional links are created.
	 * 
	 * @return {@link #annotatedMethods}
	 */
	public Set<TypeWithAnnotations> getAnnotatedTypes() {
		if (null == annotatedTypes) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(annotatedTypes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<? extends ImmutableTypeWithAnnotations> getImmutableAnnotatedTypes() {
		return getAnnotatedTypes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAnnotation(AnnotationType annotationType) {
		addAnnotationNoBidirectionalUpdate(annotationType);
		annotationType.addAnnotatedType(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAnnotationNoBidirectionalUpdate(AnnotationType annotationType) {
		if (null == annotations) {
			initAnnotations();
		}
		annotations.addOrUpdate(annotationType);
	}

	/**
	 * Init {@link #annotations}.
	 */
	private void initAnnotations() {
		annotations = new TypeSet<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AnnotationType> getAnnotations() {
		if (null == annotations) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(annotations);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<? extends ImmutableAnnotationType> getImmutableAnnotations() {
		return getAnnotations();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearUnmeaningfulBackReferences() {
		annotatedTypes = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "AnnotationType [fqn=" + fqn + ", hashes=" + hashes + "]";
	}
}