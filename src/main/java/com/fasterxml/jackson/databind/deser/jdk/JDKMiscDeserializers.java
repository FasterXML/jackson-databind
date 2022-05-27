package com.fasterxml.jackson.databind.deser.jdk;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.NullifyingDeserializer;

/**
 * Container class that contains serializers for miscellaneous JDK types
 * that require special handling and are not grouped along with a set of
 * other serializers (like date/time)
 */
public class JDKMiscDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        _classNames.add(UUID.class.getName());
        _classNames.add(AtomicBoolean.class.getName());
        _classNames.add(AtomicInteger.class.getName());
        _classNames.add(AtomicLong.class.getName());
        _classNames.add(StackTraceElement.class.getName());
        _classNames.add(ByteBuffer.class.getName());
        _classNames.add(Void.class.getName());
        for (Class<?> cls : JDKFromStringDeserializer.types()) {
            _classNames.add(cls.getName());
        }
    }

    public static ValueDeserializer<?> find(DeserializationContext ctxt,
            Class<?> rawType, String clsName)
    {
        if (_classNames.contains(clsName)) {
            ValueDeserializer<?> d = JDKFromStringDeserializer.findDeserializer(rawType);
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
