package info.novatec.inspectit.agent.analyzer.impl;

import info.novatec.inspectit.agent.analyzer.IByteCodeAnalyzer;
import info.novatec.inspectit.agent.analyzer.IClassPoolAnalyzer;
import info.novatec.inspectit.agent.asm.ClassInstrumenter;
import info.novatec.inspectit.agent.asm.LoaderAwareClassWriter;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.agent.config.impl.RegisteredSensorConfig;
import info.novatec.inspectit.agent.connection.IConnection;
import info.novatec.inspectit.agent.connection.ServerUnavailableException;
import info.novatec.inspectit.agent.core.IIdManager;
import info.novatec.inspectit.agent.core.IdNotAvailableException;
import info.novatec.inspectit.agent.hooking.IHookDispatcherMapper;
import info.novatec.inspectit.exception.BusinessException;
import info.novatec.inspectit.spring.logger.Log;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	 * Class pool analyzer for the classes that have null as byte code.
	 */
	@Autowired
	IClassPoolAnalyzer classPoolAnalyzer;

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
	 * If class loader delegation should be active.
	 */
	@Value("${instrumentation.classLoaderDelegation}")
	boolean classLoaderDelegation; // TODO

	/**
	 * {@link IHookDispatcherMapper}.
	 */
	@Autowired
	IHookDispatcherMapper hookDispatcherMapper;

	/**
	 * {@link SendingClassHashCache} that will serve in deciding if class should be sent to the
	 * sever or not.
	 */
	@Autowired
	SendingClassHashCache sendingClassHashCache;

	/**
	 * {@inheritDoc}
	 */
	public byte[] analyzeAndInstrument(byte[] byteCode, String className, final ClassLoader classLoader) {
		try {
			if (null == byteCode) {
				// TODO Patrice and class pool stuff
				// this occurs if we are in the initialization phase and are instrumenting classes
				// where we don't have the byte code directly. Thus we try to load it.
				ClassPool classPool = classPoolAnalyzer.getClassPool(classLoader);
				byteCode = classPool.get(className).toBytecode();
			}

			// create the hash
			String hash = DigestUtils.sha256Hex(byteCode);

			// in asynch we should move to check for instrumentation points
			if (!sendingClassHashCache.isSending(hash)) {
				return null;
			}

			// try connecting to server
			InstrumentationResult instrumentationResult = connection.analyzeAndInstrument(idManager.getPlatformId(), hash, byteCode);

			if (null == instrumentationResult || CollectionUtils.isEmpty(instrumentationResult.getRegisteredSensorConfigs())) {
				sendingClassHashCache.markSending(hash, false);
				return null;
			}

			// here do the instrumentation
			ClassReader classReader = new ClassReader(byteCode);
			ClassWriter classWriter = new LoaderAwareClassWriter(ClassWriter.COMPUTE_FRAMES, classLoader);
			ClassInstrumenter classInstrumenter = new ClassInstrumenter(classWriter, instrumentationResult.getRegisteredSensorConfigs(), false);
			classReader.accept(classInstrumenter, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

			for (RegisteredSensorConfig registeredSensorConfig : instrumentationResult.getRegisteredSensorConfigs()) {
				hookDispatcherMapper.addMapping(registeredSensorConfig.getId(), registeredSensorConfig);
			}

			sendingClassHashCache.markSending(hash, true);

			return classWriter.toByteArray();
		} catch (IdNotAvailableException idNotAvailableException) {
			log.error("Error occurred instrumenting the byte code of class " + className, idNotAvailableException);
			return null;
		} catch (ServerUnavailableException serverUnavailableException) {
			log.error("Error occurred instrumenting the byte code of class " + className, serverUnavailableException);
			return null;
		} catch (NotFoundException notFoundException) {
			log.error("Error occurred instrumenting the byte code of class " + className, notFoundException);
			return null;
		} catch (IOException iOException) {
			log.error("Error occurred instrumenting the byte code of class " + className, iOException);
			return null;
		} catch (CannotCompileException cannotCompileException) {
			log.error("Error occurred instrumenting the byte code of class " + className, cannotCompileException);
			return null;
		} catch (BusinessException businessException) {
			log.error("Error occurred instrumenting the byte code of class " + className, businessException);
			return null;
		} finally { // NOCHK //NOPMD
			// TODO what with the class pools
			// Remove the byte array class path from the class pool. The class
			// loader now should know this class, thus it can be accessed
			// through the standard way.
			// if (null != classPath) {
			// classPool.removeClassPath(classPath);
			// }
			// if (null != loaderClassPath) {
			// classPool.removeClassPath(loaderClassPath);
			// }
		}
	}

}
