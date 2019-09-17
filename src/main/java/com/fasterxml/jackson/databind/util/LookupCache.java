package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.util.Snapshottable;

/**
 * An interface describing the required API for the Jackson-Databind Type cache.
 *<p>
 * Note that while interface itself does not specify synchronization requirements for
 * implementations, specific use cases do. Typically implementations are
 * expected to be thread-safe, that is, to handle synchronization.
 *
 * @see com.fasterxml.jackson.databind.type.TypeFactory#withCache(LookupCache<java.lang.Object,com.fasterxml.jackson.databind.JavaType>)
 * @see SimpleLookupCache
 */
public interface LookupCache <K,V>
    extends Snapshottable<LookupCache<K,V>>
{
    // 17-Sep-2019, tatu: There is one usage, by `ReadOnlyClassToSerializerMap`, so
    //    for now NOT exposed, but can reconsider if it proves generally useful
    
//    void contents(BiConsumer<K,V> consumer);

    /**
     * @return Number of entries currently in cache: may be approximate, only
     *    to be used for diagnostics, metrics reporting
     */
    int size();

    /**
     * NOTE: key is of type Object only to retain binary backwards-compatibility
     *
     * @param key
     * @return value associated with key (can return null)
     */
    V get(Object key);

    V put(K key, V value);

    V putIfAbsent(K key, V value);

    /**
     * Method for removing all contents this cache has.
     */
    void clear();
}
