package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;

/**
 * To support [JACKSON-420] we need bit more indirection; this is used to produce
 * artificial failure for primitives that don't accept JSON null as value.
 */
public final class NullProvider
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object _nullValue;

    private final boolean _isPrimitive;
    
    private final Class<?> _rawType;
    
    public NullProvider(JavaType type, Object nullValue)
    {
        _nullValue = nullValue;
        // [JACKSON-420]
        _isPrimitive = type.isPrimitive();
        _rawType = type.getRawClass();
    }

    public Object nullValue(DeserializationContext ctxt) throws JsonProcessingException
    {
        if (_isPrimitive && ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
            throw ctxt.mappingException("Can not map JSON null into type "+_rawType.getName()
                    +" (set DeserializationConfig.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES to 'false' to allow)");
        }
        return _nullValue;
    }
}