package com.fasterxml.jackson.databind.deser.std;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.*;

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
                AtomicInteger.class,
                AtomicLong.class,
                StackTraceElement.class,
                ByteBuffer.class,
                Void.class
        };
        for (Class<?> cls : types) { _classNames.add(cls.getName()); }
        for (Class<?> cls : FromStringDeserializer.types()) { _classNames.add(cls.getName()); }
    }

    /**
     * @deprecated Since 2.14 use the variant that takes one more argument
     */
    @Deprecated // since 2.14
    public static JsonDeserializer<?> find(Class<?> rawType, String clsName)
        throws JsonMappingException
    {
        return find(null, rawType, clsName);
    }

    /**
     * @since 2.14
     */
    public static JsonDeserializer<?> find(DeserializationContext ctxt,
            Class<?> rawType, String clsName)
        throws JsonMappingException
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
                return StackTraceElementDeserializer.construct(ctxt);
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
            if (rawType == Void.class) {
                return NullifyingDeserializer.instance;
            }
        }
        return null;
    }

    // @since 2.11
    public static boolean hasDeserializerFor(Class<?> rawType) {
        return _classNames.contains(rawType.getName());
    }
}
