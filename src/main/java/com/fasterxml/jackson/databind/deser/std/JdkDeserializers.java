package com.fasterxml.jackson.databind.deser.std;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Container class that contains serializers for JDK types that
 * require special handling for some reason.
 */
public class JdkDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        Class<?>[] types = new Class<?>[] {
                UUID.class,
                AtomicBoolean.class,
                StackTraceElement.class,
                ByteBuffer.class
        };
        for (Class<?> cls : types) { _classNames.add(cls.getName()); }
        for (Class<?> cls : FromStringDeserializer.types()) { _classNames.add(cls.getName()); }
    }

    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
    {
        if (_classNames.contains(clsName)) {
            JsonDeserializer<?> d = FromStringDeserializer.findDeserializer(rawType);
            if (d != null) {
                return d;
            }
            if (rawType == UUID.class) {
                return new UUIDDeserializer();
            }
            if (rawType == StackTraceElement.class) {
                return new StackTraceElementDeserializer();
            }
            if (rawType == AtomicBoolean.class) {
                return new AtomicBooleanDeserializer();
            }
            if (rawType == AtomicInteger.class) {
                return new AtomicIntegerDeserializer();
            }
            if (rawType == AtomicLong.class) {
                return new AtomicLongDeserializer();
            }
            if (rawType == ByteBuffer.class) {
                return new ByteBufferDeserializer();
            }
        }
        return null;
    }
}
