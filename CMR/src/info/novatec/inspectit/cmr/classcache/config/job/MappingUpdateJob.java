package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.ci.Environment;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Job that is executed when a different environment is mapped to the agent as a result of the
 * mapping update in the CI.
 * 
 * @author Ivan Senic
 * 
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy
public class MappingUpdateJob extends AbstractConfigurationChangeJob {

	/**
	 * New environment connected to the agent. Should be <code>null</code> to denote that new
	 * mapping is have no matching environment for the agent.
	 */
	private Environment environment;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		// first remove all existing instrumentation points
		getInstrumentationPointsUtil().removeAllInstrumentationPoints(getClassCache());

		if (null == environment) {
			// if we have no environment, reset
			clearEnvironmentAndConfiguration();
		} else {
			// first update configuration based on new env
			updateConfiguration(environment);

			// then add instrumentation points
			getInstrumentationPointsUtil().addAllInstrumentationPoints(getClassCache(), getAgentConfiguration(), environment);

			// then add them to configuration
			updateInstrumentationPointsInConfiguration();
		}
	}

	/**
	 * Sets {@link #environment}.
	 * 
	 * @param environment
	 *            New value for {@link #environment}
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
