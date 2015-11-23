package info.novatec.inspectit.classcache;

import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.classcache.util.TypeSet;
import info.novatec.inspectit.classcache.util.UpdateableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Models a method used in classes and interfaces.
 * 
 * @author Stefan Siegl
 * @author Ivan Senic
 */
public class MethodType implements TypeWithAnnotations, ImmutableMethodType {

	/**
	 * The type of the method.
	 * 
	 * @author Stefan Siegl
	 */
	public enum Character {
		/** A method. */
		METHOD,

		/** A constructor. */
		CONSTRUCTOR;
	}

	/**
	 * The name of the method.
	 */
	private String name;

	/**
	 * The modifier of the method.
	 */
	private int modifiers;

	/**
	 * Return type of the method.
	 */
	private String returnType;

	/**
	 * Ordered list of all parameters of this method.
	 */
	private List<String> parameters = null;

	/**
	 * List of all exceptions this method throws.
	 */
	private UpdateableSet<ClassType> exceptions = null;

	/**
	 * List of annotations of this method.
	 */
	private UpdateableSet<AnnotationType> annotations = null;

	/**
	 * The class this method belongs to.
	 */
	private TypeWithMethods classOrInterface;

	/**
	 * {@link RegisteredSensorConfig} denoting that this method is instrumented.
	 */
	private RegisteredSensorConfig registeredSensorConfig;

	/**
	 * Gets {@link #classType}.
	 * 
	 * @return {@link #classType}
	 */
	public TypeWithMethods getClassOrInterfaceType() {
		return classOrInterface;
	}

	/**
	 * Returns {@link ImmutableTypeWithMethods} being this method's class or interface.
	 * 
	 * @return Returns {@link ImmutableTypeWithMethods} being this method's class or interface.
	 */
	public ImmutableTypeWithMethods getImmutableClassOrInterfaceType() {
		return classOrInterface;
	}

	/**
	 * Sets {@link #classType}.
	 * 
	 * @param type
	 *            New value for {@link #classType}
	 */
	public void setClassOrInterfaceType(TypeWithMethods type) {
		setClassOrInterfaceTypeNoBidirectionalUpdate(type);
		type.addMethodNoBidirectionalUpdate(this);
	}

	/**
	 * Sets {@link #classType} without updating the back reference.
	 * 
	 * @param type
	 *            New value for {@link #classType}
	 */
	public void setClassOrInterfaceTypeNoBidirectionalUpdate(TypeWithMethods type) {
		this.classOrInterface = type;
	}

	/**
	 * Gets {@link #name}.
	 * 
	 * @return {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@link #name}.
	 * 
	 * @param name
	 *            New value for {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets {@link #modifiers}.
	 * 
	 * @return {@link #modifiers}
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
	}

	/**
	 * {@inheritDoc}
	 */
	public Character getMethodCharacter() {
		// cinit is static initialization method, we consider it as constructor
		if ("<init>".equals(name) || "<cinit>".equals(name)) {
			return Character.CONSTRUCTOR;
		} else {
			return Character.METHOD;
		}
	}

	/**
	 * Gets {@link #returnType}.
	 * 
	 * @return {@link #returnType}
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Sets {@link #returnType}.
	 * 
	 * @param returnType
	 *            New value for {@link #returnType}
	 */
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	/**
	 * Gets {@link #parameters}.
	 * 
	 * @return {@link #parameters}
	 */
	public List<String> getParameters() {
		if (null == parameters) {
			return Collections.emptyList();
		}
		return parameters;
	}

	/**
	 * Sets {@link #parameters}.
	 * 
	 * @param parameters
	 *            New value for {@link #parameters}
	 */
	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * sets a parameter at the specified index.
	 * 
	 * @param index
	 *            index
	 * @param type
	 *            type.
	 */
	public void setParameterAt(int index, String type) {
		if (null == parameters) {
			initParameters();
		}
		parameters.set(index, type);
	}

	/**
	 * Init {@link #parameters}.
	 */
	private void initParameters() {
		parameters = new ArrayList<String>(1);
	}

