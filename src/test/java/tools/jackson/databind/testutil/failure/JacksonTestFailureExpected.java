package tools.jackson.databind.testutil.failure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * <p>
 * Annotation used to indicate that a JUnit-5 based tests method is expected to fail.
 *
 * <p>
 * When a test method is annotated with {@code @JacksonTestFailureExpected}, the
 * {@link JacksonTestFailureExpectedInterceptor} will intercept the test execution.
 * If the test passes, which is an unexpected behavior, the interceptor will throw an exception to fail the test,
 * indicating that the test was expected to fail but didn't.
 * </p>
 *
 * <h3>Usage Example:</h3>
 *
 * <pre><code>
 *
 *     &#64;Test
 *     &#64;JacksonTestFailureExpected
 *     public void testFeatureNotYetImplemented() {
 *         // Test code that is expected to fail
 *     }
 * }
 * </code></pre>
 *
 * <p>
 *
 * @since 2.19
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JacksonTestFailureExpectedInterceptor.class)
public @interface JacksonTestFailureExpected { }
