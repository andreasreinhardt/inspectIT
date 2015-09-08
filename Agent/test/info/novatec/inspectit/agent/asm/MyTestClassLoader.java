package info.novatec.inspectit.agent.asm;

@SuppressWarnings("PMD")
public class MyTestClassLoader extends ClassLoader {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

}
