package tools.jackson.databind.ser.std;

import java.util.function.UnaryOperator;

import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;

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

    /**
     * Helper method for finding serializer for value of given runtime type:
     * first checks for an already resolved serializer in {@link #_dynamicValueSerializers}
     * and, if none found, resolves one (using type specialized from {@code declaredType}
     * if it has generic types; otherwise {@code runtimeType} as is) and adds it.
     *
     * @param declaredType Declared (nominal) value type
     * @param runtimeType Actual runtime type of value
     *
     * @since 3.4
     */
    protected final ValueSerializer<Object> _findCachedOrAddDynamic(SerializationContext ctxt,
            JavaType declaredType, Class<?> runtimeType)
    {
        return _findCachedOrAddDynamic(ctxt, declaredType, runtimeType, UnaryOperator.identity());
    }

    /**
     * Variant of {@link #_findCachedOrAddDynamic(SerializationContext, JavaType, Class)}
     * that applies given transformer to newly resolved serializer before adding it.
     *
     * @since 3.4
     */
    protected final ValueSerializer<Object> _findCachedOrAddDynamic(SerializationContext ctxt,
            JavaType declaredType, Class<?> runtimeType,
            UnaryOperator<ValueSerializer<Object>> serTransformer)
    {
        ValueSerializer<Object> ser = _dynamicValueSerializers.serializerFor(runtimeType);
        if (ser == null) {
            if (declaredType.hasGenericTypes()) {
                ser = _findAndAddDynamic(ctxt, ctxt.constructSpecializedType(declaredType, runtimeType),
                        serTransformer);
            } else {
                ser = _findAndAddDynamic(ctxt, runtimeType, serTransformer);
            }
        }
        return ser;
    }

    /**
     * Method for resolving serializer for given type and adding it in
     * {@link #_dynamicValueSerializers}. NOTE: does NOT check for an already
     * resolved serializer first; callers should usually use
     * {@link #_findCachedOrAddDynamic} instead.
     */
    protected final ValueSerializer<Object> _findAndAddDynamic(SerializationContext ctxt, Class<?> type)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final ValueSerializer<Object> _findAndAddDynamic(SerializationContext ctxt, Class<?> type,
            UnaryOperator<ValueSerializer<Object>> serTransformer)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property, serTransformer);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final ValueSerializer<Object> _findAndAddDynamic(SerializationContext ctxt, JavaType type)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }

    protected final ValueSerializer<Object> _findAndAddDynamic(SerializationContext ctxt, JavaType type,
            UnaryOperator<ValueSerializer<Object>> serTransformer)
    {
        PropertySerializerMap map = _dynamicValueSerializers;
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type,
                ctxt, _property, serTransformer);
        if (map != result.map) {
            _dynamicValueSerializers = result.map;
        }
        return result.serializer;
    }
}
