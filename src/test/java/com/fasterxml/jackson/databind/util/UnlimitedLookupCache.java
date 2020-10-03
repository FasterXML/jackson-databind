package com.fasterxml.jackson.databind.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A LookupCache implementation that has no synchronization (like LRUMap does)
 * but that has the downside of not limiting the size of the cache.
 */
public class UnlimitedLookupCache<K,V> implements LookupCache<K,V> {

    private final transient ConcurrentHashMap<K,V> _map;

    public UnlimitedLookupCache(int initialEntries)
    {
        // We'll use concurrency level of 4, seems reasonable
        _map = new ConcurrentHashMap<K,V>(initialEntries, 0.8f, 4);
    }

    @Override
    public int size() {
        return _map.size();
    }

    @Override
    public V get(Object key) {
        return _map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return _map.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return _map.putIfAbsent(key, value);
    }

    @Override
    public void clear() {
        _map.clear();
    }
}
