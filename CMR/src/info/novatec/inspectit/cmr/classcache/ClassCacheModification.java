package info.novatec.inspectit.cmr.classcache;

import info.novatec.inspectit.classcache.AbstractInterfaceType;
import info.novatec.inspectit.classcache.AnnotationType;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableType;
import info.novatec.inspectit.classcache.InterfaceType;
import info.novatec.inspectit.classcache.MethodType;
import info.novatec.inspectit.classcache.Modifiers;
import info.novatec.inspectit.classcache.Type;
import info.novatec.inspectit.classcache.TypeWithAnnotations;
import info.novatec.inspectit.classcache.TypeWithMethods;
import info.novatec.inspectit.cmr.classcache.events.Events;
import info.novatec.inspectit.cmr.classcache.events.NodeEvent;
import info.novatec.inspectit.cmr.classcache.events.NodeEvent.NodeEventDetails;
import info.novatec.inspectit.cmr.classcache.events.NodeEvent.NodeEventType;
import info.novatec.inspectit.cmr.classcache.events.ReferenceEvent;
import info.novatec.inspectit.cmr.classcache.events.ReferenceEvent.ReferenceType;
import info.novatec.inspectit.spring.logger.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The <code>ClassCacheModification</code> service provides safe entry points to change the class
 * cache.
 * 
 * The clients of the class cache should not acquire this class directly, but rather ask the
 * <code>ClassCache</code> for its ModificationService.
 * 
 * @author Stefan Siegl
 * @author Ivan Senic
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy
class ClassCacheModification implements IClassCacheModification {

	/**
	 * Log of the class.
	 */
	@Log
	Logger log;

	/**
	 * Lookup service. Package access for easier testing. No auto-wiring here on purpose!
	 */
	ClassCacheLookup lookup;

	/**
	 * Class Cache. Package access for easier testing. No auto-wiring here on purpose!
	 */
	ClassCache classCache;

