package com.fasterxml.jackson.databind;

import tools.jackson.core.*;

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
 * Note that while it is possible to just directly implement {@link JacksonSerializable},
 * actual implementations are strongly recommended to instead extend
 * {@link JacksonSerializable.Base}.
 *<p>
 * NOTE: in Jackson 2.x this type was named {@code JsonSerializable}
 */
public interface JacksonSerializable
{
    /**
     * Serialization method called when no additional type information is
     * to be included in serialization.
     */
    public void serialize(JsonGenerator gen, SerializerProvider serializers)
        throws JacksonException;

    /**
     * Serialization method called when additional type information is
     * expected to be included in serialization, for deserialization to use.
     *<p>
     * Usually implementation consists of a call to {@link TypeSerializer#writeTypePrefix}
     * followed by serialization of contents,
     * followed by a call to {@link TypeSerializer#writeTypeSuffix}).
     * Details of the type id argument to pass depend on shape of JSON Object used
     * (Array, Object or scalar like String/Number/Boolean).
     *<p>
     * Note that some types (most notably, "natural" types: String, Integer,
     * Double and Boolean) never include type information.
     */
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer)
        throws JacksonException;

    /**
     * Base class with minimal implementation, as well as couple of extension methods
     * that core Jackson databinding makes use of.
     * Use of this base class is strongly recommended over directly implementing
     * {@link JacksonSerializable}.
     */
    public abstract static class Base implements JacksonSerializable
    {
        /**
         * Method that may be called on instance to determine if it is considered
         * "empty" for purposes of serialization filtering or not.
         */
        public boolean isEmpty(SerializerProvider serializers) {
            return false;
        }
    }
}
