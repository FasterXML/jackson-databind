package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Base class for standard serializers that are not (necessarily) container types
 * but that similarly handle content that may vary in ways to require dynamic lookups.
 * Typically these are referential or delegating types.
 *
 * @since 3.0
 */
public abstract class StdDynamicSerializer<T>
    extends StdSerializer<T>
{
    /**
     * Property for which this serializer is being used, if known at this point
     * (`null` for root value serializers as well as those cached as blueprints).
     */
    protected final BeanProperty _property;

    /**
     * Type serializer used for values, if any: used for serializing values of
     * polymorphic types.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Eagerly fetched serializer for actual value contained or referenced,
     * if fetched.
     */
    protected final ValueSerializer<Object> _valueSerializer;

    /**
     * If value type cannot be statically determined, mapping from
     * runtime value types to serializers are stored in this object.
     *
     * @since 3.0 (in 2.x subtypes contained it)
     */
    protected PropertySerializerMap _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    protected StdDynamicSerializer(JavaType type, BeanProperty prop,
            TypeSerializer vts, ValueSerializer<?> valueSer)
    {
        super(type);
        _property = prop;
        _valueTypeSerializer = vts;
        _valueSerializer = (ValueSerializer<Object>) valueSer;
    }

    protected StdDynamicSerializer(StdDynamicSerializer<?> src, BeanProperty prop)
    {
        super(src);
        _property = prop;
        _valueTypeSerializer = src._valueTypeSerializer;
        _valueSerializer = src._valueSerializer;
    }

    @SuppressWarnings("unchecked")
    protected StdDynamicSerializer(StdDynamicSerializer<?> src,
            BeanProperty prop, TypeSerializer vts, ValueSerializer<?> valueSer)
    {
        super(src);
        _property = prop;
        _valueTypeSerializer = vts;
        _valueSerializer = (ValueSerializer<Object>) valueSer;
    }

    /*
    /**********************************************************************
    /* Helper methods for locating, caching element/value serializers
    /**********************************************************************
     */

    protected final ValueSerializer<Object> _findAndAddDynamic(SerializerProvider ctxt, Class<?> type)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final ValueSerializer<Object> _findAndAddDynamic(SerializerProvider ctxt, JavaType type)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }
}
