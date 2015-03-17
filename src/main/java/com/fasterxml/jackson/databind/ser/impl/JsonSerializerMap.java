package com.fasterxml.jackson.databind.ser.impl;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.util.TypeKey;

/**
 * Specialized read-only map used for storing and accessing serializers by type.
 * Used for per-{@link com.fasterxml.jackson.databind.ObjectMapper} sharing
 * of resolved serializers; in addition, a per-call non-shared read/write
 * map may be needed, which will (after call) get merged to create a new
 * shared map of this type.
 */
public class JsonSerializerMap
{
    private final Bucket[] _buckets;

    private final int _size;

    private final int _mask;
    
    public JsonSerializerMap(Map<TypeKey,JsonSerializer<Object>> serializers)
    {
        int size = findSize(serializers.size());
        _size = size;
        _mask = (size-1);
        Bucket[] buckets = new Bucket[size];
        for (Map.Entry<TypeKey,JsonSerializer<Object>> entry : serializers.entrySet()) {
            TypeKey key = entry.getKey();
            int index = key.hashCode() & _mask;
            buckets[index] = new Bucket(buckets[index], key, entry.getValue());
        }
        _buckets = buckets;
    }
    
    private final static int findSize(int size)
    {
        // For small enough results (64 or less), we'll require <= 50% fill rate; otherwise 80%
        int needed = (size <= 64) ? (size + size) : (size + (size >> 2));
        int result = 8;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int size() { return _size; }
    
    public JsonSerializer<Object> findTyped(JavaType type)
    {
        Bucket bucket = _buckets[TypeKey.typedHash(type) & _mask];
        if (bucket == null) {
            return null;
        }
        if (bucket.matchesTyped(type)) {
            return bucket.value;
        }
        while ((bucket = bucket.next) != null) {
            if (bucket.matchesTyped(type)) {
                return bucket.value;
            }
        }
        return null;
    }

    public JsonSerializer<Object> findTyped(Class<?> type)
    {
        Bucket bucket = _buckets[TypeKey.typedHash(type) & _mask];
        if (bucket == null) {
            return null;
        }
        if (bucket.matchesTyped(type)) {
            return bucket.value;
        }
        while ((bucket = bucket.next) != null) {
            if (bucket.matchesTyped(type)) {
                return bucket.value;
            }
        }
        return null;
    }

    public JsonSerializer<Object> findUntyped(JavaType type)
    {
        Bucket bucket = _buckets[TypeKey.untypedHash(type) & _mask];
        if (bucket == null) {
            return null;
        }
        if (bucket.matchesUntyped(type)) {
            return bucket.value;
        }
        while ((bucket = bucket.next) != null) {
            if (bucket.matchesUntyped(type)) {
                return bucket.value;
            }
        }
        return null;
    }

    public JsonSerializer<Object> findUntyped(Class<?> type)
    {
        Bucket bucket = _buckets[TypeKey.untypedHash(type) & _mask];
        if (bucket == null) {
            return null;
        }
        if (bucket.matchesUntyped(type)) {
            return bucket.value;
        }
        while ((bucket = bucket.next) != null) {
            if (bucket.matchesUntyped(type)) {
                return bucket.value;
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    private final static class Bucket
    {
        public final JsonSerializer<Object> value;
        public final Bucket next;

        protected final Class<?> _class;
        protected final JavaType _type;

        protected final boolean _isTyped;
        
        public Bucket(Bucket next, TypeKey key, JsonSerializer<Object> value)
        {
            this.next = next;
            this.value = value;
            _isTyped = key.isTyped();
            _class = key.getRawType();
            _type = key.getType();
        }

        public boolean matchesTyped(Class<?> key) {
            return (_class == key) && _isTyped;
        }

        public boolean matchesUntyped(Class<?> key) {
            return (_class == key) && !_isTyped;
        }

        public boolean matchesTyped(JavaType key) {
            return _isTyped && key.equals(_type);
        }

        public boolean matchesUntyped(JavaType key) {
            return !_isTyped && key.equals(_type);
        }
    }
}