	/**
	 * Creates a new instance of the modification service. Note that this constructor is package
	 * access on purpose to allow the class cache to create instances but to disallow creating
	 * instances from the outside.
	 * 
	 * Note that the lookup service is provided here explicitly to show that this service needs an
	 * already initialized lookup service.
	 * 
	 * @param classCache
	 *            the class cache reference.
	 * @param lookup
	 *            the lookup service for this class cache.
	 */
	public void init(ClassCache classCache, ClassCacheLookup lookup) {
		this.classCache = classCache;
		this.lookup = lookup;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Events merge(ImmutableType givenAsImmutable) throws ClassCacheModificationException {
		if (null == givenAsImmutable) {
			throw new ClassCacheModificationException("The given model instance was null");
		}

		if (null == givenAsImmutable.getFQN()) {
			throw new ClassCacheModificationException("The given model instance does not provide a FQN (was null)");
		}

		final Type given = (Type) givenAsImmutable;

		// (check) given type needs to be initialized, meaning FQN, hash and modifiers need to
		// be set at least
		if (!given.isInitialized()) {
			throw new ClassCacheModificationException("The instance of the type is not yet initialized. At the very least the basic type needs to be initialized.");
		}

		// running with write lock
		try {
			return classCache.executeWithWriteLock(new Callable<Events>() {

				@Override
				public Events call() throws Exception {
					Events events = new Events();

					// handle basic error conditions early prior to executing the merge.
					Type inStructureRaw = (Type) lookup.findByFQN(given.getFQN());

					if (null != inStructureRaw) {
						// (shortcut) Safety check, if we have the same hash (or the hash is already
						// in the
						// type), then we are done. Note that this check needs to be done after the
						// check
						// for the FQN otherwise we need to add this one here as well.
						if (inStructureRaw.containsHash(given.getHash())) {
							return events;
						}
					}

					handleBaseEntity(inStructureRaw, given, events);

					return events;
				}
			});
		} catch (ClassCacheModificationException e) { // NOPMD
			throw e;
		} catch (Exception e) {
			// this should never happen
			// packing into runtime exception
			throw new RuntimeException("Unexpected exception occurred while modifying the class cache.", e);
		}
	}

	/**
	 * merges/adds the given entity to the type in the structure.
	 * 
	 * @param inputType
	 *            the type in the structure or null if not existing
	 * @param givenType
	 *            the given entity
	 * @param events
	 *            write notifications here.
	 */
	private void handleBaseEntity(Type inputType, Type givenType, Events events) {
		if (null == inputType) {
			// ADD the new element to the structure
			fireAndSave(new NodeEvent(givenType, NodeEventType.NEW, NodeEventDetails.INITIALIZED, givenType.getHash()), events);

			// RESOLVE the references of the type
			resolveReferences(givenType, events);
		} else {
			if (givenType instanceof AnnotationType && inputType instanceof InterfaceType) {

				// special case, when annotation is used as interface must be handled in different
				// way
				mergeAnnotationAsInterface((AnnotationType) givenType, (InterfaceType) inputType, events);
			} else {

				// MERGE the existing element with the information from the given entity
				merge(inputType, givenType, events);
			}
		}
	}

	/**
	 * Resolves the references of the given type and ensures that references to already existing
	 * entities in the structure are used and new entities - unknown to the class cache - are added.
	 * 
	 * @param base
	 *            the type.
	 * @param events
	 *            write notifications here.
	 */
	private void resolveReferences(Type base, Events events) {
		// remove unmeaningful references (for safety), as we are using this reference within the
		// structure directly and do not copy it
		base.clearUnmeaningfulBackReferences();

		if (base instanceof TypeWithAnnotations) {
			addAnnotations((TypeWithAnnotations) base, ((TypeWithAnnotations) base).getAnnotations(), events);
		}

		if (base instanceof TypeWithMethods) {
			resolveMethodReferences(((TypeWithMethods) base).getMethods(), events);
		}

		if (base instanceof ClassType) {
			addSuperclass((ClassType) base, ((ClassType) base).getSuperClasses(), events);
			addInterface((ClassType) base, ((ClassType) base).getRealizedInterfaces(), events);
		}

		if (base instanceof InterfaceType) {
			addSuperinterface((InterfaceType) base, ((InterfaceType) base).getSuperInterfaces(), events);
		}

		// nothing to do for AnnotationTypes - everything handled already at TypeWithAnnotations.
	}

	/**
	 * resolve the references of the methods of the given type.
	 * 
	 * @param methods
	 *            the methods to resolve.
	 * @param events
	 *            change events.
	 */
	private void resolveMethodReferences(Set<MethodType> methods, Events events) {
		for (MethodType m : methods) {
			resolveMethodReferences(m, events);
		}
	}

	/**
	 * resolve the method references of a method.
	 * 
	 * @param method
	 *            the method type.
	 * @param events
	 *            the change events.
	 * @return the method.
	 */
	private MethodType resolveMethodReferences(MethodType method, Events events) {
		for (AnnotationType a : method.getAnnotations()) {
			AnnotationType lookupUp = getOrAddReferredType(a, events);
			method.addAnnotation(lookupUp);
		}

		for (ClassType t : method.getExceptions()) {
			ClassType lookup = getOrAddReferredType(t, events);
			method.addException(lookup);
		}

		return method;
	}

	/**
	 * adds all annotations to this type.
	 * 
	 * @param type
	 *            to add the annotations to.
	 * @param annotations
	 *            the annotations to add.
	 * @param events
	 *            write notifications here.
	 */
	private void addAnnotations(TypeWithAnnotations type, Set<AnnotationType> annotations, Events events) {
		for (AnnotationType annotationType : annotations) {
			AnnotationType lookupUp = getOrAddReferredType(annotationType, events);
			fireAndSave(new ReferenceEvent((Type) type, lookupUp, ReferenceType.ANNOTATION), events);
			((TypeWithAnnotations) type).addAnnotation(lookupUp);
		}
	}

	/**
	 * Adds all super classes to the class type.
	 * 
	 * @param type
	 *            to add the super-classes to.
	 * @param superclasses
	 *            the super-classes to add.
	 * @param events
	 *            write notifications here.
	 */
	private void addSuperclass(ClassType type, Set<ClassType> superclasses, Events events) {
		for (ClassType superClass : superclasses) {
			ClassType lookupUp = getOrAddReferredType(superClass, events);
			fireAndSave(new ReferenceEvent(type, lookupUp, ReferenceType.SUPERCLASS), events);
			type.addSuperClass(lookupUp);
		}
	}

	/**
	 * adds all interfaces to the class type.
	 * 
	 * @param type
	 *            to add the interfaces to.
	 * @param interfaces
	 *            the interfaces to add.
	 * @param events
	 *            write notifications here.
	 */
	private void addInterface(ClassType type, Set<AbstractInterfaceType> interfaces, Events events) {
		for (AbstractInterfaceType i : interfaces) {
			AbstractInterfaceType lookupUp = getOrAddReferredType(i, events);
			fireAndSave(new ReferenceEvent(type, lookupUp, ReferenceType.REALIZE_INTERFACE), events);
			type.addInterface(lookupUp);
		}
	}

	/**
	 * adds all super interfaces to the interface type.
	 * 
	 * @param type
	 *            to add the super interfaces to.
	 * @param superinterfaces
	 *            the super interfaces to add.
	 * @param events
	 *            write notifications here.
	 */
	private void addSuperinterface(InterfaceType type, Set<InterfaceType> superinterfaces, Events events) {
		for (InterfaceType i : superinterfaces) {
			InterfaceType lookupUp = getOrAddReferredType(i, events);
			fireAndSave(new ReferenceEvent(type, lookupUp, ReferenceType.SUPERINTERFACE), events);
			type.addSuperInterface(lookupUp);
		}
	}

	/**
	 * Merges two entities with methods.
	 * 
	 * @param base
	 *            entity in the storage.
	 * @param given
	 *            entity that was given
	 * @param events
	 *            list of events.
	 */
	private void mergeMethods(TypeWithMethods base, TypeWithMethods given, Events events) {
		// go over the method of the given entity
		for (MethodType givenMethod : given.getMethods()) {
			// check if this method is already in the stored class.
			if (base.getMethods().contains(givenMethod)) {
				for (MethodType baseMethod : base.getMethods()) {
					if (!baseMethod.equals(givenMethod)) {
						continue;
					}

					boolean changed = false;

					for (AnnotationType a : findNewEntries(baseMethod.getAnnotations(), givenMethod.getAnnotations())) {
						changed = true;
						baseMethod.addAnnotation(getOrAddReferredType(a, events));
					}

					for (ClassType a : findNewEntries(baseMethod.getExceptions(), givenMethod.getExceptions())) {
						changed = true;
						baseMethod.addException(getOrAddReferredType(a, events));
					}

					if (mergeModifiers(baseMethod, givenMethod)) {
						changed = true;
					}

					if (changed) {
						fireAndSave(new NodeEvent((Type) base, NodeEventType.CHANGED, NodeEventDetails.METHOD_CHANGED_OR_ADDED), events);
					}

					break;
				}
			} else {
				// add it but also resolve it first
				fireAndSave(new NodeEvent((Type) base, NodeEventType.CHANGED, NodeEventDetails.METHOD_CHANGED_OR_ADDED), events);
				base.addMethod(resolveMethodReferences(givenMethod, events));
			}
		}
	}

	/**
	 * Merges the given type onto the base type and write change events to the given events list.
	 * 
	 * @param base
	 *            the base type (usually from the structure)
	 * @param given
	 *            the given type.
	 * @param events
	 *            the events list the change events are pushed to.
	 * @param <T>
	 *            The entity type.
	 */
	private <T extends Type> void merge(T base, T given, Events events) {
		// we already know that we have a new hash
		base.addHash(given.getHash());
		if (!base.isInitialized()) {
			fireAndSave(new NodeEvent(base, NodeEventType.CHANGED, NodeEventDetails.INITIALIZED, given.getHash()), events);
		} else {
			fireAndSave(new NodeEvent(base, NodeEventType.CHANGED, NodeEventDetails.HASH_ADDED, given.getHash()), events);
		}

		if (mergeModifiers(base, given)) {
			fireAndSave(new NodeEvent(base, NodeEventType.CHANGED, NodeEventDetails.MODIFIERS_CHANGED), events);
		}

		if (given instanceof TypeWithAnnotations) {
			addAnnotations((TypeWithAnnotations) base, findNewEntries(((TypeWithAnnotations) base).getAnnotations(), ((TypeWithAnnotations) given).getAnnotations()), events);
		}

		if (given instanceof TypeWithMethods) {
			mergeMethods((TypeWithMethods) base, ((TypeWithMethods) given), events);
		}

		if (given instanceof ClassType) {
			addSuperclass((ClassType) base, findNewEntries(((ClassType) base).getSuperClasses(), ((ClassType) given).getSuperClasses()), events);
			addInterface((ClassType) base, findNewEntries(((ClassType) base).getRealizedInterfaces(), ((ClassType) given).getRealizedInterfaces()), events);
		}

		if (given instanceof InterfaceType) {
			addSuperinterface((InterfaceType) base, findNewEntries(((InterfaceType) base).getSuperInterfaces(), ((InterfaceType) given).getSuperInterfaces()), events);
		}
	}

	/**
	 * Merges the modifiers of two types and store the result to the base type.
	 * 
	 * @param base
	 *            the base type
	 * @param given
	 *            the given type
	 * @return if there were any changes
	 */
	private boolean mergeModifiers(Type base, Type given) {
		int baseMod = base.getModifiers();
		base.setModifiers(Modifiers.mergeModifiers(base.getModifiers(), given.getModifiers()));
		return baseMod != base.getModifiers();
	}

	/**
	 * merges the modifiers.
	 * 
	 * @param base
	 *            the base type
	 * @param given
	 *            the given modifiers
	 * @return if there were any changes
	 */
	private boolean mergeModifiers(MethodType base, MethodType given) {
		int baseMod = base.getModifiers();
		base.setModifiers(Modifiers.mergeModifiers(base.getModifiers(), given.getModifiers()));
		return baseMod != base.getModifiers();
	}

	/**
	 * Checks the current class cache if the given type is already available. In this case, the
	 * available type is returned. If the type is not available, the given type is added to the
	 * structure and returned.
	 * 
	 * @param given
	 *            the given type.
	 * @param events
	 *            add change events this this list.
	 * @param <T>
	 *            the type of the entries in the set.
	 * @return either the type in the structure representing the given type or the given type.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Type> T getOrAddReferredType(T given, Events events) {
		if (given.isInitialized()) {
			log.warn("We found that the given structure also has the second level initialized. This use case is currently not addressed by the merger. We thus interpret the given 2nd level as uninitialized and will add it as such if no entry exists.");
		}
		T inStructure = (T) lookup.findByFQN(given.getFQN());
		if (null == inStructure) {
			fireAndSave(new NodeEvent(given, NodeEventType.NEW, NodeEventDetails.NOT_INITIALIZED), events);
			return given;
		} else {
			return inStructure;
		}
	}

	/**
	 * Merges given annotation when it exist in the structure as interface.
	 * 
	 * @param given
	 *            {@link AnnotationType}
	 * @param inStructure
	 *            {@link InterfaceType}
	 * @param events
	 *            Eventts to process.
	 */
	private void mergeAnnotationAsInterface(AnnotationType given, InterfaceType inStructure, Events events) {
		for (ClassType classType : inStructure.getRealizingClasses()) {
			// this should update the references in the class type correctly
			classType.addInterface(given);
		}

		// must be specified as new, no other way
		fireAndSave(new NodeEvent(given, NodeEventType.NEW, NodeEventDetails.INITIALIZED, given.getHash()), events);
	}

	/**
	 * Fires up the {@link NodeEvent} and saves it to the {@link Events}.
	 * 
	 * @param nodeEvent
	 *            {@link NodeEvent}
	 * @param events
	 *            Events to add event to.
	 */
	private void fireAndSave(NodeEvent nodeEvent, Events events) {
		events.addEvent(nodeEvent);
		classCache.informNodeChange(nodeEvent);
	}

	/**
	 * Fires up the {@link NodeEvent} and saves it to the {@link Events}.
	 * 
	 * @param referenceEvent
	 *            {@link ReferenceEvent}
	 * @param events
	 *            Events to add event to.
	 */
	private void fireAndSave(ReferenceEvent referenceEvent, Events events) {
		events.addEvent(referenceEvent);
		classCache.informReferenceChange(referenceEvent);
	}

	/**
	 * finds entries that are available in the given set, but not in the base set.
	 * 
	 * @param base
	 *            base set
	 * @param given
	 *            given set
	 * @param <T>
	 *            the type of the entries in the set.
	 * @return difference.
	 */
	private <T> Set<T> findNewEntries(Set<T> base, Set<T> given) {
		Set<T> result = new HashSet<>();
		for (T found : given) {
			if (!base.contains(found)) {
				result.add(found);
			}
		}
		return result;
	}

}
