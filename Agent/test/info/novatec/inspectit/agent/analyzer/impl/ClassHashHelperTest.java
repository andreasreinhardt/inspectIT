package info.novatec.inspectit.agent.analyzer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.IConfigurationStorage;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.agent.core.ICoreService;
import info.novatec.inspectit.agent.spring.PrototypesProvider;
import info.novatec.inspectit.storage.serializer.SerializationException;
import info.novatec.inspectit.storage.serializer.impl.SerializationManager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
public class ClassHashHelperTest {

	protected static final String TEST_CACHE_FILE = "test.cache";

	private ClassHashHelper helper;

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
		helper = new ClassHashHelper() {
			@Override
			protected File getCacheFile() {
				return new File(TEST_CACHE_FILE);
			}
		};
		helper.configurationStorage = configurationStorage;
		helper.coreService = coreService;
		helper.prototypesProvider = prototypesProvider;
		helper.log = LoggerFactory.getLogger(ClassHashHelper.class);

		when(prototypesProvider.createSerializer()).thenReturn(serializationManager);
		when(coreService.getExecutorService()).thenReturn(executorService);
	}

	@Test
	public void noCacheFileExists() throws Exception {
		when(configurationStorage.isClassCacheExistsOnCmr()).thenReturn(true);
		helper.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		verifyZeroInteractions(serializationManager);
		assertThat(helper.isEmpty(), is(true));
	}

	@Test
	public void cacheFileExists() throws Exception {
		when(configurationStorage.isClassCacheExistsOnCmr()).thenReturn(true);
		new File(TEST_CACHE_FILE).createNewFile();

		Object hashes = Collections.singleton("hash");

		when(serializationManager.deserialize(Mockito.<Input> any())).thenReturn(hashes);
		helper.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(serializationManager, times(1)).deserialize(Mockito.<Input> any());
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(helper.isEmpty(), is(false));
	}

	@Test
	public void cacheFileExistsException() throws Exception {
		when(configurationStorage.isClassCacheExistsOnCmr()).thenReturn(true);
		new File(TEST_CACHE_FILE).createNewFile();

		when(serializationManager.deserialize(Mockito.<Input> any())).thenThrow(new SerializationException());
		helper.afterPropertiesSet();

		verify(prototypesProvider, times(1)).createSerializer();
		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(helper.isEmpty(), is(true));
	}

	@Test
	public void cacheFileExistsCacheOnCmrNot() throws Exception {
		when(configurationStorage.isClassCacheExistsOnCmr()).thenReturn(false);
		new File(TEST_CACHE_FILE).createNewFile();

		Object hashes = Collections.singleton("hash");

		when(serializationManager.deserialize(Mockito.<Input> any())).thenReturn(hashes);
		helper.afterPropertiesSet();

		verify(executorService, times(1)).scheduleAtFixedRate(Mockito.<Runnable> any(), anyLong(), anyLong(), Mockito.<TimeUnit> any());
		assertThat(helper.isEmpty(), is(true));
		assertThat(new File(TEST_CACHE_FILE).exists(), is(false));
		verifyZeroInteractions(serializationManager);
	}

	@Test
	public void initialInstrumentationPoints() throws Exception {
		String hash = "hash";
		InstrumentationResult instrumentationResult = mock(InstrumentationResult.class);
		Map<Collection<String>, InstrumentationResult> initInstrumentations = Collections.<Collection<String>, InstrumentationResult> singletonMap(Collections.singleton(hash), instrumentationResult);
		when(configurationStorage.getInitialInstrumentationResults()).thenReturn(initInstrumentations);

		helper.afterPropertiesSet();

		assertThat(helper.isEmpty(), is(false));
		assertThat(helper.isSent(hash), is(true));
		assertThat(helper.getInstrumentationResult(hash), is(instrumentationResult));
	}

	@Test
	public void noInitialInstrumentationPoints() throws Exception {
		when(configurationStorage.getInitialInstrumentationResults()).thenReturn(Collections.<Collection<String>, InstrumentationResult> emptyMap());

		helper.afterPropertiesSet();

		assertThat(helper.isEmpty(), is(true));
	}

	@Test
	public void registerSent() throws Exception {
		helper.afterPropertiesSet();
		InstrumentationResult ir1 = mock(InstrumentationResult.class);
		InstrumentationResult ir2 = mock(InstrumentationResult.class);

		String hash = "hash";
		assertThat(helper.isSent(hash), is(false));

		helper.registerSent(hash, ir1);
		assertThat(helper.isSent(hash), is(true));
		assertThat(helper.getInstrumentationResult(hash), is(ir1));

		helper.registerSent(hash, ir2);
		assertThat(helper.isSent(hash), is(true));
		assertThat(helper.getInstrumentationResult(hash), is(ir2));
	}

	@Test
	public void registerLoader() throws Exception {
		helper.afterPropertiesSet();

		ClassLoader cl1 = mock(ClassLoader.class);
		ClassLoader cl2 = mock(ClassLoader.class);

		String hash = "hash";
		assertThat(helper.getClassLoaders(hash), is(empty()));

		helper.registerLoaded(hash, cl1);
		assertThat(helper.getClassLoaders(hash), hasSize(1));
		assertThat(helper.getClassLoaders(hash), hasItem(cl1));

		helper.registerLoaded(hash, cl2);
		assertThat(helper.getClassLoaders(hash), hasSize(2));
		assertThat(helper.getClassLoaders(hash), hasItem(cl1));
		assertThat(helper.getClassLoaders(hash), hasItem(cl2));

	}

	@AfterMethod
	public void delete() {
		File file = new File(TEST_CACHE_FILE);
		if (file.exists()) {
			assertThat(file.delete(), is(true));
		}
	}
}
