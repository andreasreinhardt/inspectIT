package info.novatec.inspectit.agent.config.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import info.novatec.inspectit.agent.config.PriorityEnum;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for the {@link RegisteredSensorConfig}.
 * 
 * @author Ivan Senic
 * 
 */
@SuppressWarnings("PMD")
public class RegisteredSensorConfigTest {

	private RegisteredSensorConfig registeredSensorConfig;

	@BeforeMethod
	public void init() {
		registeredSensorConfig = new RegisteredSensorConfig();
	}
	
	@Test
	public void containsSensorIds() {
		registeredSensorConfig.addSensorId(1, PriorityEnum.MIN);
		registeredSensorConfig.addSensorId(2, PriorityEnum.MAX);

		assertThat(registeredSensorConfig.containsSensorId(1), is(true));
		assertThat(registeredSensorConfig.containsSensorId(2), is(true));
	}

	@Test
	public void noDoubleSensorId() {
		registeredSensorConfig.addSensorId(1, PriorityEnum.MIN);
		registeredSensorConfig.addSensorId(2, PriorityEnum.MAX);
		registeredSensorConfig.addSensorId(1, PriorityEnum.MAX);
		registeredSensorConfig.addSensorId(2, PriorityEnum.MIN);

		long[] sensorIds = registeredSensorConfig.getSensorIds();
		assertThat(sensorIds.length, is(2));
	}

	@Test
	public void sensorIdProprities() {
		registeredSensorConfig.addSensorId(1, PriorityEnum.MAX);
		registeredSensorConfig.addSensorId(2, PriorityEnum.MIN);

		long[] sensorIds = registeredSensorConfig.getSensorIds();
		assertThat(sensorIds.length, is(2));
		assertThat(sensorIds[0], is(1L));
		assertThat(sensorIds[1], is(2L));
	}

	@Test
	public void sensorIdPropritiesReverse() {
		registeredSensorConfig.addSensorId(1, PriorityEnum.MIN);
		registeredSensorConfig.addSensorId(2, PriorityEnum.MAX);

		long[] sensorIds = registeredSensorConfig.getSensorIds();
		assertThat(sensorIds.length, is(2));
		// highest priority first
		assertThat(sensorIds[0], is(2L));
		assertThat(sensorIds[1], is(1L));
	}
}
