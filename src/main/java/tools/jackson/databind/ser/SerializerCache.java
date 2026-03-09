package tools.jackson.databind.ser;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.*;
import tools.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
import tools.jackson.databind.util.LookupCache;
import tools.jackson.databind.util.SimpleLookupCache;
import tools.jackson.databind.util.TypeKey;

/**
 * Simple cache object that allows for doing 2-level lookups: first level is
 * by "local" read-only lookup Map (used without locking) and second backup
 * level is by a shared modifiable HashMap. The idea is that after a while,
 * most serializers are found from the local Map (to optimize performance,
 * reduce lock contention), but that during buildup we can use a shared map
 * to reduce both number of distinct read-only maps constructed, and number
 * of serializers constructed.
 *<p>
 * Cache contains three kinds of entries, based on combination of class pair key.
 * First class in key is for the type to serialize, and second one is type used for
 * determining how to resolve value type. One (but not both) of entries can be null.
 */
public final class SerializerCache
    implements Snapshottable<SerializerCache>,
        java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * By default, allow caching of up to 4000 serializer entries (for possibly up to
     * 1000 types; but depending access patterns may be as few as half of that).
     */
    public final static int DEFAULT_MAX_CACHE_SIZE = 4000;

    /**
     * Shared, modifiable map; used if local read-only copy does not contain serializer
     * caller expects.
     *<p>
     * NOTE: keys are of various types (see below for key types), in addition to
     * basic {@link JavaType} used for "untyped" serializers.
     */
    private final LookupCache<TypeKey, ValueSerializer<Object>> _sharedMap;

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private final transient AtomicReference<ReadOnlyClassToSerializerMap> _readOnlyMap;

    /**
     * During serializer construction we may need to keep track of partially
     * completed serializers, to resolve cyclic dependencies. This is the
     * map used for storing serializers before they are fully complete.
     * Only accessed within {@code synchronized(this)} blocks.
     *<p>
     * Follows the same pattern as {@code DeserializerCache._incompleteDeserializers}:
     * serializers are placed here during {@code resolve()}, then moved to
     * {@link #_sharedMap} only after resolution is complete. This ensures that
     * concurrent readers of {@link #_sharedMap} never see partially-resolved
     * serializers.
     *
     * @since 3.2
     */
    private final transient HashMap<TypeKey, ValueSerializer<Object>> _incompleteSerializers
        = new HashMap<>(8);

    public SerializerCache() {
        this(DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * @since 3.0
     */
    public SerializerCache(int maxCached) {
        int initial = Math.min(64, maxCached>>2);
        _sharedMap = new SimpleLookupCache<TypeKey, ValueSerializer<Object>>(initial, maxCached);
        _readOnlyMap = new AtomicReference<>();
    }

    public SerializerCache(LookupCache<TypeKey, ValueSerializer<Object>> cache) {
        _sharedMap = cache;
        _readOnlyMap = new AtomicReference<>();
    }

    protected SerializerCache(SimpleLookupCache<TypeKey, ValueSerializer<Object>> shared) {
        _sharedMap = shared;
        _readOnlyMap = new AtomicReference<ReadOnlyClassToSerializerMap>();
    }

    // Since 3.0, needed to initialize cache properly: shared map would be ok but need to
    // reconstruct AtomicReference
    protected Object readResolve() {
        return new SerializerCache(_sharedMap);
    }

    @Override
    public SerializerCache snapshot() {
        return new SerializerCache(_sharedMap.snapshot());
    }

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
            m = ReadOnlyClassToSerializerMap.from(this, _sharedMap);
            _readOnlyMap.set(m);
        }
        return m;
    }

    /*
    /**********************************************************************
    /* Lookup methods for accessing shared (slow) cache
    /**********************************************************************
     */

    public int size() {
        return _sharedMap.size();
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * untyped serializer for given type.
     */
    public ValueSerializer<Object> untypedValueSerializer(Class<?> type)
    {
        return _sharedMap.get(new TypeKey(type, false));
    }

    public ValueSerializer<Object> untypedValueSerializer(JavaType type)
    {
        return _sharedMap.get(new TypeKey(type, false));
    }

    public ValueSerializer<Object> typedValueSerializer(JavaType type)
    {
        return _sharedMap.get(new TypeKey(type, true));
    }

    public ValueSerializer<Object> typedValueSerializer(Class<?> cls)
    {
        return _sharedMap.get(new TypeKey(cls, true));
    }

    /*
    /**********************************************************************
    /* Methods for adding shared serializer instances
    /**********************************************************************
     */

    /**
     * Method called if none of lookups succeeded, and caller had to construct
     * a serializer. If so, we will update the shared lookup map so that it
     * can be resolved via it next time.
     */
    public void addTypedSerializer(JavaType type, ValueSerializer<Object> ser)
    {
        if (_sharedMap.put(new TypeKey(type, true), ser) == null) {
            // let's invalidate the read-only copy, too, to get it updated
            _readOnlyMap.set(null);
        }
    }

    public void addTypedSerializer(Class<?> cls, ValueSerializer<Object> ser)
    {
        if (_sharedMap.put(new TypeKey(cls, true), ser) == null) {
            // let's invalidate the read-only copy, too, to get it updated
            _readOnlyMap.set(null);
        }
    }

    // [databind#5615]: All three addAndResolveNonTypedSerializer overloads follow
    // the same pattern as DeserializerCache._createAndCache2: serializers are placed
    // in _incompleteSerializers during resolve(), then moved to _sharedMap only after
    // resolution completes. This prevents concurrent readers from seeing
    // partially-resolved serializers.

    public ValueSerializer<Object> addAndResolveNonTypedSerializer(Class<?> type,
            ValueSerializer<Object> ser, SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            // Check for cyclic dependency: another call up the stack may already be resolving this type
            ValueSerializer<Object> existing = _incompleteSerializers.get(key);
            if (existing != null) {
                return existing;
            }
            // Or, another thread may have fully resolved it while we were creating ours
            existing = _sharedMap.get(key);
            if (existing != null) {
                return existing;
            }
            int incompleteCount = _incompleteSerializers.size();
            _incompleteSerializers.put(key, ser);
            try {
                ser.resolve(ctxt);
            } finally {
                _incompleteSerializers.remove(key);
                if (incompleteCount == 0 && !_incompleteSerializers.isEmpty()) {
                    _incompleteSerializers.clear();
                }
            }
            if (_sharedMap.put(key, ser) == null) {
                _readOnlyMap.set(null);
            }
            return ser;
        }
    }

    public ValueSerializer<Object> addAndResolveNonTypedSerializer(JavaType type,
            ValueSerializer<Object> ser, SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            ValueSerializer<Object> existing = _incompleteSerializers.get(key);
            if (existing != null) {
                return existing;
            }
            existing = _sharedMap.get(key);
            if (existing != null) {
                return existing;
            }
            int incompleteCount = _incompleteSerializers.size();
            _incompleteSerializers.put(key, ser);
            try {
                ser.resolve(ctxt);
            } finally {
                _incompleteSerializers.remove(key);
                if (incompleteCount == 0 && !_incompleteSerializers.isEmpty()) {
                    _incompleteSerializers.clear();
                }
            }
            if (_sharedMap.put(key, ser) == null) {
                _readOnlyMap.set(null);
            }
            return ser;
        }
    }

    /**
     * Another alternative that will cover both access via raw type and matching
     * fully resolved type, in one fell swoop.
     */
    public ValueSerializer<Object> addAndResolveNonTypedSerializer(Class<?> rawType, JavaType fullType,
            ValueSerializer<Object> ser,
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey rawKey = new TypeKey(rawType, false);
            TypeKey fullKey = new TypeKey(fullType, false);
            // Check for cyclic dependency (check both keys)
            ValueSerializer<Object> existing = _incompleteSerializers.get(fullKey);
            if (existing != null) {
                return existing;
            }
            existing = _incompleteSerializers.get(rawKey);
            if (existing != null) {
                return existing;
            }
            // Or, another thread may have fully resolved it
            existing = _sharedMap.get(fullKey);
            if (existing != null) {
                return existing;
            }
            int incompleteCount = _incompleteSerializers.size();
            _incompleteSerializers.put(rawKey, ser);
            _incompleteSerializers.put(fullKey, ser);
            try {
                ser.resolve(ctxt);
            } finally {
                _incompleteSerializers.remove(rawKey);
                _incompleteSerializers.remove(fullKey);
                if (incompleteCount == 0 && !_incompleteSerializers.isEmpty()) {
                    _incompleteSerializers.clear();
                }
            }
            Object ob1 = _sharedMap.put(rawKey, ser);
            Object ob2 = _sharedMap.put(fullKey, ser);
            if ((ob1 == null) || (ob2 == null)) {
                _readOnlyMap.set(null);
            }
            return ser;
        }
    }

    /**
     * Method called by StdSerializationContext#flushCachedSerializers() to
     * clear all cached serializers
     */
    public synchronized void flush() {
        _sharedMap.clear();
        _incompleteSerializers.clear();
        _readOnlyMap.set(null);
    }
}
