package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;

/**
 * @deprecated
 */
@Deprecated // since 2.6, remove in 2.7
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
        _isPrimitive = type.isPrimitive();
        _rawType = type.getRawClass();
    }

    public Object nullValue(DeserializationContext ctxt) throws JsonProcessingException
    {
        if (_isPrimitive && ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
            throw ctxt.mappingException("Can not map JSON null into type %s"
                    +" (set DeserializationConfig.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES to 'false' to allow)",
                    _rawType.getName());                    
        }
        return _nullValue;
    }
}