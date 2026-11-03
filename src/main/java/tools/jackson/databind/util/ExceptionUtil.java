package tools.jackson.databind.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;

/**
 * Utility methods for dealing with exceptions/throwables
 *
 * @since 2.15
 */
public class ExceptionUtil {
    private ExceptionUtil() {}

    /**
     * It is important never to catch all <code>Throwable</code>s. Some like
     * {@link InterruptedException} should be rethrown. Based on
     * <a href="https://www.scala-lang.org/api/2.13.10/scala/util/control/NonFatal$.html">scala.util.control.NonFatal</a>.
     *
     * This method should be used with care.
     * <p>
     *     If the <code>Throwable</code> is fatal, it is rethrown, otherwise, this method just returns.
     *     The input throwable is thrown if it is an <code>Error</code> or a <code>RuntimeException</code>.
     *     Otherwise, the method wraps the throwable in a RuntimeException and throws that.
     * </p>
     *
     * @param throwable to check
     * @throws Error the input throwable if it is fatal
     * @throws RuntimeException the input throwable if it is fatal - throws the original throwable
     * if is a <code>RuntimeException</code>. Otherwise, wraps the throwable in a RuntimeException.
     */
    public static void rethrowIfFatal(Throwable throwable) throws Error, RuntimeException {
        if (isFatal(throwable)) {
            if (throwable instanceof Error error) {
                throw error;
            }
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Helper method that will either throw given throwable -- if (and only if)
     * one of following is true:
     *
     * 1. It is an {@link Error}
     * 2. It is a {@link JacksonException}
     * 3. {@code DeserializationFeature.WRAP_EXCEPTIONS} is NOT enabled AND
     *    exception is a {@link RuntimeException}
     *
     * -- or (otherwise) returns throwable as-is.
     *
     * @param ctxt Current deserialization context
     * @param e Exception caught to possibly re-throw
     *
     * @return Exception passed in for call chaining
     *
     * @since 3.1
     */
    public static <ERR extends Throwable> ERR rethrowIfNoWrap(DeserializationContext ctxt, ERR e)
    {
        if (e instanceof Error err) {
            throw err;
        }
        if (e instanceof JacksonException je) {
            throw je;
        }
        if ((ctxt != null)
                && !ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)
                && e instanceof RuntimeException re) {
            throw re;
        }
        return e;
    }

    /**
     * It is important never to catch all <code>Throwable</code>s. Some like
     * {@link InterruptedException} should be rethrown. Based on
     * <a href="https://www.scala-lang.org/api/2.13.10/scala/util/control/NonFatal$.html">scala.util.control.NonFatal</a>.
     *
     * @param throwable to check
     * @return whether the <code>Throwable</code> is a fatal error
     */
    @SuppressWarnings("removal")
    private static boolean isFatal(Throwable throwable) {
        return (throwable instanceof VirtualMachineError
                || throwable instanceof ThreadDeath
                || throwable instanceof InterruptedException
                || throwable instanceof ClassCircularityError
                || throwable instanceof ClassFormatError
                || throwable instanceof IncompatibleClassChangeError
                || throwable instanceof BootstrapMethodError
                || throwable instanceof VerifyError
        );
    }
}
