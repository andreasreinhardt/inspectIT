package info.novatec.inspectit.cmr.classcache.asm;

import info.novatec.inspectit.cmr.asm.ClassAnalyzer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.ClassReader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Performance test for the {@link ClassAnalyzer} class using JMH framework.
 * 
 * @author Ivan Senic
 * 
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(value = 1)
@State(Scope.Thread)
public class ClassAnalyzerPerfTest {

	@Param({ "info.novatec.inspectit.cmr.storage.CmrStorageManager", "java.lang.String", "java.lang.Object", "java.lang.Comparable" })
	private String clazz;

	@Param({ "true", "false" })
	private boolean internFQNs;

	/**
	 * Parsing class without intern option set.
	 */
	@Benchmark
	public void parse() throws InterruptedException, IOException {
		ClassReader classReader = new ClassReader(clazz);
		classReader.accept(new ClassAnalyzer("MyHash", null, internFQNs), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}
}
