package info.novatec.inspectit.cmr.classcache.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.agent.config.impl.AgentConfiguration;
import info.novatec.inspectit.agent.config.impl.InstrumentationResult;
import info.novatec.inspectit.ci.Environment;
import info.novatec.inspectit.ci.assignment.impl.ExceptionSensorAssignment;
import info.novatec.inspectit.ci.assignment.impl.MethodSensorAssignment;
import info.novatec.inspectit.classcache.AnnotationType;
import info.novatec.inspectit.classcache.ClassType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.InterfaceType;
import info.novatec.inspectit.classcache.Type;
import info.novatec.inspectit.cmr.classcache.ClassCache;
import info.novatec.inspectit.cmr.classcache.ClassCacheLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("all")
public class InstrumentationPointsUtilsTest {

	@InjectMocks
	protected InstrumentationPointsUtil instrumentationPointsUtil;

	@Mock
	protected Logger log;

	@Mock
	protected InstrumentationCreator instrumentationCreator;

	@Mock
	protected ConfigurationResolver configurationResolver;

	@Mock
	protected ClassCache classCache;

	@Mock
	protected ClassCacheLookup lookup;

	@Mock
	protected Environment environment;

	@Mock
	protected AgentConfiguration agentConfiguration;

	@Mock
	protected ClassType classType;

	@Mock
	protected Collection<MethodSensorAssignment> methodSensorAssignments;

	@Mock
	protected Collection<ExceptionSensorAssignment> exceptionSensorAssignments;

	@BeforeMethod
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(classCache.getLookupService()).thenReturn(lookup);

