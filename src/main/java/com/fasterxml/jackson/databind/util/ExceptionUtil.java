package com.fasterxml.jackson.databind.util;

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
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        }
    }

    /**
     * It is important never to catch all <code>Throwable</code>s. Some like
     * {@link InterruptedException} should be rethrown. Based on
     * <a href="https://www.scala-lang.org/api/2.13.10/scala/util/control/NonFatal$.html">scala.util.control.NonFatal</a>.
     *
     * @param throwable to check
     * @return whether the <code>Throwable</code> is a fatal error
     */
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
