package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.util.Snapshottable;

import java.util.function.BiConsumer;

/**
 * An interface describing the required API for the Jackson-Databind Type cache.
 *
 * @see com.fasterxml.jackson.databind.type.TypeFactory#withCache(LookupCache<java.lang.Object,com.fasterxml.jackson.databind.JavaType>)
 * @see SimpleLookupCache
 */
public interface LookupCache <K,V>
        extends Snapshottable<LookupCache<K,V>>,
        java.io.Serializable {
    void contents(BiConsumer<K,V> consumer);
    int size();

    /**
     * NOTE: key is of type Object only to retain binary backwards-compatibility
     * @param key
     * @return value associated with key (can return null)
     */
    V get(Object key);

    V put(K key, V value);
    V putIfAbsent(K key, V value);
    void clear();
}
