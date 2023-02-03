package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface that defines API for filter objects use (as configured
 * using {@link com.fasterxml.jackson.annotation.JsonFilter})
 * for filtering bean properties to serialize.
 *<p>
 * Starting with version 2.3 this class is deprecated; use
 * {@link PropertyFilter} instead.
 *
 * @deprecated Since 2.3: use {@link PropertyFilter} instead.
 */
@Deprecated
public interface BeanPropertyFilter
{
    /**
     * Method called by {@link BeanSerializer} to let filter decide what to do with
     * given bean property value: the usual choices are to either filter out (i.e.
     * do nothing) or write using given {@link BeanPropertyWriter}, although filters
     * can choose other to do something different altogether.
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.serializeAsField(pojo, jgen, prov);
     * }
     *</pre>
     *
     * @param pojo Object that contains property value to serialize
     * @param jgen Generator use for serializing value
     * @param prov Provider that can be used for accessing dynamic aspects of serialization
     *    processing
     * @param writer Default bean property serializer to use
     */
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov,
            BeanPropertyWriter writer)
        throws Exception;

    /**
     * Method called by {@link BeanSerializer} to let the filter determine whether, and in what
     * form the given property exist within the parent, or root, schema. Filters can omit
     * adding the property to the node, or choose the form of the schema value for the property.
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.depositSchemaProperty(propertiesNode, provider);
     * }
     *</pre>
     *
     * @param writer Bean property writer to use to create schema value
     * @param propertiesNode Node which the given property would exist within
     * @param provider Provider that can be used for accessing dynamic aspects of serialization
     * 	processing
     *
     * @since 2.1
     * @deprecated Since 2.3: new code should use the alternative <code>depositSchemaProperty</code>
     *   method
     */
    @Deprecated
    public void depositSchemaProperty(BeanPropertyWriter writer, ObjectNode propertiesNode,
            SerializerProvider provider)
        throws JsonMappingException;

    /**
     * Method called by {@link BeanSerializer} to let the filter determine whether, and in what
     * form the given property exist within the parent, or root, schema. Filters can omit
     * adding the property to the node, or choose the form of the schema value for the property
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.depositSchemaProperty(objectVisitor, provider);
     * }
     *</pre>
     *
     * @param writer Bean property serializer to use to create schema value
     * @param objectVisitor JsonObjectFormatVisitor which should be aware of
     * the property's existence
     * @param provider Provider that can be used for accessing dynamic aspects of serialization
     * 	processing
     *
     * @since 2.1
     */
    public void depositSchemaProperty(BeanPropertyWriter writer, JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
        throws JsonMappingException;
}
