package info.novatec.inspectit.agent.analyzer.impl;

import info.novatec.inspectit.agent.SpringAgent;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.core.ICoreService;
import info.novatec.inspectit.agent.spring.PrototypesProvider;
import info.novatec.inspectit.spring.logger.Log;
import info.novatec.inspectit.storage.serializer.impl.SerializationManager;
import info.novatec.inspectit.storage.serializer.provider.SerializationManagerProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Cache for holding the classes to be send to the CMR. This cache works in a way that it remembers
 * the classes that should not be sent to the CMR.
 * 
 * @author Ivan Senic
 */
@Component
public class SendingClassHashCache implements InitializingBean, DisposableBean {

	/**
	 * Logger for the class.
	 */
	@Log
	Logger log;

	/**
	 * Configuration storage to read the agent name.
	 */
	@Autowired
	IConfigurationStorage configurationStorage;

	/**
	 * Core service.
	 */
	@Autowired
	ICoreService coreService;

	/**
	 * {@link SerializationManagerProvider} for the serialization.
	 */
	@Autowired
	PrototypesProvider prototypesProvider;

	/**
	 * Serialization manager to use when storing loading from disk.
	 */
	private SerializationManager serializationManager;

	/**
	 * Valid time-stamp of the cache.
	 */
	private volatile long validTimestamp;

	/**
	 * Set for storing the hashes not to send. Created via {@link #createCache(Map)}.
	 */
	// TODO maybe some other structure is better, too much space to use Concurrent hash map
	private Cache<String, Boolean> cache;

	/**
	 * Dirty flag.
	 */
	private volatile boolean dirty;

	/**
	 * Marks class to be sent to the CMR or not.
	 * 
	 * @param hash
	 *            The class hash.
	 * @param send
	 *            If should be sent or not.
	 */
	public void markSending(String hash, boolean send) {
		if (null == hash) {
			throw new IllegalArgumentException("Class hash to be added can not be null.");
		}

		dirty = true;
		if (send) {
			cache.invalidate(hash);
		} else {
			cache.put(hash, Boolean.TRUE);
		}
	}

	/**
	 * Returns if the class should be sent to the CMR by the byte code analyzer.
	 * 
	 * @param hash
	 *            The class hash.
	 * @return If it should be sent to the CMR.
	 */
	public boolean isSending(String hash) {
		return null == cache.getIfPresent(hash);
	}

	/**
	 * Invalidates the current if the given time-stamp is greater the the currently hold in the
	 * cache.
	 * 
	 * @param timestamp
	 *            new/lastest time-stamp
	 */
	public void invalidate(long timestamp) {
		if (timestamp > validTimestamp) {
			validTimestamp = timestamp;
			cache.invalidateAll();
		}
	}

	/**
	 * Updates the time-stamp of the cache without invalidating it. This can be used when the cache
	 * should be marked as valid for given time-stamp.
	 * 
	 * @param timestamp
	 *            valid time-stamp for this cache
	 */
	public void validate(long timestamp) {
		if (timestamp > validTimestamp) {
			validTimestamp = timestamp;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void afterPropertiesSet() throws Exception {
		serializationManager = prototypesProvider.createSerializer();

		loadCacheFromDisk();

		// invalidate if needed
		// TODO invalidate only in the sync mode, in async mode we always have a proper cache
		invalidate(configurationStorage.getInstrumentationLastModified());

		Runnable saveCacheToDiskRunnable = new Runnable() {
			public void run() {
				saveCacheToDisk();
			}
		};
		coreService.getExecutorService().scheduleAtFixedRate(saveCacheToDiskRunnable, 30, 300, TimeUnit.SECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() throws Exception {
		// save when bean is destroyed, ensure save is always done on finishing
		saveCacheToDisk();
	}

	/**
	 * Is cache empty. Used in testing.
	 * 
	 * @return Is cache empty.
	 */
	boolean isEmpty() {
		return cache.asMap().isEmpty();
	}

	/**
	 * Load cache from disk.
	 */
	@SuppressWarnings("unchecked")
	private void loadCacheFromDisk() {
		File file = getCacheFile();

		if (file.exists()) {
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(file);
				Input input = new Input(fileInputStream);

				// first valid time-stamp
				validTimestamp = (Long) serializationManager.deserialize(input);

				// then cache, but deserialize map
				Map<String, Boolean> map = (Map<String, Boolean>) serializationManager.deserialize(input);
				createCache(map);
			} catch (Throwable t) { // NOPMD
				log.warn("Unable to load sending classes cache from disk.", t);
				createCache(Collections.<String, Boolean> emptyMap());
			} finally {
				if (null != fileInputStream) {
					try {
						fileInputStream.close();
					} catch (IOException e) { // NOPMD //NOCHK
						// ignore
					}
				}
			}
		} else {
			createCache(Collections.<String, Boolean> emptyMap());
		}
	}

	/**
	 * Save cache to disk.
	 */
	private void saveCacheToDisk() {
		if (!dirty) {
			return;
		}

		File file = getCacheFile();

		if (file.exists()) {
			if (!file.delete()) {
				log.warn("Unable to delete the existing class cache file: " + file.getAbsolutePath());
			}
		} else {
			if (!file.getParentFile().mkdirs()) {
				log.warn("Unable to create needed directory for the cache file: " + file.getParentFile().getAbsolutePath());
			}
		}

		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(file);
			Output output = new Output(fileOutputStream);

			// first valid time-stamp
			serializationManager.serialize(Long.valueOf(validTimestamp), output);

			// then cache serialize as map
			Map<String, Boolean> map = new HashMap<String, Boolean>(cache.asMap());
			serializationManager.serialize(map, output);

			dirty = false;
		} catch (Throwable t) { // NOPMD
			log.warn("Unable to save sending classes cache to disk.", t);
		} finally {
			if (null != fileOutputStream) {
				try {
					fileOutputStream.close();
				} catch (IOException e) { // NOPMD //NOCHK
					// ignore
				}
			}
		}
	}

	/**
	 * Builds new Guava cache and fills it with elements from given map.
	 * 
	 * @param initMap
	 *            map to fill cache
	 */
	void createCache(Map<String, Boolean> initMap) {
		cache = CacheBuilder.newBuilder().build();
		if (MapUtils.isNotEmpty(initMap)) {
			cache.putAll(initMap);
		}
	}

	/**
	 * @return Returns file where cache for this agent should be.
	 */
	protected File getCacheFile() {
		File agentJar = new File(SpringAgent.getInspectitJarLocation()).getAbsoluteFile();
		return new File(agentJar.getParent() + File.separator + "cache" + File.separator + configurationStorage.getAgentName() + File.separator + "sendingClasses.cache");
	}

	/**
	 * Gets {@link #validTimestamp}.
	 * 
	 * @return {@link #validTimestamp}
	 */
	long getValidTimestamp() {
		return validTimestamp;
	}

}
