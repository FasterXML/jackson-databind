package com.fasterxml.jackson.databind.ser;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Intermediate base class for serializers used for serializing
 * types that contain element(s) of other types, such as arrays,
 * {@link java.util.Collection}s (<code>Lists</code>, <code>Sets</code>
 * etc) and {@link java.util.Map}s and iterable things
 * ({@link java.util.Iterator}s).
 */
@SuppressWarnings("serial")
public abstract class ContainerSerializer<T>
    extends StdSerializer<T>
{
    /**
     * Property that contains values handled by this serializer, if known; `null`
     * for root value serializers (ones directly called by {@link ObjectMapper} and
     * {@link ObjectWriter}).
     *
     * @since 3.0
     */
    protected final BeanProperty _property;

    /**
     * If value type cannot be statically determined, mapping from
     * runtime value types to serializers are stored in this object.
     *
     * @since 3.0 (in 2.x subtypes contained it)
     */
    protected PropertySerializerMap _dynamicValueSerializers;
    
    /*
    /**********************************************************************
    /* Construction, initialization
    /**********************************************************************
     */

    protected ContainerSerializer(Class<T> t) {
        super(t);
        _property = null;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    protected ContainerSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
        _property = null;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    protected ContainerSerializer(JavaType fullType, BeanProperty prop) {
        super(fullType);
        _property = prop;
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    protected ContainerSerializer(ContainerSerializer<?> src) {
        this(src, src._property);
    }

    protected ContainerSerializer(ContainerSerializer<?> src, BeanProperty prop) {
        super(src._handledType, false);
        _property = prop;
        // 16-Apr-2018, tatu: Could retain, possibly, in some cases... but may be
        //    dangerous as general practice so reset
        _dynamicValueSerializers = PropertySerializerMap.emptyForProperties();
    }

    /**
     * Factory(-like) method that can be used to construct a new container
     * serializer that uses specified {@link TypeSerializer} for decorating
     * contained values with additional type information.
     * 
     * @param vts Type serializer to use for contained values; can be null,
     *    in which case 'this' serializer is returned as is
     * @return Serializer instance that uses given type serializer for values if
     *    that is possible (or if not, just 'this' serializer)
     */
    public ContainerSerializer<?> withValueTypeSerializer(TypeSerializer vts) {
        if (vts == null) return this;
        return _withValueTypeSerializer(vts);
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Accessor for finding declared (static) element type for
     * type this serializer is used for.
     */
    public abstract JavaType getContentType();

    /**
     * Accessor for serializer used for serializing contents
     * (List and array elements, Map values etc) of the
     * container for which this serializer is used, if it is
     * known statically.
     * Note that for dynamic types this may return null; if so,
     * caller has to instead use {@link #getContentType()} and
     * {@link com.fasterxml.jackson.databind.SerializerProvider#findValueSerializer}.
     */
    public abstract JsonSerializer<?> getContentSerializer();

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************************
     */

    @Override
    public abstract boolean isEmpty(SerializerProvider prov, T value) throws IOException;

    /**
     * Method called to determine if the given value (of type handled by
     * this serializer) contains exactly one element.
     *<p>
     * Note: although it might seem sensible to instead define something
     * like "getElementCount()" method, this would not work well for
     * containers that do not keep track of size (like linked lists may
     * not).
     *<p>
     * Note, too, that this method is only called by serializer
     * itself; and specifically is not used for non-array/collection types
     * like <code>Map</code> or <code>Map.Entry</code> instances.
     */
    public abstract boolean hasSingleElement(T value);

    /*
    /**********************************************************************
    /* Helper methods for locating, caching element/value serializers
    /**********************************************************************
     */
    
    /**
     * Method that needs to be implemented to allow construction of a new
     * serializer object with given {@link TypeSerializer}, used when
     * addition type information is to be embedded.
     */
    protected abstract ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts);

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
