package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Intermediate base class for serializers used for serializing
 * types that contain element(s) of other types, such as arrays,
 * {@link java.util.Collection}s (<code>Lists</code>, <code>Sets</code>
 * etc) and {@link java.util.Map}s and iterable things
 * ({@link java.util.Iterator}s).
 */
public abstract class ContainerSerializer<T>
    extends StdSerializer<T>
{
    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    protected ContainerSerializer(Class<T> t) {
        super(t);
    }
    
    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     * 
     * @param t
     */
    protected ContainerSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
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
    /**********************************************************
    /* Extended API
    /**********************************************************
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
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */
    
    /* Overridden as abstract, to force re-implementation; necessary for all
     * collection types.
     */
    @Override
    public abstract boolean isEmpty(T value);

    /**
     * Method that needs to be implemented to allow construction of a new
     * serializer object with given {@link TypeSerializer}, used when
     * addition type information is to be embedded.
     */
    protected abstract ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts);
}
