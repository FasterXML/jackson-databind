package com.fasterxml.jackson.databind.ser;

/**
 * @deprecated Since 1.9 use {@link  com.fasterxml.jackson.databind.ser.std.SerializerBase} instead.
 */
@Deprecated
public abstract class ScalarSerializerBase<T>
    extends com.fasterxml.jackson.databind.ser.std.SerializerBase<T>
{
    protected ScalarSerializerBase(Class<T> t) {
        super(t);
    }

    @SuppressWarnings("unchecked")
    protected ScalarSerializerBase(Class<?> t, boolean dummy) {
        super((Class<T>) t);
    }
}
