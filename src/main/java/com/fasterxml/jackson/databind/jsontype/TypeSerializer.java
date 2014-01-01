package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for serializing type information regarding instances of specified
 * base type (super class), so that exact subtype can be properly deserialized
 * later on. These instances are to be called by regular
 * {@link com.fasterxml.jackson.databind.JsonSerializer}s using proper contextual
 * calls, to add type information using mechanism type serializer was
 * configured with.
 */
public abstract class TypeSerializer
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
    public abstract TypeSerializer forProperty(BeanProperty prop);
    
    /*
    /**********************************************************
    /* Introspection
    /**********************************************************
     */

    /**
     * Accessor for type information inclusion method
     * that serializer uses; indicates how type information
     * is embedded in resulting JSON.
     */
    public abstract JsonTypeInfo.As getTypeInclusion();

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
    
    /*
    /**********************************************************
    /* Type serialization methods
    /**********************************************************
     */
    
    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as scalar JSON value (not as JSON
     * Object or Array).
     * This means that the context after call can not be that of JSON Object;
     * it may be Array or root context.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForScalar(Object value, JsonGenerator jgen) throws IOException;

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Object value (not as JSON
     * Array or scalar).
     * This means that context after call must be JSON Object, meaning that
     * caller can then proceed to output field entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException;

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Array value (not as JSON
     * Object or scalar).
     * This means that context after call must be JSON Array, that is, there
     * must be an open START_ARRAY to write contents in.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     */
    public abstract void writeTypePrefixForArray(Object value, JsonGenerator jgen) throws IOException;
    
    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForScalar}.
     * Actual action to take may depend on various factors, but has to match with
     * action {@link #writeTypePrefixForScalar} did (close array or object; or do nothing).
     */
    public abstract void writeTypeSuffixForScalar(Object value, JsonGenerator jgen) throws IOException;

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForObject}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException;

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypeSuffixForScalar}.
     * It needs to write closing END_ARRAY marker, and any other decoration
     * that needs to be matched.
     */
    public abstract void writeTypeSuffixForArray(Object value, JsonGenerator jgen) throws IOException;

    /**
     * Alternative version of the prefix-for-scalar method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        writeTypePrefixForScalar(value, jgen);
    }

    /**
     * Alternative version of the prefix-for-object method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        writeTypePrefixForObject(value, jgen);
    }

    /**
     * Alternative version of the prefix-for-array method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        writeTypePrefixForArray(value, jgen);
    }

    /*
    /**********************************************************
    /* Type serialization methods with type id override
    /**********************************************************
     */

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as scalar JSON value (not as JSON
     * Object or Array),
     * using specified custom type id instead of calling {@link TypeIdResolver}.
     * This means that the context after call can not be that of JSON Object;
     * it may be Array or root context.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     * @param typeId Exact type id to use
     */
    public abstract void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException, JsonProcessingException;
    
    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Object value (not as JSON
     * Array or scalar),
     * using specified custom type id instead of calling {@link TypeIdResolver}.
     * This means that context after call must be JSON Object, meaning that
     * caller can then proceed to output field entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param jgen Generator to use for writing type information
     * @param typeId Exact type id to use
     */
    public abstract void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException;
    
    public abstract void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException;

    public abstract void writeCustomTypeSuffixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException;

    public abstract void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException;

    public abstract void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException;
}