	/**
	 * Adds an exception thrown by this method and ensures that the back-reference on the referred
	 * entity is set as well.
	 * 
	 * @param type
	 *            the exception that is thrown.
	 */
	public void addException(ClassType type) {
		addExceptionNoBidirectionalUpdate(type);
		type.addMethodThrowingExceptionNoBidirectionalUpdate(this);
	}

	/**
	 * Adds an exception thrown by this method WITHOUT setting the back-reference. Please be aware
	 * that this method should only be called internally as this might mess up the bidirectional
	 * structure.
	 * 
	 * @param type
	 *            the exception that is thrown.
	 */
	public void addExceptionNoBidirectionalUpdate(ClassType type) {
		if (null == exceptions) {
			initExceptions();
		}
		exceptions.addOrUpdate(type);
	}

	/**
	 * Init {@link #exceptions}.
	 */
	private void initExceptions() {
		exceptions = new TypeSet<ClassType>();
	}

	/**
	 * Gets {@link #exceptions} as an unmodifiableSet. If you want to add something to the list, use
	 * the provided adders, as they ensure that the bidirectional links are created.
	 * 
	 * @return {@link #exceptions}
	 */
	public Set<ClassType> getExceptions() {
		if (null == exceptions) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(exceptions);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<? extends ImmutableClassType> getImmutableExceptions() {
		return getExceptions();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addAnnotation(AnnotationType annotationType) {
		addAnnotationNoBidirectionalUpdate(annotationType);
		annotationType.addAnnotatedType(this);
	}

	/**
	 * {@inheritDoc}
	 */
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
		annotations = new TypeSet<AnnotationType>();
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<AnnotationType> getAnnotations() {
		if (null == annotations) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(annotations);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<? extends ImmutableAnnotationType> getImmutableAnnotations() {
		return getAnnotations();
	}

	/**
	 * Gets {@link #registeredSensorConfig}.
	 * 
	 * @return {@link #registeredSensorConfig}
	 */
	public RegisteredSensorConfig getRegisteredSensorConfig() {
		return registeredSensorConfig;
	}

	/**
	 * Sets {@link #registeredSensorConfig}.
	 * 
	 * @param registeredSensorConfig
	 *            New value for {@link #registeredSensorConfig}
	 */
	public void setRegisteredSensorConfig(RegisteredSensorConfig registeredSensorConfig) {
		this.registeredSensorConfig = registeredSensorConfig;
	}

	/**
	 * Factory method to easily build methods. Mainly used within testing.
	 * 
	 * @param name
	 *            the method name.
	 * @param modifiers
	 *            the modifiers.
	 * @param returnType
	 *            the return types.
	 * @param parameters
	 *            the parameters.
	 * @param exceptions
	 *            the exceptions.
	 * @param annotations
	 *            the annotations.
	 * @return an initialized method instance.
	 */
	public static MethodType build(String name, int modifiers, String returnType, List<String> parameters, Set<ClassType> exceptions, Set<AnnotationType> annotations) {
		// bidirectional setting needs an correctly constructed MethodType, thus we cannot have this
		// in the constructor itself.
		MethodType type = new MethodType();
		type.name = name;
		type.modifiers = modifiers;

		if (null != returnType) {
			type.returnType = returnType;
		}
		if (null != parameters) {
			type.parameters = parameters;
		}
		if (null != annotations) {
			for (AnnotationType a : annotations) {
				type.addAnnotation(a);
			}
		}
		if (null != exceptions) {
			for (ClassType e : exceptions) {
				type.addException(e);
			}
		}

		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
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
		MethodType other = (MethodType) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!parameters.equals(other.parameters)) {
			return false;
		}
		if (returnType == null) {
			if (other.returnType != null) {
				return false;
			}
		} else if (!returnType.equals(other.returnType)) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "MethodType [name=" + name + ", modifiers=" + modifiers + ", methodCharacter=" + getMethodCharacter() + ", returnType=" + returnType + ", parameters=" + parameters + "]";
	}

}
