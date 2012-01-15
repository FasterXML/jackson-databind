package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;

/**
 * Add-on interface that {@link JsonSerializer}s can implement to get a callback
 * that can be used to create contextual instances of serializer to use for
 * handling properties of supported type. This can be useful
 * for serializers that can be configured by annotations, or should otherwise
 * have differing behavior depending on what kind of property is being serialized.
 *<p>
 * Note that in cases where serializer needs both contextualization and
 * resolution -- that is, implements both this interface and {@link ResolvableSerializer}
 * -- resolution via {@link ResolvableSerializer} occurs first, and contextual
 * resolution (via this interface) later on.
 *
 * @param <T> Type of serializer to contextualize
 */
public interface ContextualSerializer<T>
{
    /**
     * Method called to see if a different (or differently configured) serializer
     * is needed to serialize values of specified property.
     * Note that instance that this method is called on is typically shared one and
     * as a result method should <b>NOT</b> modify this instance but rather construct
     * and return a new instance. This instance should only be returned as-is, in case
     * it is already suitable for use.
     * 
     * @param config Current serialization configuration
     * @param property Method or field that represents the property
     *   (and is used to access value to serialize).
     *   Should be available; but there may be cases where caller can not provide it and
     *   null is passed instead (in which case impls usually pass 'this' serializer as is)
     * 
     * @return Serializer to use for serializing values of specified property;
     *   may be this instance or a new instance.
     * 
     * @throws JsonMappingException
     */
    public JsonSerializer<T> createContextual(SerializationConfig config,
            BeanProperty property)
        throws JsonMappingException;
}
