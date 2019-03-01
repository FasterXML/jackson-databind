package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface used to encapsulate rules that determine subtypes that
 * are invalid to use, even with default typing, mostly due to security
 * concerns.
 * Used by <code>BeanDeserializerFactory</code>.
 */
public interface SubTypeValidator {

    void validateSubType(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc)
        throws JsonMappingException;
}
