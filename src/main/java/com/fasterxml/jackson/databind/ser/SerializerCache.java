package com.fasterxml.jackson.databind.ser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;

/**
 * Simple cache object that allows for doing 2-level lookups: first level is
 * by "local" read-only lookup Map (used without locking)
 * and second backup level is by a shared modifiable HashMap.
 * The idea is that after a while, most serializers are found from the
 * local Map (to optimize performance, reduce lock contention),
 * but that during buildup we can use a shared map to reduce both
 * number of distinct read-only maps constructed, and number of
 * serializers constructed.
 *<p>
 * Cache contains three kinds of entries,
 * based on combination of class pair key. First class in key is for the
 * type to serialize, and second one is type used for determining how
 * to resolve value type. One (but not both) of entries can be null.
 */
public final class SerializerCache
{
    /**
     * Shared, modifiable map; all access needs to be through synchronized blocks.
     *<p>
     * NOTE: keys are of various types (see below for key types), in addition to
     * basic {@link JavaType} used for "untyped" serializers.
     */
    private final HashMap<com.fasterxml.jackson.databind.util.TypeKey, JsonSerializer<Object>> _sharedMap
        = new HashMap<com.fasterxml.jackson.databind.util.TypeKey, JsonSerializer<Object>>(64);

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private final AtomicReference<ReadOnlyClassToSerializerMap> _readOnlyMap
        = new AtomicReference<ReadOnlyClassToSerializerMap>();

    public SerializerCache() { }

    /**
     * Method that can be called to get a read-only instance populated from the
     * most recent version of the shared lookup Map.
     */
    public ReadOnlyClassToSerializerMap getReadOnlyLookupMap()
    {
        ReadOnlyClassToSerializerMap m = _readOnlyMap.get();
        if (m != null) {
            return m;
        }
        return _makeReadOnlyLookupMap();
    }

    private final synchronized ReadOnlyClassToSerializerMap _makeReadOnlyLookupMap() {
        // double-locking; safe, but is it really needed? Not doing that is only a perf problem,
        // not correctness
        ReadOnlyClassToSerializerMap m = _readOnlyMap.get();
        if (m == null) {
            m = ReadOnlyClassToSerializerMap.from(_sharedMap);
            _readOnlyMap.set(m);
        }
        return m;
    }

    /*
    /**********************************************************
    /* Lookup methods for accessing shared (slow) cache
    /**********************************************************
     */

    public synchronized int size() {
        return _sharedMap.size();
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * untyped serializer for given type.
     */
    public JsonSerializer<Object> untypedValueSerializer(Class<?> type)
    {
        synchronized (this) {
            return _sharedMap.get(new TypeKey(type, false));
        }
    }

    public JsonSerializer<Object> untypedValueSerializer(JavaType type)
    {
        synchronized (this) {
            return _sharedMap.get(new TypeKey(type, false));
        }
    }

    public JsonSerializer<Object> typedValueSerializer(JavaType type)
    {
        synchronized (this) {
            return _sharedMap.get(new TypeKey(type, true));
        }
    }

    public JsonSerializer<Object> typedValueSerializer(Class<?> cls)
    {
        synchronized (this) {
            return _sharedMap.get(new TypeKey(cls, true));
        }
    }

    /*
    /**********************************************************
    /* Methods for adding shared serializer instances
    /**********************************************************
     */
    
    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a serializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addTypedSerializer(JavaType type, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(type, true), ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap.set(null);
            }
        }
    }

    public void addTypedSerializer(Class<?> cls, JsonSerializer<Object> ser)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(cls, true), ser) == null) {
                // let's invalidate the read-only copy, too, to get it updated
                _readOnlyMap.set(null);
            }
        }
    }
    
    public void addAndResolveNonTypedSerializer(Class<?> type, JsonSerializer<Object> ser,
            SerializerProvider provider)
        throws JsonMappingException
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(type, false), ser) == null) {
                _readOnlyMap.set(null);
            }
            /* Finally: some serializers want to do post-processing, after
             * getting registered (to handle cyclic deps).
             */
            /* 14-May-2011, tatu: As per [JACKSON-570], resolving needs to be done
             *   in synchronized manner; this because while we do need to register
             *   instance first, we also must keep lock until resolution is complete
             */
            if (ser instanceof ResolvableSerializer) {
                ((ResolvableSerializer) ser).resolve(provider);
            }
        }
    }

    public void addAndResolveNonTypedSerializer(JavaType type, JsonSerializer<Object> ser,
            SerializerProvider provider)
        throws JsonMappingException
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(type, false), ser) == null) {
                _readOnlyMap.set(null);
            }
            /* Finally: some serializers want to do post-processing, after
             * getting registered (to handle cyclic deps).
             */
            /* 14-May-2011, tatu: As per [JACKSON-570], resolving needs to be done
             *   in synchronized manner; this because while we do need to register
             *   instance first, we also must keep lock until resolution is complete
             */
            if (ser instanceof ResolvableSerializer) {
                ((ResolvableSerializer) ser).resolve(provider);
            }
        }
    }

    /**
     * Method called by StdSerializerProvider#flushCachedSerializers() to
     * clear all cached serializers
     */
    public synchronized void flush() {
        _sharedMap.clear();
    }

    /*
    /**************************************************************
    /* Helper class(es)
    /**************************************************************
     */

    /**
     * @deprecated Since 2.6; replaced by {@link com.fasterxml.jackson.databind.util.TypeKey}
     */
    @Deprecated
    public final static class TypeKey extends com.fasterxml.jackson.databind.util.TypeKey
    {
        public TypeKey(Class<?> key, boolean typed) {
            super(key, typed);
        }

        public TypeKey(JavaType key, boolean typed) {
            super(key, typed);
        }
    }
}
