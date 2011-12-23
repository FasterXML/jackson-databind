package com.fasterxml.jackson.databind.deser;

/**
 * @deprecated Since 1.9, use {@link com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer} instead.
 */
@Deprecated
public abstract class StdScalarDeserializer<T>
    extends com.fasterxml.jackson.databind.deser.std.StdDeserializer<T>
{
    protected StdScalarDeserializer(Class<?> vc) {
        super(vc);
    } 
}
