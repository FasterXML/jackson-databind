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
    /* Yeah, pretty unclean from OOP perspective, but saves us from trouble of
     * allocating key instances. So we'll take that for this simple internal class.
     */
    extends TypeKey // since 2.6
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
        resetTyped(type);
        return _map.find(this);
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> cls) { 
        resetTyped(cls);
        return _map.find(this);
    }

    public JsonSerializer<Object> untypedValueSerializer(JavaType type) { 
        resetUntyped(type);
        return _map.find(this);
    }

    public JsonSerializer<Object> untypedValueSerializer(Class<?> cls) { 
        resetUntyped(cls);
        return _map.find(this);
    }
}
