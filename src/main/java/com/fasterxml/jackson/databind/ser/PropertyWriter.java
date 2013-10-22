package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for writers used to output property values (name-value pairs)
 * as key/value pairs via streaming API. This is the most generic abstraction
 * implemented by both POJO and {@link java.util.Map} serializers, and invoked
 * by filtering functionality.
 * 
 * @since 2.3
 */
public abstract class PropertyWriter
{
    /*
    /**********************************************************
    /* Metadata access
    /**********************************************************
     */

    public abstract String getName();

    public abstract PropertyName getFullName();
    
    /*
    /**********************************************************
    /* Serialization methods, regular output
    /**********************************************************
     */

    /**
     * The main serialization method called by filter when property is to be written normally.
     */
    public abstract void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;

    /**
     * Serialization method that filter needs to call in cases where property is to be
     * filtered, but the underlying data format requires a placeholder of some kind.
     * This is usually the case for tabular (positional) data formats such as CSV.
     */
    public abstract void serializeAsOmittedField(Object pojo, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;

    /*
    /**********************************************************
    /* Serialization methods, explicit positional/tabular formats
    /**********************************************************
     */

    /**
     * Serialization method called when output is to be done as an array,
     * that is, not using property names. This is needed when serializing
     * container ({@link java.util.Collection}, array) types,
     * or POJOs using <code>tabular</code> ("as array") output format.
     *<p>
     * Note that this mode of operation is independent of underlying
     * data format; so it is typically NOT called for fully tabular formats such as CSV,
     * where logical output is still as form of POJOs.
     */
    public abstract void serializeAsElement(Object pojo, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;

    /**
     * Serialization method called when doing tabular (positional) output from databind,
     * but then value is to be omitted. This requires output of a placeholder value
     * of some sort; often similar to {@link #serializeAsOmittedField}.
     */
    public abstract void serializeAsPlaceholder(Object pojo, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;

    /*
    /**********************************************************
    /* Schema-related
    /**********************************************************
     */

    /**
     * Traversal method used for things like JSON Schema generation, or
     * POJO introspection.
     */
    public abstract void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor)
        throws JsonMappingException;


    /**
     * Legacy method called for JSON Schema generation; should not be called by new code
     * 
     * @deprecated Since 2.2
     */
    @Deprecated
    public abstract void depositSchemaProperty(ObjectNode propertiesNode, SerializerProvider provider)
        throws JsonMappingException;
}
