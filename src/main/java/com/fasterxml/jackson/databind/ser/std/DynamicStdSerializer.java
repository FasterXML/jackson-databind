package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Base class for standard serializers that are not (necessarily) container types
 * but that similarly handle content that may vary in ways to require dynamic lookups.
 * Typically these are referential or delegating types.
 *
 * @since 3.0
 */
public abstract class DynamicStdSerializer<T>
    extends StdSerializer<T>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Property for which this serializer is being used, if known at this point
     * (`null` for root value serializers as well as those cached as blueprints).
     */
    protected final BeanProperty _property;

    /**
     * Eagerly fetched serializer for actual value contained or referenced,
     * if fetched (based on {@link #_valueType} being non-parameterized and
     * either final, or type handling defined as static).
     */
    protected final JsonSerializer<Object> _valueSerializer;

    /**
     * If value type cannot be statically determined, mapping from
     * runtime value types to serializers are stored in this object.
     *
     * @since 3.0 (in 2.x subtypes contained it)
     */
    protected PropertySerializerMap _dynamicValueSerializers;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    @SuppressWarnings("unchecked")
    protected DynamicStdSerializer(JavaType type,
            BeanProperty prop, JsonSerializer<?> valueSer)
    {
        super(type);
        _property = prop;
        _valueSerializer = (JsonSerializer<Object>) valueSer;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    @SuppressWarnings("unchecked")
    protected DynamicStdSerializer(DynamicStdSerializer<?> src,
            BeanProperty prop, JsonSerializer<?> valueSer)
    {
        super(src);
        _property = prop;
        _valueSerializer = (JsonSerializer<Object>) valueSer;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    /*
    /**********************************************************************
    /* Helper methods for locating, caching element/value serializers
    /**********************************************************************
     */

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            JavaType type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }
}
