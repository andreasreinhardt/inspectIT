package info.novatec.inspectit.cmr.classcache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import info.novatec.inspectit.classcache.ImmutableAnnotationType;
import info.novatec.inspectit.classcache.ImmutableClassType;
import info.novatec.inspectit.classcache.ImmutableInterfaceType;
import info.novatec.inspectit.classcache.Type;
import info.novatec.inspectit.cmr.classcache.index.FQNIndexer;
import info.novatec.inspectit.cmr.classcache.index.HashIndexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for the {@link ClassCacheLookup}.
 * 
 * @author Ivan Senic
 * 
 */
@SuppressWarnings("PMD")
public class ClassCacheLookupTest {

	private ClassCacheLookup lookup;

	@Mock
	private ClassCache classCache;

	@Mock
	private FQNIndexer<Type> fqnIndexer;

	@Mock
	private HashIndexer hashIndexer;

	@Mock
	private Type type;

	@Mock
	private ImmutableClassType classType;

	@Mock
	private ImmutableInterfaceType interfaceType;

	@Mock
	private ImmutableAnnotationType annotationType;

	@BeforeMethod
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);

		lookup = new ClassCacheLookup();
		lookup.log = LoggerFactory.getLogger(ClassCacheLookup.class);
		lookup.fqnIndexer = fqnIndexer;
		lookup.hashIndexer = hashIndexer;

		Answer<Object> callableAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Callable<?> callable = (Callable<?>) invocation.getArguments()[0];
				return callable.call();
			}
		};
		doAnswer(callableAnswer).when(classCache).executeWithReadLock(Mockito.<Callable<?>> anyObject());
		doAnswer(callableAnswer).when(classCache).executeWithWriteLock(Mockito.<Callable<?>> anyObject());

		lookup.init(classCache);
		verify(classCache, times(1)).registerNodeChangeListener(fqnIndexer);
		verify(classCache, times(1)).registerNodeChangeListener(hashIndexer);
	}

	@Test
	public void fqnLookup() throws Exception {
		String fqn = "fqn";
		when(fqnIndexer.lookup(fqn)).thenReturn(type);

		assertThat((Type) lookup.findByFQN(fqn), is(type));

		verify(fqnIndexer, times(1)).lookup(fqn);
		verify(classCache, times(1)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void hashLookup() throws Exception {
		String hash = "hash";
		when(hashIndexer.lookup(hash)).thenReturn(type);

		assertThat((Type) lookup.findByHash(hash), is(type));

		verify(hashIndexer, times(1)).lookup(hash);
		verify(classCache, times(1)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(hashIndexer, classCache);
		verifyZeroInteractions(fqnIndexer);
	}

	@Test
	public void findByPatternDirect() throws Exception {
		String pattern = "pattern";
		when(fqnIndexer.lookup(pattern)).thenReturn(type);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findByPattern(pattern, false), hasSize(1));

		assertThat(lookup.findByPattern("somethingElse", false), hasSize(0));

		verify(fqnIndexer, times(4)).lookup(pattern);
		verify(fqnIndexer, times(1)).lookup("somethingElse");
		verify(classCache, times(5)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.isInitialized()).thenReturn(true);
		when(type.getFQN()).thenReturn("pattttern");

		assertThat(lookup.findByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findByPattern(pattern, false), hasSize(1));
		assertThat(lookup.findByPattern(pattern, true), hasSize(0));

		verify(fqnIndexer, times(4)).findStartsWith("pat");
		verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findByPatternWildCardNotMatched() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.isInitialized()).thenReturn(true);
		when(type.getFQN()).thenReturn("patsomething");

		assertThat(lookup.findByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findByPattern(pattern, false), hasSize(0));

		verify(fqnIndexer, times(2)).findStartsWith("pat");
		verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findClassTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isClass()).thenReturn(true);
		when(type.castToClass()).thenReturn(classType);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findClassTypesByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findClassTypesByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findClassTypesByPattern(pattern, false), hasSize(1));
		assertThat(lookup.findClassTypesByPattern(pattern, true), hasSize(0));

		verify(fqnIndexer, times(4)).findStartsWith("pat");
		verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findNoClassTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isClass()).thenReturn(false);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findClassTypesByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findClassTypesByPattern(pattern, false), hasSize(0));

		verify(fqnIndexer, times(2)).findStartsWith("pat");
		verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findInterfaceTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isInterface()).thenReturn(true);
		when(type.castToInterface()).thenReturn(interfaceType);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findInterfaceTypesByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findInterfaceTypesByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findInterfaceTypesByPattern(pattern, false), hasSize(1));
		assertThat(lookup.findInterfaceTypesByPattern(pattern, true), hasSize(0));

		verify(fqnIndexer, times(4)).findStartsWith("pat");
		verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findNoInterfaceTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isInterface()).thenReturn(false);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findInterfaceTypesByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findInterfaceTypesByPattern(pattern, false), hasSize(0));

		verify(fqnIndexer, times(2)).findStartsWith("pat");
		verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findAnnotationTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isAnnotation()).thenReturn(true);
		when(type.castToAnnotation()).thenReturn(annotationType);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findAnnotationTypesByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findAnnotationTypesByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findAnnotationTypesByPattern(pattern, false), hasSize(1));
		assertThat(lookup.findAnnotationTypesByPattern(pattern, true), hasSize(0));

		verify(fqnIndexer, times(4)).findStartsWith("pat");
		verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findNoAnnotationTypeByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isAnnotation()).thenReturn(false);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findAnnotationTypesByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findAnnotationTypesByPattern(pattern, false), hasSize(0));

		verify(fqnIndexer, times(2)).findStartsWith("pat");
		verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findExceptionByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isClass()).thenReturn(true);
		when(type.castToClass()).thenReturn(classType);
		when(classType.isException()).thenReturn(true);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findExceptionTypesByPattern(pattern, true), hasSize(1));
		assertThat(lookup.findExceptionTypesByPattern(pattern, false), hasSize(1));

		when(type.isInitialized()).thenReturn(false);

		assertThat(lookup.findExceptionTypesByPattern(pattern, false), hasSize(1));
		assertThat(lookup.findExceptionTypesByPattern(pattern, true), hasSize(0));

		verify(fqnIndexer, times(4)).findStartsWith("pat");
		verify(classCache, times(4)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

	@Test
	public void findNoExceptionByPatternWildCard() throws Exception {
		String pattern = "pat*tern";
		Collection<Type> result = new ArrayList<>(1);
		result.add(type);

		when(fqnIndexer.findStartsWith("pat")).thenReturn(result);
		when(type.getFQN()).thenReturn("pattttern");
		when(type.isClass()).thenReturn(true);
		when(type.castToClass()).thenReturn(classType);
		when(classType.isException()).thenReturn(false);
		when(type.isInitialized()).thenReturn(true);

		assertThat(lookup.findExceptionTypesByPattern(pattern, true), hasSize(0));
		assertThat(lookup.findExceptionTypesByPattern(pattern, false), hasSize(0));

		verify(fqnIndexer, times(2)).findStartsWith("pat");
		verify(classCache, times(2)).executeWithReadLock(Mockito.<Callable<?>> anyObject());

		verifyNoMoreInteractions(fqnIndexer, classCache);
		verifyZeroInteractions(hashIndexer);
	}

}
