package info.novatec.inspectit.agent.analyzer.impl;

import info.novatec.inspectit.agent.analyzer.IByteCodeAnalyzer;
import info.novatec.inspectit.agent.analyzer.IClassHashHelper;
import info.novatec.inspectit.agent.asm.ClassAnalyzer;
import info.novatec.inspectit.agent.asm.ClassInstrumenter;
import info.novatec.inspectit.agent.asm.LoaderAwareClassWriter;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.config.StorageException;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IIdManager;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.agent.hooking.IHookDispatcherMapper;
import info.novatec.inspectit.classcache.Type;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.spring.logger.Log;

import java.util.Collection;

import org.apache.commons.codec.digest.DigestUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@link IByteCodeAnalyzer} that uses {@link IConnection} to connect to the CMR and instrument the
 * byte code on the server side.
 * 
 * @author Ivan Senic
 * 
 */
@Component
public class ByteCodeAnalyzer implements IByteCodeAnalyzer {

	/**
	 * Log for the class.
	 */
	@Log
	Logger log;

	/**
	 * Id manager.
	 */
	@Autowired
	IIdManager idManager;

	/**
	 * {@link IConnection}.
	 */
	@Autowired
	IConnection connection;

	/**
	 * {@link IHookDispatcherMapper}.
	 */
	@Autowired
	IHookDispatcherMapper hookDispatcherMapper;

	/**
	 * {@link IClassHashHelper}.
	 */
	@Autowired
	IClassHashHelper classHashHelper;

	/**
	 * Configuration storage.
	 */
	@Autowired
	IConfigurationStorage configurationStorage;

	/**
	 * {@inheritDoc}
	 */
	public byte[] analyzeAndInstrument(byte[] byteCode, String className, final ClassLoader classLoader) {
		try {
			if (null == byteCode) {
				// no support for null byte-codes
				// in future we want to have possibility to analyze also the null byte-codes
				// in earlier versions we used javassist for this, however we want to use something
				// ours in future
				return null;
			}

			// create the hash
			String hash = DigestUtils.sha256Hex(byteCode);

			// TODO when asynch add registerLoaded to class cache helper

			// if not sent we go for the sending
			if (!classHashHelper.isSent(hash)) {
				// TODO when asynch call service in asynch mode

				// parse first
				ClassReader classReader = new ClassReader(byteCode);
				ClassAnalyzer classAnalyzer = new ClassAnalyzer(hash, null, true);
				classReader.accept(classAnalyzer, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				Type type = (Type) classAnalyzer.getType();

				// try connecting to server
				InstrumentationResult instrumentationResult = connection.analyzeAndInstrument(idManager.getPlatformId(), hash, type);

				classHashHelper.registerSent(hash, instrumentationResult);

				return performInstrumentation(byteCode, classLoader, instrumentationResult);
			} else {
				// load instrumentation result from the class hash helper
				InstrumentationResult instrumentationResult = classHashHelper.getInstrumentationResult(hash);

				return performInstrumentation(byteCode, classLoader, instrumentationResult);
			}
		} catch (IdNotAvailableException idNotAvailableException) {
			log.error("Error occurred instrumenting the byte code of class " + className, idNotAvailableException);
			return null;
		} catch (ServerUnavailableException serverUnavailableException) {
			log.error("Error occurred instrumenting the byte code of class " + className, serverUnavailableException);
			return null;
		} catch (BusinessException businessException) {
			log.error("Error occurred instrumenting the byte code of class " + className, businessException);
			return null;
		} catch (StorageException storageException) {
			log.error("Error occurred instrumenting the byte code of class " + className, storageException);
			return null;
		}
	}

	/**
	 * Performs the instrumentation.
	 * 
	 * @param byteCode
	 *            original byte code
	 * @param classLoader
	 *            class loader loading the class
	 * @param instrumentationResult
	 *            {@link InstrumentationResult} holding instrumentation properties.
	 * @return instrumented byte code or <code>null</code> if instrumentation result is
	 *         <code>null</code> or contains no instrumentation points
	 * @throws StorageException
	 *             If storage exception occurs when reading if enhanced exception sensor is active
	 */
	private byte[] performInstrumentation(byte[] byteCode, ClassLoader classLoader, InstrumentationResult instrumentationResult) throws StorageException {
		// if no instrumentation result return null
		if (null == instrumentationResult || instrumentationResult.isEmpty()) {
			return null;
		}

		Collection<RegisteredSensorConfig> instrumentationPoints = instrumentationResult.getRegisteredSensorConfigs();
		boolean classLoadingDelegation = instrumentationResult.isClassLoadingDelegation();

		// here do the instrumentation
		ClassReader classReader = new ClassReader(byteCode);
		ClassWriter classWriter = new LoaderAwareClassWriter(ClassWriter.COMPUTE_FRAMES, classLoader);
		ClassInstrumenter classInstrumenter = new ClassInstrumenter(classWriter, instrumentationPoints, configurationStorage.isEnhancedExceptionSensorActivated(), classLoadingDelegation);
		classReader.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		// map the instrumentation points if we have them
		for (RegisteredSensorConfig registeredSensorConfig : instrumentationPoints) {
			hookDispatcherMapper.addMapping(registeredSensorConfig.getId(), registeredSensorConfig);
		}

		return classWriter.toByteArray();
	}

}
