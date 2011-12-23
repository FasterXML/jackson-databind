package com.fasterxml.jackson.databind.deser;

/**
 * @deprecated Since 1.9, use {@link com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer} instead.
 */
@Deprecated
public abstract class StdKeyDeserializer
    extends com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer
{
    protected StdKeyDeserializer(Class<?> cls) { super(cls); }
}

