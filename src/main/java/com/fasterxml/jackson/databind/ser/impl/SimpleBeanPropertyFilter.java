package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

/**
 * Simple {@link BeanPropertyFilter} implementation that only uses property name
 * to determine whether to serialize property as is, or to filter it out.
 */
public abstract class SimpleBeanPropertyFilter
    implements BeanPropertyFilter // sub-classes must also implement java.io.Serializable
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected SimpleBeanPropertyFilter() { }

    /**
     * Factory method to construct filter that filters out all properties <b>except</b>
     * ones includes in set
     */
    public static SimpleBeanPropertyFilter filterOutAllExcept(Set<String> properties) {
        return new FilterExceptFilter(properties);
    }

    public static SimpleBeanPropertyFilter filterOutAllExcept(String... propertyArray) {
        HashSet<String> properties = new HashSet<String>(propertyArray.length);
        Collections.addAll(properties, propertyArray);
        return new FilterExceptFilter(properties);
    }

    public static SimpleBeanPropertyFilter serializeAllExcept(Set<String> properties) {
        return new SerializeExceptFilter(properties);
    }

    public static SimpleBeanPropertyFilter serializeAllExcept(String... propertyArray) {
        HashSet<String> properties = new HashSet<String>(propertyArray.length);
        Collections.addAll(properties, propertyArray);
        return new SerializeExceptFilter(properties);
    }

    /*
    /**********************************************************
    /* Methods for sub-classes
    /**********************************************************
     */

    /**
     * Method called to determine whether property will be included
     * (if 'true' returned) or filtered out (if 'false' returned)
     */
    protected abstract boolean include(BeanPropertyWriter writer);

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen,
            SerializerProvider provider, BeanPropertyWriter writer) throws Exception
    {
        if (include(writer)) {
            writer.serializeAsField(bean, jgen, provider);
        }
    }

    @Override
    public void depositSchemaProperty(BeanPropertyWriter writer,
            ObjectNode propertiesNode, SerializerProvider provider)
        throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(propertiesNode, provider);
        }
    }

    @Override
    public void depositSchemaProperty(BeanPropertyWriter writer,
            JsonObjectFormatVisitor objectVisitor, SerializerProvider provider)
        throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(objectVisitor);
        }
    }
    
    /*
    /**********************************************************
    /* Sub-classes
    /**********************************************************
     */

    /**
     * Filter implementation which defaults to filtering out unknown
     * properties and only serializes ones explicitly listed.
     */
    public static class FilterExceptFilter
        extends SimpleBeanPropertyFilter
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        /**
         * Set of property names to serialize.
         */
        protected final Set<String> _propertiesToInclude;

        public FilterExceptFilter(Set<String> properties) {
            _propertiesToInclude = properties;
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return _propertiesToInclude.contains(writer.getName());
        }
    }

    /**
     * Filter implementation which defaults to serializing all
     * properties, except for ones explicitly listed to be filtered out.
     */
    public static class SerializeExceptFilter
        extends SimpleBeanPropertyFilter
    {
        /**
         * Set of property names to filter out.
         */
        protected final Set<String> _propertiesToExclude;

        public SerializeExceptFilter(Set<String> properties) {
            _propertiesToExclude = properties;
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return !_propertiesToExclude.contains(writer.getName());
        }
    }
}