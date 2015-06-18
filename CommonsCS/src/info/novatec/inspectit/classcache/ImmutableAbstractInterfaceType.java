package info.novatec.inspectit.classcache;

import java.util.Set;

public interface ImmutableAbstractInterfaceType extends ImmutableType {

	/**
	 * Gets immutable realizing classes as an unmodifiableSet.
	 * 
	 * @return Immutable realizing classes as an unmodifiableSet.
	 */
	Set<? extends ImmutableClassType> getImmutableRealizingClasses();
}
