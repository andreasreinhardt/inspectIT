package info.novatec.inspectit.cmr.classcache.config.job;

import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.cmr.service.IRegistrationService;
import info.novatec.inspectit.cmr.service.RegistrationService;
import info.novatec.inspectit.spring.logger.Log;

import java.util.Collection;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Job for updating the time-stamp of the {@link RegisteredSensorConfig}s in the database.
 * 
 * @author Ivan Senic
 * 
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Lazy
public class RefreshInstrumentationTimestampsJob implements Runnable {

	/**
	 * Class logger.
	 */
	@Log
	Logger log;

	/**
	 * Class type to refresh the instrumentation points for.
	 */
	private ImmutableClassType classType;

	/**
	 * {@link RegistrationService}.
	 */
	@Autowired
	IRegistrationService registrationService;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		if (!classType.hasInstrumentationPoints()) {
			return;
		}

		Collection<RegisteredSensorConfig> registeredSensorConfigs = classType.getInstrumentationPoints();
		for (RegisteredSensorConfig rsc : registeredSensorConfigs) {
			long methodId = rsc.getId();
			registrationService.refreshMethodIdent(methodId);
			for (long sensorId : rsc.getSensorIds()) {
				registrationService.addSensorTypeToMethod(sensorId, methodId);
			}
		}

	}

	/**
	 * Sets {@link #classType}.
	 * 
	 * @param classType
	 *            New value for {@link #classType}
	 */
	public void setClassType(ImmutableClassType classType) {
		this.classType = classType;
	}

}
