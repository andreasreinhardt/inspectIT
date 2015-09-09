package info.novatec.inspectit.classcache;


import info.novatec.inspectit.classcache.util.ArraySet;

import java.util.Collections;
import java.util.Set;

/**
 * Base type of most model types.
 * 
 * @author Stefan Siegl
 */
public abstract class Type implements ImmutableType {

	/**
	 * The FQN of the type.
	 */
	protected final String fqn;

	/**
	 * Marks whether the type is completely initialized.
	 */
	protected boolean initialized = false;

	/**
	 * The hash of the byte code of this class. As we can have multiple version, we can keep a list
	 * of hashes.
	 */
	protected Set<String> hashes;

	/**
	 * The modifiers of the type.
	 */
	protected int modifiers;

	/**
	 * Creates a new <code> Type </code> instance. This constructor is usually invoked to add a
	 * <code>Type</code> at a point when the hashes and the modifiers are not yet available.
	 * 
	 * @param fqn
	 *            fully qualified name.
	 */
	public Type(String fqn) {
		this.fqn = fqn;
	}

	/**
	 * Creates a new <code> Type </code> instance. This constructor is usually used when all
	 * information is available.
	 * 
	 * @param fqn
	 *            fully qualified name.
	 * @param hash
	 *            the hash of the bytecode.
	 * @param modifiers
	 *            the modifiers of the class.
	 */
	public Type(String fqn, String hash, int modifiers) {
		this.fqn = fqn;
		this.modifiers = modifiers;
		initHashes();
		hashes.add(hash);
		initialized = true;
	}


	/**
	 * Removes all references from the type which are only back references. The merging logic will
	 * ensure that a given instance will only have forward references set by executing this method.
	 */
	public abstract void clearUnmeaningfulBackReferences();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFQN() {
		return fqn;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasMultipleVersions() {
		if (null == hashes) {
			return false;
		}
		return hashes.size() > 1;
	}

	/**
	 * Adds a byte code hash.
	 * 
	 * @param hash
	 *            the byte code hash.
	 */
	public void addHash(String hash) {
		if (null == hashes) {
			initHashes();
		}
		hashes.add(hash);
		checkInitialized();
	}

	/**
	 * Init {@link #hashes}.
	 */
	private void initHashes() {
		hashes = new ArraySet<>(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsHash(String hash) {
		if (null == hashes) {
			return false;
		}
		return hashes.contains(hash);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getHashes() {
		if (null == hashes) {
			return Collections.emptySet();
		}
		return hashes;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getHash() {
		if (null == hashes) {
			return null;
		}
		return hashes.iterator().next();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getModifiers() {
		return modifiers;
	}

	/**
	 * Sets {@link #modifiers}.
	 * 
	 * @param modifiers
	 *            New value for {@link #modifiers}
	 */
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
		checkInitialized();
	}

	/**
	 * Checks whether the type is completely initialized.
	 */
	protected void checkInitialized() {
		if (!getHashes().isEmpty() && 0 != modifiers) {
			initialized = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAnnotation() {
		return AnnotationType.class.isAssignableFrom(this.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isInterface() {
		return InterfaceType.class.isAssignableFrom(this.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isClass() {
		return ClassType.class.isAssignableFrom(this.getClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableClassType castToClass() {
		return (ImmutableClassType) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableAnnotationType castToAnnotation() {
		return (ImmutableAnnotationType) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ImmutableInterfaceType castToInterface() {
		return (ImmutableInterfaceType) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fqn == null) ? 0 : fqn.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Type other = (Type) obj;
		if (fqn == null) {
			if (other.fqn != null) {
				return false;
			}
		} else if (!fqn.equals(other.fqn)) {
			return false;
		}
		return true;
	}

}
