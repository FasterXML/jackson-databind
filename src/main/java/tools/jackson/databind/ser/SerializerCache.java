package tools.jackson.databind.ser;

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
 * {@code _sharedMap} after {@code resolve()} completes. {@code _inProgressMap} is then
 * cleared once the outermost resolution finishes, so it tends to stay empty at steady state.
 * The lock-free read path ({@link #untypedValueSerializer}) reads only from
 * {@code _sharedMap}, which therefore never contains a partially-resolved serializer.
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
     * Entries here are moved to {@code _sharedMap} once resolution completes.
     * The map tends to empty out as serializers finish resolving, and is
     * cleared entirely when the outermost {@link #addAndResolveNonTypedSerializer}
     * call returns.
     *<p>
     * Access to this map is always guarded by {@code synchronized (this)}.
     */
    private final transient LookupCache<TypeKey, ValueSerializer<Object>> _inProgressMap;

    /**
     * Tracks the nesting depth of active {@link #addAndResolveNonTypedSerializer}
     * calls on the current thread (re-entrant, since {@code synchronized} is
     * re-entrant in Java).  Used to determine when it is safe to clear
     * {@code _inProgressMap}: we clear only when depth returns to zero so that
     * nested cyclic-resolution calls still find their in-progress entries.
     *<p>
     * Access is always guarded by {@code synchronized (this)}.
     */
    private transient int _resolveDepth;

    /**
     * Most recent read-only instance, created from _sharedMap, if any.
     */
    private final transient AtomicReference<ReadOnlyClassToSerializerMap> _readOnlyMap;

    public SerializerCache() {
        this(DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * @since 3.0
     */
    public SerializerCache(int maxCached) {
        int initial = Math.min(64, maxCached>>2);
        _sharedMap = new SimpleLookupCache<TypeKey, ValueSerializer<Object>>(initial, maxCached);
        _inProgressMap = _sharedMap.emptyCopy();
        _readOnlyMap = new AtomicReference<>();
    }

    public SerializerCache(LookupCache<TypeKey, ValueSerializer<Object>> cache) {
        _sharedMap = cache;
        _inProgressMap = cache.emptyCopy();
        _readOnlyMap = new AtomicReference<>();
    }

    protected SerializerCache(SimpleLookupCache<TypeKey, ValueSerializer<Object>> shared) {
        _sharedMap = shared;
        _inProgressMap = shared.emptyCopy();
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

    public ValueSerializer<Object> untypedValueSerializer(JavaType type)
    {
        TypeKey key = new TypeKey(type, false);
        ValueSerializer<Object> ser = _sharedMap.get(key);
        if (ser == null && Thread.holdsLock(this)) {
            ser = _inProgressMap.get(key);
        }
        return ser;
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
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            // Stage in _inProgressMap so cyclic-resolution re-entrant lookups can find it
            _inProgressMap.put(key, ser);
            _readOnlyMap.set(null);
            // Need resolution to handle cyclic POJO type dependencies
            /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
             *   this because while we do need to register instance first, we also must
             *   keep lock until resolution is complete.
             */
            _resolveDepth++;
            try {
                ser.resolve(ctxt);
                // Resolution complete: promote to the main (fully-resolved) map
                _sharedMap.put(key, ser);
            } finally {
                _resolveDepth--;
                if (_resolveDepth == 0) {
                    // Outermost resolution finished: clear the staging map so it
                    // tends to stay empty at steady state
                    _inProgressMap.clear();
                }
            }
        }
    }

    public void addAndResolveNonTypedSerializer(JavaType type, ValueSerializer<Object> ser,
            SerializationContext ctxt)
    {
        synchronized (this) {
            TypeKey key = new TypeKey(type, false);
            _inProgressMap.put(key, ser);
            _readOnlyMap.set(null);
            // Need resolution to handle cyclic POJO type dependencies
            /* 14-May-2011, tatu: Resolving needs to be done in synchronized manner;
             *   this because while we do need to register instance first, we also must
             *   keep lock until resolution is complete.
             */
            _resolveDepth++;
            try {
                ser.resolve(ctxt);
                _sharedMap.put(key, ser);
            } finally {
                _resolveDepth--;
                if (_resolveDepth == 0) {
                    _inProgressMap.clear();
                }
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
            _readOnlyMap.set(null);
            _resolveDepth++;
            try {
                ser.resolve(ctxt);
                _sharedMap.put(keyRaw, ser);
                _sharedMap.put(keyFull, ser);
            } finally {
                _resolveDepth--;
                if (_resolveDepth == 0) {
                    _inProgressMap.clear();
                }
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
