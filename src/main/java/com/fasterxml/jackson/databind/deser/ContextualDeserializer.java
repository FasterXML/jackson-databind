package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;

/**
 * @deprecated Since 3.0: method demoted to <code>JsonDeserializer</code>
 */
@Deprecated
public interface ContextualDeserializer
{
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
        throws JsonMappingException;
}
