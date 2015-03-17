package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.util.TypeKey;

/**
 * Optimized lookup table for accessing two types of serializers; typed
 * and non-typed. Only accessed from a single thread, so no synchronization
 * needed for accessors.
 */
public final class ReadOnlyClassToSerializerMap
{
    /**
     * Actual mappings from type key to serializers
     */
    protected final JsonSerializerMap _map;

    private ReadOnlyClassToSerializerMap(JsonSerializerMap map) {
        super();
        _map = map;
    }

    public ReadOnlyClassToSerializerMap instance() {
        return new ReadOnlyClassToSerializerMap(_map);
    }

    /**
     * Factory method for creating the "blueprint" lookup map. Such map
     * can not be used as is but just shared: to get an actual usable
     * instance, {@link #instance} has to be called first.
     */
    public static ReadOnlyClassToSerializerMap from(HashMap<TypeKey, JsonSerializer<Object>> src) {
        return new ReadOnlyClassToSerializerMap(new JsonSerializerMap(src));
    }

    public JsonSerializer<Object> typedValueSerializer(JavaType type) { 
        return _map.findTyped(type);
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> cls) { 
        return _map.findTyped(cls);
    }

    public JsonSerializer<Object> untypedValueSerializer(JavaType type) { 
        return _map.findUntyped(type);
    }

    public JsonSerializer<Object> untypedValueSerializer(Class<?> cls) { 
        return _map.findUntyped(cls);
    }
}
