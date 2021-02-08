package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Container class for core pre-Java8 JDK date/time type deserializers.
 */
public class JDKDateDeserializers
{
    private final static HashSet<String> _utilClasses = new HashSet<String>();
    static {
        _utilClasses.add("java.util.Calendar");
        _utilClasses.add("java.util.GregorianCalendar");
        _utilClasses.add("java.util.Date");
    }

    public static ValueDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (_utilClasses.contains(clsName)) {
            // Start with the most common type
            if (rawType == java.util.Calendar.class) {
                return new JavaUtilCalendarDeserializer();
            }
            if (rawType == java.util.Date.class) {
                return JavaUtilDateDeserializer.instance;
            }
            if (rawType == java.util.GregorianCalendar.class) {
                return new JavaUtilCalendarDeserializer(GregorianCalendar.class);
            }
        }
        return null;
    }

    public static boolean hasDeserializerFor(Class<?> rawType) {
        return _utilClasses.contains(rawType.getName());
    }

    /*
    /**********************************************************************
    /* Deserializer implementations for Date types
    /**********************************************************************
     */
}
