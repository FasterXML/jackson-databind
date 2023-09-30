package com.fasterxml.jackson.databind.util;

import java.util.function.BiConsumer;

/**
 * An interface describing the required API for the Jackson-databind Type cache.
 *<p>
 * Note that while interface itself does not specify synchronization requirements for
 * implementations, specific use cases do. Typically implementations are
 * expected to be thread-safe, that is, to handle synchronization.
 *
 * @since 2.12 (for forwards-compatiblity with 3.0)
 */
public interface LookupCache<K,V>
{
    /**
     * Method to apply operation on cache contents without exposing them.
     *<p>
     * Default implementation throws {@link UnsupportedOperationException}.
     * Implementations are required to override this method.
     *
     * @param consumer Operation to apply on cache contents.
     *
     * @throws UnsupportedOperationException if implementation does not override this method.
     *
     * @since 2.16
     */
    default void contents(BiConsumer<K,V> consumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Method needed for creating clones but without contents.
     *<p>
     * Default implementation throws {@link UnsupportedOperationException}.
     * Implementations are required to override this method.
     * 
     * @throws UnsupportedOperationException if implementation does not override this method.
     *
     * @since 2.16
     */
    default LookupCache<K,V> emptyCopy() {
        throw new UnsupportedOperationException("LookupCache implementation "
                +getClass().getName()+" does not implement `emptyCopy()`");
    }

    /**
     * @return Number of entries currently in cache: may be approximate, only
     *    to be used for diagnostics, metrics reporting
     */
    int size();

    /**
     * NOTE: key is of type Object only to retain binary backwards-compatibility
     *
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
