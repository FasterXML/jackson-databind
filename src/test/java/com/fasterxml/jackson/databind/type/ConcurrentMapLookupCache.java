package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.util.LookupCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapLookupCache<K, V> implements LookupCache<K, V> {

    private final ConcurrentMap<K, V> cache = new ConcurrentHashMap<>();

    @Override
    public V put(K key, V value) {
        return cache.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return cache.putIfAbsent(key, value);
    }

    @Override
    public V get(Object key) {
        return cache.get(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        return cache.size();
    }
}
