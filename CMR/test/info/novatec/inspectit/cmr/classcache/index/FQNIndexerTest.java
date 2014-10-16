package info.novatec.inspectit.cmr.classcache.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import info.novatec.inspectit.classcache.ClassType;

import java.util.Collection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("PMD")
public class FQNIndexerTest {

	private FQNIndexer<ClassType> indexer;

	@BeforeMethod
	public void init() {
		indexer = new FQNIndexer<>();
	}

	@Test
	public void indexSameFQNTwice() {
		ClassType stringType1 = new ClassType(String.class.getName());
		ClassType stringType2 = new ClassType(String.class.getName());

		indexer.index(stringType1);
		indexer.index(stringType2);

		assertThat(indexer, hasSize(1));
		assertThat(indexer.lookup(String.class.getName()) == stringType2, is(true));
	}

	@Test
	public void lookup() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName()); 
		ClassType thisType = new ClassType(FQNIndexer.class.getName());
		
		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(new ClassType("a"));
		indexer.index(new ClassType("java.nolang"));
		indexer.index(objectType);

		assertThat(indexer, hasSize(5));
		assertThat(indexer.lookup(stringType.getFQN()), is(stringType));
		assertThat(indexer.lookup(objectType.getFQN()), is(objectType));
		assertThat(indexer.lookup(thisType.getFQN()), is(thisType));
	}

	@Test
	public void notFound() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName());
		ClassType thisType = new ClassType(FQNIndexer.class.getName());

		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(objectType);

		assertThat(indexer, hasSize(3));
		assertThat(indexer.lookup("nonExistingType"), is(nullValue()));
	}

	@Test
	public void replace() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName());
		ClassType thisType = new ClassType(FQNIndexer.class.getName());
		ClassType secondObjectType = new ClassType(Object.class.getName());

		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(objectType);
		indexer.index(secondObjectType);

		assertThat(indexer, hasSize(3));
		assertThat(System.identityHashCode(indexer.lookup(Object.class.getName())), is(System.identityHashCode(secondObjectType)));
	}

	@Test
	public void findAll() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName());
		ClassType thisType = new ClassType(FQNIndexer.class.getName());
		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(objectType);

		Collection<ClassType> types = indexer.findAll();
		assertThat(types, hasSize(3));
		assertThat(types, hasItem(stringType));
		assertThat(types, hasItem(objectType));
		assertThat(types, hasItem(thisType));
	}

	@Test
	public void fqnWildcardFound() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName());
		ClassType thisType = new ClassType(FQNIndexer.class.getName());

		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(new ClassType("a"));
		indexer.index(new ClassType("java.nolang"));
		indexer.index(objectType);

		// middle
		Collection<ClassType> results = indexer.findStartsWith("java.lang");
		assertThat(results, hasSize(2));
		for (ClassType classType : results) {
			assertThat(classType.getFQN().startsWith("java.lang"), is(true));
		}

		// end
		results = indexer.findStartsWith("java.nolang");
		assertThat(results, hasSize(1));
		for (ClassType classType : results) {
			assertThat(classType.getFQN().startsWith("java.nolang"), is(true));
		}

		// begin
		results = indexer.findStartsWith("a");
		assertThat(results, hasSize(1));
		for (ClassType classType : results) {
			assertThat(classType.getFQN().startsWith("a"), is(true));
		}
	}

	@Test
	public void fqnWildcardNotFound() {
		ClassType stringType = new ClassType(String.class.getName());
		ClassType objectType = new ClassType(Object.class.getName());
		ClassType thisType = new ClassType(FQNIndexer.class.getName());

		indexer.index(stringType);
		indexer.index(thisType);
		indexer.index(new ClassType("a"));
		indexer.index(new ClassType("java.nolang"));
		indexer.index(objectType);

		// middle
		Collection<ClassType> results = indexer.findStartsWith("aa");
		assertThat(results, is(empty()));

		results = indexer.findStartsWith("java.lang.something");
		assertThat(results, is(empty()));

		results = indexer.findStartsWith("ww");
		assertThat(results, is(empty()));
	}

}
