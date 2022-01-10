package com.fasterxml.jackson.databind.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Helper class that contains functionality needed by both serialization
 * and deserialization side.
 */
public class BeanUtil
{
    /*
    /**********************************************************************
    /* Name mangling
    /**********************************************************************
     */

    public static String stdManglePropertyName(final String basename, final int offset)
    {
        final int end = basename.length();
        if (end == offset) { // empty name, nope
            return null;
        }
        // first: if it doesn't start with capital, return as-is
        char c0 = basename.charAt(offset);
        char c1 = Character.toLowerCase(c0);
        if (c0 == c1) {
            return basename.substring(offset);
        }
        // 17-Dec-2014, tatu: As per [databind#653], need to follow more
        //   closely Java Beans spec; specifically, if two first are upper-case,
        //   then no lower-casing should be done.
        if ((offset + 1) < end) {
            if (Character.isUpperCase(basename.charAt(offset+1))) {
                return basename.substring(offset);
            }
        }
        StringBuilder sb = new StringBuilder(end - offset);
        sb.append(c1);
        sb.append(basename, offset+1, end);
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Value defaulting helpers
    /**********************************************************************
     */
    
    /**
     * Accessor used to find out "default value" to use for comparing values to
     * serialize, to determine whether to exclude value from serialization with
     * inclusion type of {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}.
     *<p>
     * Default logic is such that for primitives and wrapper types for primitives, expected
     * defaults (0 for `int` and `java.lang.Integer`) are returned; for Strings, empty String,
     * and for structured (Maps, Collections, arrays) and reference types, criteria
     * {@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_DEFAULT}
     * is used.
     */
    public static Object getDefaultValue(JavaType type)
    {
        // 06-Nov-2015, tatu: Returning null is fine for Object types; but need special
        //   handling for primitives since they are never passed as nulls.
        Class<?> cls = type.getRawClass();

        // 30-Sep-2016, tatu: Also works for Wrappers, so both `Integer.TYPE` and `Integer.class`
        //    would return `Integer.TYPE`
        Class<?> prim = ClassUtil.primitiveType(cls);
        if (prim != null) {
            return ClassUtil.defaultValue(prim);
        }
        if (type.isContainerType() || type.isReferenceType()) {
            return JsonInclude.Include.NON_EMPTY;
        }
        if (cls == String.class) {
            return "";
        }
        // 09-Mar-2016, tatu: Not sure how far this path we want to go but for now
        //   let's add `java.util.Date` and `java.util.Calendar`, as per [databind#1550]
        if (type.isTypeOrSubTypeOf(Date.class)) {
            return new Date(0L);
        }
        if (type.isTypeOrSubTypeOf(Calendar.class)) {
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(0L);
            return c;
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Package-specific type detection for error handling
    /**********************************************************************
     */

    /**
     * Helper method called by {@link com.fasterxml.jackson.databind.deser.BeanDeserializerFactory}
     * and {@link com.fasterxml.jackson.databind.ser.BeanSerializerFactory} to check
     * if given unrecognized type (to be (de)serialized as general POJO) is one of
     * "well-known" types for which there would be a datatype module; and if so,
     * return appropriate failure message to give to caller.
     */
    public static String checkUnsupportedType(JavaType type) {
        final String className = type.getRawClass().getName();
        String typeName, moduleName;

        if (isJava8TimeClass(className)) {
            // [modules-java8#207]: do NOT check/block helper types in sub-packages,
            // but only main-level types (to avoid issues with module)
            if (className.indexOf('.', 10) >= 0) {
                return null;
            }
            typeName =  "Java 8 date/time";
            moduleName = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310";
        } else if (isJodaTimeClass(className)) {
            typeName =  "Joda date/time";
            moduleName = "com.fasterxml.jackson.datatype:jackson-datatype-joda";
        } else {
            return null;
        }
        return String.format("%s type %s not supported by default: add Module \"%s\" to enable handling",
                typeName, ClassUtil.getTypeDescription(type), moduleName);
    }

    public static boolean isJava8TimeClass(Class<?> rawType) {
        return isJava8TimeClass(rawType.getName());
    }

    private static boolean isJava8TimeClass(String className) {
        return className.startsWith("java.time.");
    }

    public static boolean isJodaTimeClass(Class<?> rawType) {
        return isJodaTimeClass(rawType.getName());
    }

    private static boolean isJodaTimeClass(String className) {
        return className.startsWith("org.joda.time.");
    }
}
