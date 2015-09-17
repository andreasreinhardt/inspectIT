package info.novatec.inspectit.classcache;


import java.util.Set;

/**
 * A class cache model element of a type that can have annotation, which only provides immutable
 * access.
 * 
 * @author Stefan Siegl
 */
public interface ImmutableTypeWithAnnotations {

	/**
	 * Returns immutable annotations as an unmodifiableSet.
	 * 
	 * @return Returns immutable annotations as an unmodifiableSet.
	 */
	Set<? extends ImmutableAnnotationType> getImmutableAnnotations();
}
