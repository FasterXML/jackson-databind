package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;

/**
 * Add-on interface that {@link JsonDeserializer}s can implement to get a callback
 * that can be used to create contextual (context-dependent) instances of
 * deserializer to use for  handling properties of supported type.
 * This can be useful
 * for deserializers that can be configured by annotations, or should otherwise
 * have differing behavior depending on what kind of property is being deserialized.
 *<p>
 * Note that in cases where deserializer needs both contextualization and
 * resolution -- that is, implements both this interface and {@link ResolvableDeserializer}
 * -- resolution via {@link ResolvableDeserializer} occurs first, and contextual
 * resolution (via this interface) later on.
 */
public interface ContextualDeserializer
{
    /**
     * Method called to see if a different (or differently configured) deserializer
     * is needed to deserialize values of specified property.
     * Note that instance that this method is called on is typically shared one and
     * as a result method should <b>NOT</b> modify this instance but rather construct
     * and return a new instance. This instance should only be returned as-is, in case
     * it is already suitable for use.
     *
     * @param ctxt Deserialization context to access configuration, additional
     *    deserializers that may be needed by this deserializer
     * @param property Method, field or constructor parameter that represents the property
     *   (and is used to assign deserialized value).
     *   Should be available; but there may be cases where caller cannot provide it and
     *   null is passed instead (in which case impls usually pass 'this' deserializer as is)
     *
     * @return Deserializer to use for deserializing values of specified property;
     *   may be this instance or a new instance.
     *
     * @throws JsonMappingException
     */
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
        throws JsonMappingException;
}
