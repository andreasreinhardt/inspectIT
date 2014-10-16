package info.novatec.inspectit.agent.analyzer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.core.ICoreService;
import info.novatec.inspectit.agent.spring.PrototypesProvider;
import info.novatec.inspectit.storage.serializer.SerializationException;
import info.novatec.inspectit.storage.serializer.impl.SerializationManager;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.esotericsoftware.kryo.io.Input;

@SuppressWarnings("PMD")
public class SendingClassHashCacheTest {

	protected static final String TEST_CACHE_FILE = "test.cache";

	private SendingClassHashCache cache;

	@Mock
	private IConfigurationStorage configurationStorage;

	@Mock
	private ICoreService coreService;

	@Mock
	private PrototypesProvider prototypesProvider;
	
	@Mock
	private SerializationManager serializationManager;

	@Mock
	ScheduledExecutorService executorService;

	@BeforeMethod
	public void init() {
		MockitoAnnotations.initMocks(this);
		cache = new SendingClassHashCache() {
			@Override
			protected File getCacheFile() {
				return new File(TEST_CACHE_FILE);
			}
		};
		cache.configurationStorage = configurationStorage;
		cache.coreService = coreService;
		cache.prototypesProvider = prototypesProvider;
		cache.log = LoggerFactory.getLogger(SendingClassHashCache.class);

		when(prototypesProvider.createSerializer()).thenReturn(serializationManager);
		when(coreService.getExecutorService()).thenReturn(executorService);
	}
	
	@Test
	public void noCacheFileExists() throws Exception {
		long validTime = 1L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);

		cache.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		verifyZeroInteractions(serializationManager);
		assertThat(cache.isEmpty(), is(true));
		assertThat(cache.getValidTimestamp(), is(validTime));
	}

	@Test
	public void cacheFileExistsValid() throws Exception {
		new File(TEST_CACHE_FILE).createNewFile();
		
		Object validTimeStamp = Long.valueOf(1L);
		Object map = Collections.singletonMap("hash", Boolean.TRUE);

		long validTime = 1L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);
		when(serializationManager.deserialize(Mockito.<Input> any())).thenReturn(validTimeStamp).thenReturn(map);
		cache.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(serializationManager, times(2)).deserialize(Mockito.<Input> any());
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(cache.isEmpty(), is(false));
		assertThat(cache.getValidTimestamp(), is(validTime));
	}

	@Test
	public void cacheFileExistsNotValid() throws Exception {
		new File(TEST_CACHE_FILE).createNewFile();

		Object validTimeStamp = Long.valueOf(1L);
		Object map = Collections.singletonMap("hash", Boolean.TRUE);

		long validTime = 2L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);
		when(serializationManager.deserialize(Mockito.<Input> any())).thenReturn(validTimeStamp).thenReturn(map);
		cache.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(serializationManager, times(2)).deserialize(Mockito.<Input> any());
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(cache.isEmpty(), is(true));
		assertThat(cache.getValidTimestamp(), is(validTime));
	}

	@Test
	public void cacheFileExistsException() throws Exception {
		new File(TEST_CACHE_FILE).createNewFile();

		long validTime = 2L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);
		when(serializationManager.deserialize(Mockito.<Input> any())).thenThrow(new SerializationException());
		cache.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(cache.isEmpty(), is(true));
		assertThat(cache.getValidTimestamp(), is(validTime));
	}

	@Test
	public void validate() throws Exception {
		long validTime = 1L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);

		cache.afterPropertiesSet();
		cache.markSending("hash1", false);
		cache.markSending("hash2", true);
		assertThat(cache.getValidTimestamp(), is(validTime));

		validTime++;
		cache.validate(validTime);
		assertThat(cache.getValidTimestamp(), is(validTime));
		assertThat(cache.isSending("hash1"), is(false));
		assertThat(cache.isSending("hash2"), is(true));

		// validate with smaller time
		cache.validate(validTime - 1);
		assertThat(cache.getValidTimestamp(), is(validTime));
		assertThat(cache.isSending("hash1"), is(false));
		assertThat(cache.isSending("hash2"), is(true));
	}

	@Test
	public void invalidate() throws Exception {
		long validTime = 1L;
		when(configurationStorage.getInstrumentationLastModified()).thenReturn(validTime);

		cache.afterPropertiesSet();
		cache.markSending("hash", false);
		cache.markSending("hash2", true);
		assertThat(cache.getValidTimestamp(), is(validTime));

		validTime++;
		cache.invalidate(validTime);
		assertThat(cache.getValidTimestamp(), is(validTime));
		assertThat(cache.isSending("hash1"), is(true));
		assertThat(cache.isSending("hash2"), is(true));
	}

	@Test
	public void marking() throws Exception {
		cache.afterPropertiesSet();

		String hash = "hash";
		cache.markSending(hash, true);
		assertThat(cache.isSending(hash), is(true));

		cache.markSending(hash, false);
		assertThat(cache.isSending(hash), is(false));
	}

	@AfterMethod
	public void delete() {
		File file = new File(TEST_CACHE_FILE);
		if (file.exists()) {
			assertThat(file.delete(), is(true));
		}
	}
}
