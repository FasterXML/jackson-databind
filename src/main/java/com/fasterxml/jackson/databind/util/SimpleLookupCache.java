package com.fasterxml.jackson.databind.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Synchronized cache with bounded size: used for reusing lookup values
 * and lazily instantiated reusable items.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the initial and maximum
 * size of the contents.
 *<p>
 * NOTE: this is <b>NOT</b> an LRU-based (in 2.x it was named <code>LRUMap</code>); reason
 * being that it is not possible to use JDK components that do LRU _AND_ perform
 * well wrt synchronization on multi-core systems. So we choose efficient synchronization
 * over potentially more efficient handling of entries.
 *<p>
 * And yes, there are efficient LRU implementations such as
 * <a href="https://code.google.com/p/concurrentlinkedhashmap/">concurrentlinkedhashmap</a>;
 * but at this point we really try to keep external deps to minimum. But perhaps
 * a shaded variant may be used one day.
 */
public class SimpleLookupCache<K,V>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    protected final int _initialEntries, _maxEntries;

    protected final transient ConcurrentHashMap<K,V> _map;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public SimpleLookupCache(int initialEntries, int maxEntries)
    {
        _initialEntries = initialEntries;
        _maxEntries = maxEntries;
        // We'll use concurrency level of 4, seems reasonable
        _map = new ConcurrentHashMap<K,V>(initialEntries, 0.8f, 4);
    }

    // for JDK serialization
    protected Object readResolve() {
        return new SimpleLookupCache<Object,Object>(_initialEntries, _maxEntries);
    }

    public void contents(BiConsumer<K,V> consumer) {
        for (Map.Entry<K,V> entry : _map.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public V put(K key, V value) {
        if (_map.size() >= _maxEntries) {
            // double-locking, yes, but safe here; trying to avoid "clear storms"
            synchronized (this) {
                if (_map.size() >= _maxEntries) {
                    clear();
                }
            }
        }
        return _map.put(key, value);
    }

    public V putIfAbsent(K key, V value) {
        // not 100% optimal semantically, but better from correctness (never exceeds
        // defined maximum) and close enough all in all:
        if (_map.size() >= _maxEntries) {
            synchronized (this) {
                if (_map.size() >= _maxEntries) {
                    clear();
                }
            }
        }
        return _map.putIfAbsent(key, value);
    }
    
    // NOTE: key is of type Object only to retain binary backwards-compatibility
    public V get(Object key) {  return _map.get(key); }

    public void clear() { _map.clear(); }
    public int size() { return _map.size(); }
}
