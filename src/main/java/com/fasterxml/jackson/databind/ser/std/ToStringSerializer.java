package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * Simple general purpose serializer, useful for any
 * type for which {@link Object#toString} returns the desired JSON
 * value.
 *<p>
 * Since 2.10 extends {@link ToStringSerializerBase}
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class ToStringSerializer
    extends ToStringSerializerBase
{
    /**
     * Singleton instance to use.
     */
    public final static ToStringSerializer instance = new ToStringSerializer();

    /**
     *<p>
     * Note: usually you should NOT create new instances, but instead use
     * {@link #instance} which is stateless and fully thread-safe. However,
     * there are cases where constructor is needed; for example,
     * when using explicit serializer annotations like
     * {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}.
     */
    public ToStringSerializer() { super(Object.class); }

    /**
     * Sometimes it may actually make sense to retain actual handled type.
     *
     * @since 2.5
     */
    public ToStringSerializer(Class<?> handledType) {
        super(handledType);
    }

    @Override
    public final String valueToString(Object value) {
        return value.toString();
    }
}
