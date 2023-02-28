package tools.jackson.databind.util;

import java.util.Map;
import java.util.function.BiConsumer;

import tools.jackson.databind.util.internal.PrivateMaxEntriesMap;

/**
 * Synchronized cache with bounded size: used for reusing lookup values
 * and lazily instantiated reusable items.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the initial and maximum
 * size of the contents.
 *<p>
 * The implementation evicts the least recently used
 * entry when max size is reached; this is implemented by the backing
 * {@code PrivateMaxEntriesMap} implementation.
 * Implementation is thread-safe and does NOT require external synchronization
 *<p>
 * NOTE: in Jackson 2.x this class was named {@code com.fasterxml.jackson.databind.util.LRUMap}
 */
public class SimpleLookupCache<K,V>
    implements LookupCache<K,V>, java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final int _initialEntries;
    protected final int _maxEntries;
    protected final transient PrivateMaxEntriesMap<K,V> _map;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public SimpleLookupCache(int initialEntries, int maxEntries)
    {
        _initialEntries = initialEntries;
        _maxEntries = maxEntries;
        _map = new PrivateMaxEntriesMap.Builder<K, V>()
                .initialCapacity(initialEntries)
                .maximumCapacity(maxEntries)
                // We'll use concurrency level of 4, seems reasonable
                .concurrencyLevel(4)
                .build();
    }

    @Override
    public SimpleLookupCache<K,V> snapshot() {
        return new SimpleLookupCache<K,V>(_initialEntries, _maxEntries);
    }

    /*
    /**********************************************************************
    /* Public API, basic lookup/additions
    /**********************************************************************
     */

    @Override
    public V put(K key, V value) {
        return _map.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return _map.putIfAbsent(key, value);
    }

    @Override
    public V get(Object key) { return _map.get(key); }

    @Override
    public void clear() { _map.clear(); }

    @Override
    public int size() { return _map.size(); }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public void contents(BiConsumer<K,V> consumer) {
        for (Map.Entry<K,V> entry : _map.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    /*
    /**********************************************************************
    /* Serializable overrides
    /**********************************************************************
     */

    protected Object readResolve() {
        return new SimpleLookupCache<K,V>(_initialEntries, _maxEntries);
    }
}
