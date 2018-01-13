package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;

/**
 * @deprecated Since 3.0: method demoted to <code>JsonSerializer</code>
 */
@Deprecated
public interface ContextualSerializer
{
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        throws JsonMappingException;
}
