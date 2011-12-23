package com.fasterxml.jackson.databind.ser;

import org.codehaus.jackson.type.JavaType;

/**
 * @deprecated Since 1.9 use {@link com.fasterxml.jackson.databind.ser.std.SerializerBase}
 */
@Deprecated
public abstract class SerializerBase<T>
    extends com.fasterxml.jackson.databind.ser.std.SerializerBase<T>
{
    protected SerializerBase(Class<T> t) {
        super(t);
    }

    protected SerializerBase(JavaType type) {
        super(type);
    }
    
    protected SerializerBase(Class<?> t, boolean dummy) {
        super(t, dummy);
    }
}
