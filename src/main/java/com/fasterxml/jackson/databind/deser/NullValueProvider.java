package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Helper interface implemented by classes that are to be used as
 * null providers during deserialization. Most importantly implemented by
 * {@link com.fasterxml.jackson.databind.JsonDeserializer} (as a mix-in
 * interface), but also by converters used to support more configurable
 * null replacement.
 *
 * @since 2.9
 */
public interface NullValueProvider
{
    /**
     * Method called to possibly convert incoming `null` token (read via
     * underlying streaming input source) into other value of type accessor
     * supports. May return `null`, or value compatible with type binding.
     */
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException;
}
