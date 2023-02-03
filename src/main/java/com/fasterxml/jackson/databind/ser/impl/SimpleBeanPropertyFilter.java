package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.*;

/**
 * Simple {@link PropertyFilter} implementation that only uses property name
 * to determine whether to serialize property as is, or to filter it out.
 *<p>
 * Use of this class as the base implementation for any custom
 * {@link PropertyFilter} implementations is strongly encouraged,
 * because it can provide default implementation for any methods that may
 * be added in {@link PropertyFilter} (as unfortunate as additions may be).
 */
@SuppressWarnings("deprecation")
public class SimpleBeanPropertyFilter
    implements BeanPropertyFilter, PropertyFilter
        // sub-classes must also implement java.io.Serializable
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected SimpleBeanPropertyFilter() { }

    /**
     * Convenience factory method that will return a "no-op" filter that will
     * simply just serialize all properties that are given, and filter out
     * nothing.
     *
     * @since 2.6
     */
    public static SimpleBeanPropertyFilter serializeAll() {
        return SerializeExceptFilter.INCLUDE_ALL;
    }

    /**
     * Factory method that was accidentally added in 2.5 with arguments; basically
     * works just as an alias of {@link #filterOutAllExcept(Set)} which is not
     * very useful. Instead, see {@link #serializeAll()} for intended signature.
     *
     * @deprecated Since 2.6; to be removed from 2.7
     */
    @Deprecated
    public static SimpleBeanPropertyFilter serializeAll(Set<String> properties) {
        return new FilterExceptFilter(properties);
    }

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

    /**
     * Helper method to ease transition from {@link BeanPropertyWriter} into
     * {@link PropertyWriter}
     *
     * @since 2.3
     */
    public static PropertyFilter from(final BeanPropertyFilter src)
    {
        return new PropertyFilter() {
            @Override
            public void serializeAsField(Object pojo, JsonGenerator jgen,
                    SerializerProvider prov, PropertyWriter writer)
                throws Exception {
                src.serializeAsField(pojo, jgen, prov, (BeanPropertyWriter) writer);
            }

            @Override
            public void depositSchemaProperty(PropertyWriter writer,
                    ObjectNode propertiesNode, SerializerProvider provider)
                throws JsonMappingException {
                src.depositSchemaProperty((BeanPropertyWriter) writer, propertiesNode, provider);
            }

            @Override
            public void depositSchemaProperty(PropertyWriter writer,
                    JsonObjectFormatVisitor objectVisitor,
                SerializerProvider provider) throws JsonMappingException {
                src.depositSchemaProperty((BeanPropertyWriter) writer, objectVisitor, provider);
            }

            @Override
            public void serializeAsElement(Object elementValue,
                    JsonGenerator jgen, SerializerProvider prov,
                    PropertyWriter writer) throws Exception {
                // not needed; element filtering only available through new interfaces
                throw new UnsupportedOperationException();
            }

        };
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
    protected boolean include(BeanPropertyWriter writer) {
        return true;
    }

    /**
     * Method called to determine whether property will be included
     * (if 'true' returned) or filtered out (if 'false' returned)
     *
     * @since 2.3
     */
    protected boolean include(PropertyWriter writer) {
        return true;
    }

    /**
     * Method that defines what to do with container elements
     * (values contained in an array or {@link java.util.Collection}:
     * default implementation simply writes them out.
     *
     * @since 2.3
     */
    protected boolean includeElement(Object elementValue) {
        return true;
    }

    /*
    /**********************************************************
    /* BeanPropertyFilter (deprecated) implementation
    /**********************************************************
     */

    @Deprecated
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen,
            SerializerProvider provider, BeanPropertyWriter writer) throws Exception
    {
        if (include(writer)) {
            writer.serializeAsField(bean, jgen, provider);
        } else if (!jgen.canOmitFields()) { // since 2.3
            writer.serializeAsOmittedField(bean, jgen, provider);
        }
    }

    @Deprecated
    @Override
    public void depositSchemaProperty(BeanPropertyWriter writer,
            ObjectNode propertiesNode, SerializerProvider provider)
        throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(propertiesNode, provider);
        }
    }

    @Deprecated
    @Override
    public void depositSchemaProperty(BeanPropertyWriter writer,
            JsonObjectFormatVisitor objectVisitor, SerializerProvider provider)
        throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(objectVisitor, provider);
        }
    }

    /*
    /**********************************************************
    /* PropertyFilter implementation
    /**********************************************************
     */

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen,
            SerializerProvider provider, PropertyWriter writer)
        throws Exception
    {
        if (include(writer)) {
            writer.serializeAsField(pojo, jgen, provider);
        } else if (!jgen.canOmitFields()) { // since 2.3
            writer.serializeAsOmittedField(pojo, jgen, provider);
        }
    }

    @Override
    public void serializeAsElement(Object elementValue, JsonGenerator jgen, SerializerProvider provider,
            PropertyWriter writer)
        throws Exception
    {
        if (includeElement(elementValue)) {
            writer.serializeAsElement(elementValue, jgen, provider);
        }
    }

    @Deprecated
    @Override
    public void depositSchemaProperty(PropertyWriter writer,
            ObjectNode propertiesNode, SerializerProvider provider)
            throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(propertiesNode, provider);
        }
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer,
            JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider) throws JsonMappingException
    {
        if (include(writer)) {
            writer.depositSchemaProperty(objectVisitor, provider);
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

        @Override
        protected boolean include(PropertyWriter writer) {
            return _propertiesToInclude.contains(writer.getName());
        }
    }

    /**
     * Filter implementation which defaults to serializing all
     * properties, except for ones explicitly listed to be filtered out.
     */
    public static class SerializeExceptFilter
        extends SimpleBeanPropertyFilter
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        final static SerializeExceptFilter INCLUDE_ALL = new SerializeExceptFilter();

        /**
         * Set of property names to filter out.
         */
        protected final Set<String> _propertiesToExclude;

        SerializeExceptFilter() {
            _propertiesToExclude = Collections.emptySet();
        }

        public SerializeExceptFilter(Set<String> properties) {
            _propertiesToExclude = properties;
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return !_propertiesToExclude.contains(writer.getName());
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return !_propertiesToExclude.contains(writer.getName());
        }
    }
}