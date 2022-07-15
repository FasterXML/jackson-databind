package tools.jackson.databind.ser;

import java.util.concurrent.atomic.AtomicReference;

import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.*;
import tools.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
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
    public final static int DEFAULT_MAX_CACHED = 4000;

    /**
     * Shared, modifiable map; used if local read-only copy does not contain serializer
     * caller expects.
     *<p>
     * NOTE: keys are of various types (see below for key types), in addition to
     * basic {@link JavaType} used for "untyped" serializers.
     */
    private final SimpleLookupCache<TypeKey, ValueSerializer<Object>> _sharedMap;

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private final transient AtomicReference<ReadOnlyClassToSerializerMap> _readOnlyMap;

    public SerializerCache() {
        this(DEFAULT_MAX_CACHED);
    }

    /**
     * @since 3.0
     */
    public SerializerCache(int maxCached) {
        int initial = Math.min(64, maxCached>>2);
        _sharedMap = new SimpleLookupCache<TypeKey, ValueSerializer<Object>>(initial, maxCached);
        _readOnlyMap = new AtomicReference<ReadOnlyClassToSerializerMap>();
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

    public void addAndResolveNonTypedSerializer(Class<?> type, ValueSerializer<Object> ser,
            SerializerProvider provider)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(type, false), ser) == null) {
                _readOnlyMap.set(null);
            }
            // Need resolution to handle cyclic POJO type dependencies
            /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
             *   this because while we do need to register instance first, we also must
             *   keep lock until resolution is complete.
             */
            ser.resolve(provider);
        }
    }

    public void addAndResolveNonTypedSerializer(JavaType type, ValueSerializer<Object> ser,
            SerializerProvider provider)
    {
        synchronized (this) {
            if (_sharedMap.put(new TypeKey(type, false), ser) == null) {
                _readOnlyMap.set(null);
            }
            // Need resolution to handle cyclic POJO type dependencies
            /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
             *   this because while we do need to register instance first, we also must
             *   keep lock until resolution is complete.
             */
            ser.resolve(provider);
        }
    }

    /**
     * Another alternative that will cover both access via raw type and matching
     * fully resolved type, in one fell swoop.
     */
    public void addAndResolveNonTypedSerializer(Class<?> rawType, JavaType fullType,
            ValueSerializer<Object> ser,
            SerializerProvider provider)
    {
        synchronized (this) {
            Object ob1 = _sharedMap.put(new TypeKey(rawType, false), ser);
            Object ob2 = _sharedMap.put(new TypeKey(fullType, false), ser);
            if ((ob1 == null) || (ob2 == null)) {
                _readOnlyMap.set(null);
            }
            ser.resolve(provider);
        }
    }

    /**
     * Method called by StdSerializerProvider#flushCachedSerializers() to
     * clear all cached serializers
     */
    public synchronized void flush() {
        _sharedMap.clear();
    }
}
