package com.fasterxml.jackson.databind.testutil;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(Failing.FailingExtension.class) // Comment this out if you want to temporarily let the tests actually fail
public @interface Failing {

    /**
     * For when a test is failing only when running on specific Java version - e.g. Java 8 & Java 11 - but passing
     * for other versions, then:
     * <pre><code>
     * {@code @}Failing(javaVersion = { "1.8", "11" }
     * {@code @}Test
     *  public void test...() { ... }
     * </code></pre>
     */
    String[] javaVersion() default {};

    class FailingExtension implements BeforeEachCallback, TestExecutionExceptionHandler, AfterEachCallback {

        private static final String SHOULD_FAIL = "should fail";

        @Override
        public void beforeEach(ExtensionContext context) {
            getStore(context).put(SHOULD_FAIL, matchesFailingJavaVersion(context));
        }

        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            boolean shouldFail = getStore(context).get(SHOULD_FAIL, boolean.class);
            if (!shouldFail) {
                // the test threw exception even though it's not expected to fail for this test method, let the
                // it fail with this exception so maintainers can investigate
                throw throwable;
            }

            // Instead of swallowing the exception and silently passing, better to mark the test as "aborted"
            throw new TestAbortedException("Not implemented/fixed yet", throwable);
        }

        @Override
        public void afterEach(ExtensionContext context) {
            boolean shouldFail = getStore(context).get(SHOULD_FAIL, boolean.class);
            boolean testFailed = context.getExecutionException().isPresent();

            if (shouldFail && !testFailed) {
                throw new RuntimeException("Test that has been failing is now passing!");
            }
        }

        private ExtensionContext.Store getStore(ExtensionContext context) {
            return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
        }

        private boolean matchesFailingJavaVersion(ExtensionContext context) {
            Method method = context.getTestMethod()
                    // should not happen because @Failing can only be annotated on test methods!
                    .orElseThrow(() -> new IllegalArgumentException("Cannot get test method!"));

            String currentJavaVersion = System.getProperty("java.version");
            String[] failingJavaVersions = method.getAnnotation(Failing.class).javaVersion();

            if (failingJavaVersions.length == 0) { // failing for all Java versions
                return true;
            }
            for (String version : failingJavaVersions) {
                if (currentJavaVersion.startsWith(version)) {
                    return true;
                }
            }
            return false; // not expected to fail for the current Java version
        }
    }
}
