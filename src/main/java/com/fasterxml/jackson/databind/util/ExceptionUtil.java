package com.fasterxml.jackson.databind.util;

/**
 * Utilitity methods for dealing with exceptions/throwables
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
     * @param throwable to check
     * @return whether the <code>Throwable</code> is a fatal error
     */
    public static boolean isFatal(Throwable throwable) {
        return (throwable instanceof VirtualMachineError
                || throwable instanceof ThreadDeath
                || throwable instanceof InterruptedException
                || throwable instanceof LinkageError);
    }

    /**
     * Designed to be used in conjunction with {@link #isFatal(Throwable)}.
     * This method should be used with care.
     * <p>
     *     The input throwable is thrown if it is an <code>Error</code> or <code>RuntimeException</code>.
     *     Otherwise, the method wraps the throwable in a RuntimeException and rethrows that.
     * </p>
     *
     * @param throwable to check
     * @throws Error the input throwable if it is an <code>Error</code>.
     * @throws RuntimeException the input throwable if it is an <code>RuntimeException</code>
     * Otherwise wraps the throwable in a RuntimeException.
     */
    public static void rethrow(Throwable throwable) throws Error, RuntimeException {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        throw new RuntimeException(throwable);
    }
}
