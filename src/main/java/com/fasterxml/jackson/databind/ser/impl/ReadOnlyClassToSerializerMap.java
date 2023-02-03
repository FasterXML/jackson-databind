package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.TypeKey;

/**
 * Optimized lookup table for accessing two types of serializers; typed
 * and non-typed. Only accessed from a single thread, so no synchronization
 * needed for accessors.
 *<p>
 * Note that before 2.6 this class was much smaller, and referred most
 * operations to separate <code>JsonSerializerMap</code>, but in 2.6
 * functions were combined.
 */
public final class ReadOnlyClassToSerializerMap
{
    private final Bucket[] _buckets;

    private final int _size;

    private final int _mask;

    public ReadOnlyClassToSerializerMap(LRUMap<TypeKey,JsonSerializer<Object>> src)
    {
        _size = findSize(src.size());
        _mask = (_size-1);
        Bucket[] buckets = new Bucket[_size];
        src.contents((key, value) -> {
            int index = key.hashCode() & _mask;
            buckets[index] = new Bucket(buckets[index], key, value);
        });
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

    /**
     * Factory method for constructing an instance.
     */
    public static ReadOnlyClassToSerializerMap from(LRUMap<TypeKey, JsonSerializer<Object>> src) {
        return new ReadOnlyClassToSerializerMap(src);
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int size() { return _size; }

    public JsonSerializer<Object> typedValueSerializer(JavaType type)
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

    public JsonSerializer<Object> typedValueSerializer(Class<?> type)
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

    public JsonSerializer<Object> untypedValueSerializer(JavaType type)
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

    public JsonSerializer<Object> untypedValueSerializer(Class<?> type)
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
    /* Helper classes
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
