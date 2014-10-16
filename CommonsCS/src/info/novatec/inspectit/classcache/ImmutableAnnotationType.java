package info.novatec.inspectit.classcache;

import java.util.Set;

/**
 * A class cache model element of the type Annotation that only provides immutable access.
 * 
 * @author Stefan Siegl
 */
public interface ImmutableAnnotationType extends ImmutableType {

	/**
	 * Returns immutable annotations as an unmodifiableSet.
	 * 
	 * @return Returns immutable annotations as an unmodifiableSet.
	 */
	Set<? extends ImmutableAnnotationType> getImmutableAnnotations();

	/**
	 * Returns immutable annotated types as an unmodifiableSet.
	 * 
	 * @return Returns immutable annotated types as an unmodifiableSet.
	 */
	Set<? extends ImmutableTypeWithAnnotations> getImmutableAnnotatedTypes();

}
