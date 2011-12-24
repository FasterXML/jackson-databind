package com.fasterxml.jackson.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Interface that can be implemented by objects that know how to
 * serialize themselves to JSON, using {@link JsonGenerator}
 * (and {@link SerializerProvider} if necessary).
 *<p>
 * Note that implementing this interface binds implementing object
 * closely to Jackson API, and that it is often not necessary to do
 * so -- if class is a bean, it can be serialized without
 * implementing this interface.
 *<p>
 * NOTE: Jackson 2.0 added another method (from former "JsonSerializableWithType"),
 * which is required for proper handling of case where additional type information
 * is needed.
 */
public interface JsonSerializable
{
    /**
     * Serialization method called when no additional type information is
     * to be included in serialization.
     */
    public void serialize(JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     * Serialization method called when additional type information is
     * expected to be included in serialization, for deserialization to use.
     *<p>
     * Usually implementation consists of a call to one of methods
     * in {@link TypeSerializer} (such as {@link TypeSerializer#writeTypePrefixForObject(Object, JsonGenerator)})
     * followed by serialization of contents,
     * followed by another call to {@link TypeSerializer}
     * (such as {@link TypeSerializer#writeTypeSuffixForObject(Object, JsonGenerator)}).
     * Exact methods to call in {@link TypeSerializer} depend on shape of JSON Object used
     * (Array, Object or scalar like String/Number/Boolean).
     *<p>
     * Note that some types (most notably, "natural" types: String, Integer,
     * Double and Boolean) never include type information.
     */
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException;
}
