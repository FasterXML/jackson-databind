package tools.jackson.databind.testutil.failure;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Custom {@link InvocationInterceptor} that intercepts test method invocation.
 * To pass the test ***only if*** test fails with an exception, and fail the test otherwise.
 *
 * @since 2.19
 */
public class JacksonTestFailureExpectedInterceptor
    implements InvocationInterceptor
{
    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        try {
            invocation.proceed();
        } catch (Throwable t) {
            // do-nothing, we do expect an exception
            return;
        }
        handleUnexpectePassingTest(invocationContext);
    }

    private void handleUnexpectePassingTest(ReflectiveInvocationContext<Method> invocationContext) {
        // Collect information we need
        Object targetClass = invocationContext.getTargetClass();
        Object testMethod = invocationContext.getExecutable().getName();
        //List<Object> arguments = invocationContext.getArguments();

        // Create message
        String message = String.format("Test method %s.%s() passed, but should have failed", targetClass, testMethod);

        // throw exception
        throw new JacksonTestShouldFailException(message);
    }

}
