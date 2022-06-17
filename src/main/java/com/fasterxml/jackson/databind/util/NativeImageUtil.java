package com.fasterxml.jackson.databind.util;

import java.lang.reflect.InvocationTargetException;

/**
 * Utilities for graal native image support; mostly to improve error message handling
 * in case of missing information for native image.
 *
 * @since 2.14
 */
public class NativeImageUtil {
    private static final boolean RUNNING_IN_SVM;

    static {
        RUNNING_IN_SVM = System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    private NativeImageUtil() {
    }

    /**
     * Check whether we're running in substratevm native image runtime mode. This check cannot be a constant, because
     * the static initializer may run early during build time
     */
    private static boolean isRunningInNativeImage() {
        return RUNNING_IN_SVM && "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    /**
     * Check whether the given error is a substratevm UnsupportedFeatureError
     */
    public static boolean isUnsupportedFeatureError(Throwable e) {
        if (!isRunningInNativeImage()) {
            return false;
        }
        if (e instanceof InvocationTargetException) {
            e = e.getCause();
        }
        return e.getClass().getName().equals("com.oracle.svm.core.jdk.UnsupportedFeatureError");
    }

    /**
     * Check whether the given class is likely missing reflection configuration (running in native image, and no
     * members visible in reflection).
     */
    public static boolean needsReflectionConfiguration(Class<?> cl) {
        if (!isRunningInNativeImage()) {
            return false;
        }
        // records list their fields but not other members
        return (cl.getDeclaredFields().length == 0 || ClassUtil.isRecordType(cl)) &&
                cl.getDeclaredMethods().length == 0 &&
                cl.getDeclaredConstructors().length == 0;
    }
}
