package tools.jackson.databind.ser.std;

import java.util.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.ser.*;

/**
 * Simple {@link PropertyFilter} implementation that only uses property name
 * to determine whether to serialize property as is, or to filter it out.
 *<p>
 * Use of this class as the base implementation for any custom
 * {@link PropertyFilter} implementations is strongly encouraged,
 * because it can provide default implementation for any methods that may
 * be added in {@link PropertyFilter} (as unfortunate as additions may be).
 */
public class SimpleBeanPropertyFilter
    implements PropertyFilter
        // sub-classes must also implement java.io.Serializable
{
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SimpleBeanPropertyFilter() { }

    @Override
    public PropertyFilter snapshot() {
        // 22-Feb-2018, tatu: Since instances are immutable (no API to change)
        //   we should be able to avoid actual copying, return instances as they are
        return this;
    }

    /**
     * Convenience factory method that will return a "no-op" filter that will
     * simply just serialize all properties that are given, and filter out
     * nothing.
     */
    public static SimpleBeanPropertyFilter serializeAll() {
        return SerializeExceptFilter.INCLUDE_ALL;
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

    /*
    /**********************************************************************
    /* Methods for sub-classes
    /**********************************************************************
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
     */
    protected boolean include(PropertyWriter writer) {
        return true;
    }

    /**
     * Method that defines what to do with container elements
     * (values contained in an array or {@link java.util.Collection}:
     * default implementation simply writes them out.
     */
    protected boolean includeElement(Object elementValue) {
        return true;
    }

    /*
    /**********************************************************************
    /* PropertyFilter implementation
    /**********************************************************************
     */

    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator g,
            SerializerProvider provider, PropertyWriter writer)
        throws Exception
    {
        if (include(writer)) {
            writer.serializeAsProperty(pojo, g, provider);
        } else if (!g.canOmitProperties()) {
            writer.serializeAsOmittedProperty(pojo, g, provider);
        }
    }

    @Override
    public void serializeAsElement(Object elementValue, JsonGenerator g, SerializerProvider provider,
            PropertyWriter writer)
        throws Exception
    {
        if (includeElement(elementValue)) {
            writer.serializeAsElement(elementValue, g, provider);
        }
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer,
            JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
    {
        if (include(writer)) {
            writer.depositSchemaProperty(objectVisitor, provider);
        }
    }

    /*
    /**********************************************************************
    /* Sub-classes
    /**********************************************************************
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