		Answer<Object> callableAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Callable<?> callable = (Callable<?>) invocation.getArguments()[0];
				return callable.call();
			}
		};
		doAnswer(callableAnswer).when(classCache).executeWithReadLock(Mockito.<Callable<?>> anyObject());
		doAnswer(callableAnswer).when(classCache).executeWithWriteLock(Mockito.<Callable<?>> anyObject());

		when(classType.isClass()).thenReturn(true);
		when(classType.castToClass()).thenReturn(classType);

		when(configurationResolver.getAllMethodSensorAssignments(environment)).thenReturn(methodSensorAssignments);
		when(configurationResolver.getAllExceptionSensorAssignments(environment)).thenReturn(exceptionSensorAssignments);
	}

	public static class RemoveInstrumentationPoints extends InstrumentationPointsUtilsTest {

		@Test
		public void removeAll() throws Exception {
			when(classType.isInitialized()).thenReturn(true);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();

			instrumentationPointsUtil.removeAllInstrumentationPoints(classCache);

			// must be write lock
			verify(classCache, times(1)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(1)).removeInstrumentationPoints(classType);
			verifyZeroInteractions(log);
		}

		@Test
		public void removeNothingWhenEmpty() throws Exception {
			doReturn(Collections.emptyList()).when(lookup).findAll();
			instrumentationPointsUtil.removeAllInstrumentationPoints(classCache);

			// not touching the write lock
			verify(classCache, times(0)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

		@Test
		public void removeNothingForNonClassTypes() throws Exception {
			doReturn(Collections.singleton(new AnnotationType(""))).when(lookup).findAll();
			instrumentationPointsUtil.removeAllInstrumentationPoints(classCache);

			doReturn(Collections.singleton(new InterfaceType(""))).when(lookup).findAll();
			instrumentationPointsUtil.removeAllInstrumentationPoints(classCache);

			// must be write lock
			verify(classCache, times(2)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

	}

	public static class AddInstrumentationPoints extends InstrumentationPointsUtilsTest {

		@Test
		public void add() throws Exception {
			when(classType.isInitialized()).thenReturn(true);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			doReturn(true).when(instrumentationCreator).addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments);
			Collection<? extends ImmutableClassType> result = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);

			// assert result
			assertThat((Collection<ClassType>) result, hasItem(classType));

			// must be write lock
			verify(classCache, times(1)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(1)).addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments);
			verifyZeroInteractions(log);
		}

		@Test
		public void addNothingWhenInstrumenterDoesNotAdd() throws Exception {
			when(classType.isInitialized()).thenReturn(true);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			doReturn(false).when(instrumentationCreator).addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments);
			Collection<? extends ImmutableClassType> result = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);

			// assert result
			assertThat((Collection<ClassType>) result, is(empty()));

			// must be write lock
			verify(classCache, times(1)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(1)).addInstrumentationPoints(agentConfiguration, environment, classType, methodSensorAssignments, exceptionSensorAssignments);
			verifyZeroInteractions(log);
		}

		@Test
		public void addNothingForNonInitializedType() throws Exception {
			when(classType.isInitialized()).thenReturn(false);
			List<Type> types = new ArrayList<Type>();
			types.add(classType);
			doReturn(types).when(lookup).findAll();
			Collection<? extends ImmutableClassType> result = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);

			// assert result
			assertThat(result, is(empty()));

			// must be write lock
			verify(classCache, times(1)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);

		}

		@Test
		public void addNothingWhenEmpty() throws Exception {
			doReturn(Collections.emptyList()).when(lookup).findAll();
			Collection<? extends ImmutableClassType> result = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);

			// assert result
			assertThat(result, is(empty()));

			// not touching the write lock
			verify(classCache, times(0)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

		@Test
		public void addNothingForNonClassTypes() throws Exception {
			AnnotationType annotationType = new AnnotationType("");
			InterfaceType interfaceType = new InterfaceType("");
			List<Type> types = new ArrayList<Type>();
			types.add(annotationType);
			types.add(interfaceType);

			doReturn(types).when(lookup).findAll();
			Collection<? extends ImmutableClassType> result = instrumentationPointsUtil.addAllInstrumentationPoints(classCache, agentConfiguration, environment);

			// assert result
			assertThat(result, is(empty()));

			// must be write lock
			verify(classCache, times(1)).executeWithWriteLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(log, instrumentationCreator);
		}
	}

	public static class CollectInstrumentationPoints extends InstrumentationPointsUtilsTest {

		@Test
		public void collect() throws Exception {
			InstrumentationResult instrumentationResult = mock(InstrumentationResult.class);
			when(classType.isInitialized()).thenReturn(true);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			doReturn(instrumentationResult).when(instrumentationCreator).createInstrumentationResult(classType, environment);
			Collection<InstrumentationResult> result = instrumentationPointsUtil.collectInstrumentationResults(classCache, environment);

			// assert result
			assertThat(result, hasItem(instrumentationResult));

			// read lock is enough
			verify(classCache, times(1)).executeWithReadLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(1)).createInstrumentationResult(classType, environment);
			verifyZeroInteractions(log);
		}

		@Test
		public void collectWithHashes() throws Exception {
			InstrumentationResult instrumentationResult = mock(InstrumentationResult.class);
			Set<String> hashes = mock(Set.class);
			when(classType.isInitialized()).thenReturn(true);
			when(classType.getHashes()).thenReturn(hashes);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			doReturn(instrumentationResult).when(instrumentationCreator).createInstrumentationResult(classType, environment);
			Map<Collection<String>, InstrumentationResult> result = instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment);

			// assert result
			assertThat(result, hasEntry((Collection<String>) hashes, instrumentationResult));

			// read lock is enough
			verify(classCache, times(1)).executeWithReadLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(1)).createInstrumentationResult(classType, environment);
			verifyZeroInteractions(log);
		}

		@Test
		public void collectNothingWhenInstrumenterDoesNotCreate() throws Exception {
			when(classType.isInitialized()).thenReturn(true);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			doReturn(null).when(instrumentationCreator).createInstrumentationResult(classType, environment);

			assertThat(instrumentationPointsUtil.collectInstrumentationResults(classCache, environment), is(empty()));
			assertThat(instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment).entrySet(), is(empty()));

			// read lock is enough
			verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> any());
			verify(instrumentationCreator, times(2)).createInstrumentationResult(classType, environment);
			verifyZeroInteractions(log);
		}

		@Test
		public void collectNothingForNonInitializedType() throws Exception {
			when(classType.isInitialized()).thenReturn(false);
			doReturn(Collections.singleton(classType)).when(lookup).findAll();
			assertThat(instrumentationPointsUtil.collectInstrumentationResults(classCache, environment), is(empty()));
			assertThat(instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment).entrySet(), is(empty()));

			// must be write lock
			verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

		@Test
		public void collectNothingWhenEmpty() throws Exception {
			doReturn(Collections.emptyList()).when(lookup).findAll();
			assertThat(instrumentationPointsUtil.collectInstrumentationResults(classCache, environment), is(empty()));
			assertThat(instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment).entrySet(), is(empty()));

			// not touching the write lock
			verify(classCache, times(0)).executeWithReadLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

		@Test
		public void collectNothingForNonClassTypes() throws Exception {
			doReturn(Collections.singleton(new AnnotationType(""))).when(lookup).findAll();
			assertThat(instrumentationPointsUtil.collectInstrumentationResults(classCache, environment), is(empty()));
			assertThat(instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment).entrySet(), is(empty()));

			doReturn(Collections.singleton(new InterfaceType(""))).when(lookup).findAll();
			assertThat(instrumentationPointsUtil.collectInstrumentationResults(classCache, environment), is(empty()));
			assertThat(instrumentationPointsUtil.collectInstrumentationResultsWithHashes(classCache, environment).entrySet(), is(empty()));

			// must be write lock
			verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> any());
			verifyZeroInteractions(instrumentationCreator, log);
		}

	}
}
