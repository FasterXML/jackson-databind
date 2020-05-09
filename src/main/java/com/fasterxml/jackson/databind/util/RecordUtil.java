package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Helper class to detect Java records without Java 14 as Jackson targets is Java 8.
 * <p>
 * See <a href="https://openjdk.java.net/jeps/359">JEP 359</a>
 */
public final class RecordUtil {

    private static final String RECORD_CLASS_NAME = "java.lang.Record";
    private static final String RECORD_GET_RECORD_COMPONENTS = "getRecordComponents";

    private static final String RECORD_COMPONENT_CLASS_NAME = "java.lang.reflect.RecordComponent";
    private static final String RECORD_COMPONENT_GET_NAME = "getName";
    private static final String RECORD_COMPONENT_GET_TYPE = "getType";

    public static boolean isRecord(Class<?> aClass) {
        return aClass != null
                && aClass.getSuperclass() != null
                && aClass.getSuperclass().getName().equals(RECORD_CLASS_NAME);
    }

    /**
     * @return Record component's names, ordering is preserved.
     */
    public static String[] getRecordComponents(Class<?> aRecord) {
        if (!isRecord(aRecord)) {
            return new String[0];
        }

        try {
            Method method = Class.class.getMethod(RECORD_GET_RECORD_COMPONENTS);
            Object[] components = (Object[]) method.invoke(aRecord);
            String[] names = new String[components.length];
            Method recordComponentGetName = Class.forName(RECORD_COMPONENT_CLASS_NAME).getMethod(RECORD_COMPONENT_GET_NAME);
            for (int i = 0; i < components.length; i++) {
                Object component = components[i];
                names[i] = (String) recordComponentGetName.invoke(component);
            }
            return names;
        } catch (Throwable e) {
            return new String[0];
        }
    }

    public static AnnotatedConstructor getCanonicalConstructor(AnnotatedClass aRecord) {
        if (!isRecord(aRecord.getAnnotated())) {
            return null;
        }

        Class<?>[] paramTypes = getRecordComponentTypes(aRecord.getAnnotated());
        for (AnnotatedConstructor constructor : aRecord.getConstructors()) {
            if (Arrays.equals(constructor.getAnnotated().getParameterTypes(), paramTypes)) {
                return constructor;
            }
        }
        return null;
    }

    private static Class<?>[] getRecordComponentTypes(Class<?> aRecord) {
        try {
            Method method = Class.class.getMethod(RECORD_GET_RECORD_COMPONENTS);
            Object[] components = (Object[]) method.invoke(aRecord);
            Class<?>[] types = new Class[components.length];
            Method recordComponentGetName = Class.forName(RECORD_COMPONENT_CLASS_NAME).getMethod(RECORD_COMPONENT_GET_TYPE);
            for (int i = 0; i < components.length; i++) {
                Object component = components[i];
                types[i] = (Class<?>) recordComponentGetName.invoke(component);
            }
            return types;
        } catch (Throwable e) {
            return new Class[0];
        }
    }
}
