package tools.jackson.databind.ser.impl;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.SerializerCache;
import tools.jackson.databind.util.SimpleLookupCache;
import tools.jackson.databind.util.TypeKey;

/**
 * Optimized lookup table for accessing two types of serializers; typed
 * and non-typed. Only accessed from a single thread, so no synchronization
 * needed for accessors.
 */
public final class ReadOnlyClassToSerializerMap
{
    /**
     * Shared cache used for call-throughs in cases where we do not have local matches.
     *
     * @since 3.0
     */
    private final SerializerCache _sharedCache;

    private final Bucket[] _buckets;

    private final int _size;

    private final int _mask;

    protected ReadOnlyClassToSerializerMap(SerializerCache shared,
            SimpleLookupCache<TypeKey, ValueSerializer<Object>> src)
    {
        _sharedCache = shared;
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
    public static ReadOnlyClassToSerializerMap from(SerializerCache shared,
            SimpleLookupCache<TypeKey, ValueSerializer<Object>> src) {
        return new ReadOnlyClassToSerializerMap(shared, src);
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public int size() { return _size; }
    
    public ValueSerializer<Object> typedValueSerializer(JavaType type)
    {
        Bucket bucket = _buckets[TypeKey.typedHash(type) & _mask];
        if (bucket != null) {
            if (bucket.matchesTyped(type)) {
                return bucket.value;
            }
            while ((bucket = bucket.next) != null) {
                if (bucket.matchesTyped(type)) {
                    return bucket.value;
                }
            }
        }
        return _sharedCache.typedValueSerializer(type);
    }

    public ValueSerializer<Object> typedValueSerializer(Class<?> rawType)
    {
        Bucket bucket = _buckets[TypeKey.typedHash(rawType) & _mask];
        if (bucket != null) {
            if (bucket.matchesTyped(rawType)) {
                return bucket.value;
            }
            while ((bucket = bucket.next) != null) {
                if (bucket.matchesTyped(rawType)) {
                    return bucket.value;
                }
            }
        }
        return _sharedCache.typedValueSerializer(rawType);
    }

    public ValueSerializer<Object> untypedValueSerializer(JavaType type)
    {
        Bucket bucket = _buckets[TypeKey.untypedHash(type) & _mask];
        if (bucket != null) {
            if (bucket.matchesUntyped(type)) {
                return bucket.value;
            }
            while ((bucket = bucket.next) != null) {
                if (bucket.matchesUntyped(type)) {
                    return bucket.value;
                }
            }
        }
        return _sharedCache.untypedValueSerializer(type);
    }

    public ValueSerializer<Object> untypedValueSerializer(Class<?> rawType)
    {
        Bucket bucket = _buckets[TypeKey.untypedHash(rawType) & _mask];
        if (bucket != null) {
            if (bucket.matchesUntyped(rawType)) {
                return bucket.value;
            }
            while ((bucket = bucket.next) != null) {
                if (bucket.matchesUntyped(rawType)) {
                    return bucket.value;
                }
            }
        }
        return _sharedCache.untypedValueSerializer(rawType);
    }    

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    private final static class Bucket
    {
        public final ValueSerializer<Object> value;
        public final Bucket next;

        protected final Class<?> _class;
        protected final JavaType _type;

        protected final boolean _isTyped;
        
        public Bucket(Bucket next, TypeKey key, ValueSerializer<Object> value)
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
