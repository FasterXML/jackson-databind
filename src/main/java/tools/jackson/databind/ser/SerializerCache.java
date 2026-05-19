package tools.jackson.databind.ser;

import java.io.Serial;
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
 *<p>
 * To prevent threads from observing partially-resolved serializers during
 * {@link #addAndResolveNonTypedSerializer} calls, a two-phase write protocol is used:
 * newly constructed serializers are placed into {@code _inProgressMap} first, resolved
 * there (so cyclic POJO lookups can find the in-progress entry), and only moved to
 * {@code _sharedMap} after {@code resolve()} completes. Each entry is removed from
 * {@code _inProgressMap} as soon as it is promoted, so the map tends to stay empty
 * at steady state.
 * The lock-free read path ({@link #untypedValueSerializer}) reads only from
 * {@code _sharedMap}, which therefore never contains a partially-resolved serializer.
 */
public final class SerializerCache
    implements Snapshottable<SerializerCache>,
        java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * By default, allow caching of up to 4000 serializer entries (for possibly up to
     * 1000 types; but depending access patterns may be as few as half of that).
     */
    public final static int DEFAULT_MAX_CACHE_SIZE = 4000;

    /**
     * Shared, modifiable map; contains only <em>fully resolved</em> serializers.
     * Used if local read-only copy does not contain serializer caller expects.
     * Because entries are inserted only after {@code resolve()} completes, the
     * read path ({@link #untypedValueSerializer}) can access this map without
     * holding any lock and is guaranteed never to see a partially-resolved serializer.
     *<p>
     * NOTE: keys are of various types (see below for key types), in addition to
     * basic {@link JavaType} used for "untyped" serializers.
     */
    private final LookupCache<TypeKey, ValueSerializer<Object>> _sharedMap;

    /**
     * Transient staging map that holds serializers that are currently being
     * resolved ({@code resolve()} has been called but has not yet returned).
     * Entries here are individually removed and promoted to {@code _sharedMap}
     * once their resolution completes, so this map tends to stay empty at
     * steady state.
     *<p>
     * A plain {@link HashMap} suffices here because access is always guarded by
     * {@code synchronized (this)} and only a tiny number of entries are ever
     * present at once (no LRU eviction or concurrent-access overhead needed).
     *
     * @since 3.2
     */
    private final transient HashMap<TypeKey, ValueSerializer<Object>> _inProgressMap;

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private final transient AtomicReference<ReadOnlyClassToSerializerMap> _readOnlyMap;

    /**
     * Separate lock for {@link #_makeReadOnlyLookupMap()} so that rebuilding
     * the read-only snapshot does not compete with the main monitor used by
     * {@link #addAndResolveNonTypedSerializer} during {@code resolve()}.
     * This avoids blocking read threads behind potentially long serializer
     * resolution during warmup.
     *
     * @since 3.2
     */
    private final transient Object _readOnlyMapLock = new Object();

    public SerializerCache() {
        this(DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * @since 3.0
     */
    public SerializerCache(int maxCached) {
        int initial = Math.min(64, maxCached>>2);
        _sharedMap = new SimpleLookupCache<>(initial, maxCached);
        _inProgressMap = new HashMap<>();
        _readOnlyMap = new AtomicReference<>();
    }

    public SerializerCache(LookupCache<TypeKey, ValueSerializer<Object>> cache) {
        _sharedMap = cache;
        _inProgressMap = new HashMap<>();
        _readOnlyMap = new AtomicReference<>();
    }

    protected SerializerCache(SimpleLookupCache<TypeKey, ValueSerializer<Object>> shared) {
        _sharedMap = shared;
        _inProgressMap = new HashMap<>();
        _readOnlyMap = new AtomicReference<>();
    }

    // Since 3.0, needed to initialize cache properly: shared map would be ok but need to
    // reconstruct AtomicReference
    @Serial
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

    private ReadOnlyClassToSerializerMap _makeReadOnlyLookupMap() {
        // Use a dedicated lock so that rebuilding the read-only snapshot
        // does not block behind a long resolve() in addAndResolveNonTypedSerializer.
        // _sharedMap is thread-safe (PrivateMaxEntriesMap) so iterating it
        // concurrently with put() is safe (weakly consistent).
        synchronized (_readOnlyMapLock) {
            ReadOnlyClassToSerializerMap m = _readOnlyMap.get();
            if (m == null) {
                m = ReadOnlyClassToSerializerMap.from(this, _sharedMap);
                _readOnlyMap.set(m);
            }
            return m;
        }
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
     * Returns the fully-resolved untyped serializer for the given type, or {@code null}
     * if not yet cached. Reads from {@code _sharedMap} which only contains fully-resolved
     * entries, so no lock is needed.
     *<p>
     * During cyclic POJO resolution the resolving thread may re-enter this method before
     * the in-progress serializer has been promoted to {@code _sharedMap}. In that case,
     * because the calling thread already holds the monitor (via
     * {@code synchronized (this)} in {@link #addAndResolveNonTypedSerializer}),
     * {@link Thread#holdsLock} is {@code true} and we fall back to {@code _inProgressMap}
     * to return the partially-resolved serializer to break the cycle.  All other threads
     * never hold the monitor and therefore exclusively see fully-resolved entries.
     */
    public ValueSerializer<Object> untypedValueSerializer(Class<?> type)
    {
        TypeKey key = new TypeKey(type, false);
        ValueSerializer<Object> ser = _sharedMap.get(key);
        if (ser == null && Thread.holdsLock(this)) {
            ser = _inProgressMap.get(key);
        }
        return ser;
    }

    /**
     * Returns the fully-resolved untyped serializer for the given type, or {@code null}
     * if not yet cached. Reads from {@code _sharedMap} which only contains fully-resolved
     * entries, so no lock is needed.
     *<p>
     * During cyclic POJO resolution the resolving thread may re-enter this method before
     * the in-progress serializer has been promoted to {@code _sharedMap}. In that case,
     * because the calling thread already holds the monitor (via
     * {@code synchronized (this)} in {@link #addAndResolveNonTypedSerializer}),
     * {@link Thread#holdsLock} is {@code true} and we fall back to {@code _inProgressMap}
     * to return the partially-resolved serializer to break the cycle.  All other threads
     * never hold the monitor and therefore exclusively see fully-resolved entries.
     */
    public ValueSerializer<Object> untypedValueSerializer(JavaType type)
    {
        TypeKey key = new TypeKey(type, false);
        ValueSerializer<Object> ser = _sharedMap.get(key);
        if (ser == null && Thread.holdsLock(this)) {
            ser = _inProgressMap.get(key);
        }
        return ser;
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * a typed serializer for given type.
     */
    public ValueSerializer<Object> typedValueSerializer(JavaType type)
    {
        return _sharedMap.get(new TypeKey(type, true));
    }

    /**
     * Method that checks if the shared (and hence, synchronized) lookup Map might have
     * a typed serializer for given type.
     */
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
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            // Stage in _inProgressMap so cyclic-resolution re-entrant lookups can find it
            _inProgressMap.put(key, ser);
            try {
                // Need resolution to handle cyclic POJO type dependencies
                /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
                 *   this because while we do need to register instance first, we also must
                 *   keep lock until resolution is complete.
                 */
                ser.resolve(ctxt);
                // Resolution complete: promote to the main (fully-resolved) map
                _sharedMap.put(key, ser);
                _readOnlyMap.set(null);
            } finally {
                // Clean up staging map so _inProgressMap stays empty at steady state
                _inProgressMap.remove(key);
            }
        }
    }

    public void addAndResolveNonTypedSerializer(JavaType type, ValueSerializer<Object> ser,
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            _inProgressMap.put(key, ser);
            try {
                // Need resolution to handle cyclic POJO type dependencies
                /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
                 *   this because while we do need to register instance first, we also must
                 *   keep lock until resolution is complete.
                 */
                ser.resolve(ctxt);
                _sharedMap.put(key, ser);
                _readOnlyMap.set(null);
            } finally {
                _inProgressMap.remove(key);
            }
        }
    }

    /**
     * Another alternative that will cover both access via raw type and matching
     * fully resolved type, in one fell swoop.
     */
    public void addAndResolveNonTypedSerializer(Class<?> rawType, JavaType fullType,
            ValueSerializer<Object> ser,
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey keyRaw = new TypeKey(rawType, false);
            TypeKey keyFull = new TypeKey(fullType, false);
            _inProgressMap.put(keyRaw, ser);
            _inProgressMap.put(keyFull, ser);
            try {
                ser.resolve(ctxt);
                _sharedMap.put(keyRaw, ser);
                _sharedMap.put(keyFull, ser);
                _readOnlyMap.set(null);
            } finally {
                _inProgressMap.remove(keyRaw);
                _inProgressMap.remove(keyFull);
            }
        }
    }

    /**
     * Method called by StdSerializationContext#flushCachedSerializers() to
     * clear all cached serializers
     */
    public synchronized void flush() {
        _sharedMap.clear();
        _inProgressMap.clear();
        _readOnlyMap.set(null);
    }
}
