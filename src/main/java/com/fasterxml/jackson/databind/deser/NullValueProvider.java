package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.AccessPattern;

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
     *<p>
     * NOTE: if {@link #getNullAccessPattern()} returns `ALWAYS_NULL` or
     * `CONSTANT`, this method WILL NOT use provided `ctxt` and it may thus
     * be passed as `null`.
     */
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException;

    /**
     * Accessor that may be used to determine if and when provider must be called to
     * access null replacement value.
     */
    public AccessPattern getNullAccessPattern(); 
}
