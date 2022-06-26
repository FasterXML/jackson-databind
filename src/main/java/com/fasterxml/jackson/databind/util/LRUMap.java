package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Helper for simple bounded maps used for reusing lookup values.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the cache size of Map.
 *<p>
 * NOTE: since version 2.4.2, this is <b>NOT</b> an LRU-based at all; reason
 * being that it is not possible to use JDK components that do LRU _AND_ perform
 * well wrt synchronization on multi-core systems. So we choose efficient synchronization
 * over potentially more efficient handling of entries.
 *<p>
 * And yes, there are efficient LRU implementations such as
 * <a href="https://code.google.com/p/concurrentlinkedhashmap/">concurrentlinkedhashmap</a>;
 * but at this point we really try to keep external deps to minimum.
 * Plan from Jackson 2.12 is to focus more on pluggability as {@link LookupCache} and
 * let users, frameworks, provide their own cache implementations.
 */
public class LRUMap<K,V>
    implements LookupCache<K,V>, // since 2.12
        java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    protected final int _initialEntries;
    protected final int _maxEntries;
    protected final transient ConcurrentLinkedHashMap<K,V> _map;

    public LRUMap(int initialEntries, int maxEntries)
    {
        _initialEntries = initialEntries;
        _maxEntries = maxEntries;
        // We'll use concurrency level of 4, seems reasonable
        _map = new ConcurrentLinkedHashMap.Builder<K, V>()
                .initialCapacity(initialEntries)
                .maximumWeightedCapacity(maxEntries)
                .concurrencyLevel(4)
                .build();
    }

    @Override
    public V put(K key, V value) {
        return _map.put(key, value);
    }

    /**
     * @since 2.5
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return _map.putIfAbsent(key, value);
    }
    
    // NOTE: key is of type Object only to retain binary backwards-compatibility
    @Override
    public V get(Object key) { return _map.get(key); }

    @Override
    public void clear() { _map.clear(); }

    @Override
    public int size() { return _map.size(); }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    protected Object readResolve() {
        return new LRUMap<K,V>(_initialEntries, _maxEntries);
    }
}
