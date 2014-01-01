package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;


/**
 * Interface for deserializing type information from JSON content, to
 * type-safely deserialize data into correct polymorphic instance
 * (when type inclusion has been enabled for type handled).
 *<p>
 * Separate deserialization methods are needed because serialized
 * form for inclusion mechanism {@link As#PROPERTY}
 * is slighty different if value is not expressed as JSON Object:
 * and as such both type deserializer and serializer need to
 * JSON Object form (array, object or other (== scalar)) being used.
 */
public abstract class TypeDeserializer
{
    /*
    /**********************************************************
    /* Initialization
    /**********************************************************
     */

    /**
     * Method called to create contextual version, to be used for
     * values of given property. This may be the type itself
     * (as is the case for bean properties), or values contained
     * (for {@link java.util.Collection} or {@link java.util.Map}
     * valued properties).
     * 
     * @since 2.0
     */
    public abstract TypeDeserializer forProperty(BeanProperty prop);
    
    /*
    /**********************************************************
    /* Introspection
    /**********************************************************
     */

    /**
     * Accessor for type information inclusion method
     * that deserializer uses; indicates how type information
     * is (expected to be) embedded in JSON input.
     */
    public abstract As getTypeInclusion();

    /**
     * Name of property that contains type information, if
     * property-based inclusion is used.
     */
    public abstract String getPropertyName();

    /**
     * Accessor for object that handles conversions between
     * types and matching type ids.
     */
    public abstract TypeIdResolver getTypeIdResolver();

    /**
     * Accessor for "default implementation" type; optionally defined
     * class to use in cases where type id is not
     * accessible for some reason (either missing, or can not be
     * resolved)
     */
    public abstract Class<?> getDefaultImpl();
    
    /*
    /**********************************************************
    /* Type deserialization methods
    /**********************************************************
     */

    /**
     * Method called to let this type deserializer handle 
     * deserialization of "typed" object, when value itself
     * is serialized as JSON Object (regardless of Java type).
     * Method needs to figure out intended
     * polymorphic type, locate {@link JsonDeserializer} to use, and
     * call it with JSON data to deserializer (which does not contain
     * type information).
     */
    public abstract Object deserializeTypedFromObject(JsonParser jp, DeserializationContext ctxt) throws IOException;

    /**
     * Method called to let this type deserializer handle 
     * deserialization of "typed" object, when value itself
     * is serialized as JSON Array (regardless of Java type).
     * Method needs to figure out intended
     * polymorphic type, locate {@link JsonDeserializer} to use, and
     * call it with JSON data to deserializer (which does not contain
     * type information).
     */
    public abstract Object deserializeTypedFromArray(JsonParser jp, DeserializationContext ctxt) throws IOException;

    /**
     * Method called to let this type deserializer handle 
     * deserialization of "typed" object, when value itself
     * is serialized as a scalar JSON value (something other
     * than Array or Object), regardless of Java type.
     * Method needs to figure out intended
     * polymorphic type, locate {@link JsonDeserializer} to use, and
     * call it with JSON data to deserializer (which does not contain
     * type information).
     */
    public abstract Object deserializeTypedFromScalar(JsonParser jp, DeserializationContext ctxt) throws IOException;

    /**
     * Method called to let this type deserializer handle 
     * deserialization of "typed" object, when value itself
     * may have been serialized using any kind of JSON value
     * (Array, Object, scalar). Should only be called if JSON
     * serialization is polymorphic (not Java type); for example when
     * using JSON node representation, or "untyped" Java object
     * (which may be Map, Collection, wrapper/primitive etc).
     */
    public abstract Object deserializeTypedFromAny(JsonParser jp, DeserializationContext ctxt) throws IOException;

    /*
    /**********************************************************
    /* Shared helper methods
    /**********************************************************
     */

    /**
     * Helper method used to check if given parser might be pointing to
     * a "natural" value, and one that would be acceptable as the
     * result value (compatible with declared base type)
     */
    public static Object deserializeIfNatural(JsonParser jp, DeserializationContext ctxt, JavaType baseType) throws IOException {
        return deserializeIfNatural(jp, ctxt, baseType.getRawClass());
    }
    
    @SuppressWarnings("incomplete-switch")
    public static Object deserializeIfNatural(JsonParser jp, DeserializationContext ctxt, Class<?> base) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == null) {
            return null;
        }
        switch (t) {
        case VALUE_STRING:
            if (base.isAssignableFrom(String.class)) {
                return jp.getText();
            }
            break;
        case VALUE_NUMBER_INT:
            if (base.isAssignableFrom(Integer.class)) {
                return jp.getIntValue();
            }
            break;

        case VALUE_NUMBER_FLOAT:
            if (base.isAssignableFrom(Double.class)) {
                return Double.valueOf(jp.getDoubleValue());
            }
            break;
        case VALUE_TRUE:
            if (base.isAssignableFrom(Boolean.class)) {
                return Boolean.TRUE;
            }
            break;
        case VALUE_FALSE:
            if (base.isAssignableFrom(Boolean.class)) {
                return Boolean.FALSE;
            }
            break;
        }
        return null;
    }
}
    