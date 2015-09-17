package info.novatec.inspectit.agent.asm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Test classes below.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnnotation {

	String value() default "";
}