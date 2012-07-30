package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface that defines API for filter objects use (as configured
 * using {@link com.fasterxml.jackson.annotation.JsonFilter})
 * for filtering bean properties to serialize.
 */
public interface BeanPropertyFilter
{
    /**
     * Method called by {@link BeanSerializer} to let filter decide what to do with
     * given bean property value: the usual choices are to either filter out (i.e.
     * do nothing) or write using given {@link BeanPropertyWriter}, although filters
     * can choose other to do something different altogether.
     * 
     * @param bean Bean of which property value to serialize
     * @param jgen Generator use for serializing value
     * @param prov Provider that can be used for accessing dynamic aspects of serialization
     *    processing
     * @param writer Default bean property serializer to use
     */
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov,
            BeanPropertyWriter writer)
        throws Exception;
    
    /**
     * Method called by {@link BeanSerializer} to let the filter determine whether, and in what
     * form the given property exist within the parent, or root, schema. Filters can omit
     * adding the property to the node, or choose the form of the schema value for the property
     * 
     * @param writer Bean property serializer to use to create schema value
     * @param propertiesNode Node which the given property would exist within
     * @param provider Provider that can be used for accessing dynamic aspects of serialization
     * 	processing
     *
     */
    public void depositSchemaProperty(BeanPropertyWriter writer, JsonObjectFormatVisitor objectVisitor,
    		SerializerProvider provider);
}
