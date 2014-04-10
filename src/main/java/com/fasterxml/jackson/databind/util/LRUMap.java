package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Helper for simple bounded LRU maps used for reusing lookup values.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the cache size of Map.
 *<p>
 * NOTE: the only reason we extend {@link LinkedHashMap} instead of aggregating
 * it is that this way we can override {@link #removeEldestEntry}.
 * Access, however, MUST be done using single-element access methods (or matching
 * <code>xxxAll()</code> methods that call them); access via iterators are not
 * guaranteed to work.
 *<p>
 * NOTE: since version 2.4, uses {@link ReentrantReadWriteLock} to improve
 * concurrent access.
 */
public class LRUMap<K,V> extends LinkedHashMap<K,V>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final transient Lock _readLock, _writeLock;
    
    protected final transient int _maxEntries;
    
    public LRUMap(int initialEntries, int maxEntries)
    {
        super(initialEntries, 0.8f, true);
        _maxEntries = maxEntries;
        final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        _readLock = rwl.readLock();
        _writeLock = rwl.writeLock();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > _maxEntries;
    }

    /*
    /**********************************************************
    /* Overrides to support proper concurrency
    /**********************************************************
     */

    @Override
    public V get(Object key) {
        _readLock.lock();
        try {
            return super.get(key);
        } finally {
            _readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        _writeLock.lock();
        try {
            return super.put(key, value);
        } finally {
            _writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        _writeLock.lock();
        try {
            return super.remove(key);
        } finally {
            _writeLock.unlock();
        }
    }
    
    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Ugly hack, to work through the requirement that _value is indeed final,
     * and that JDK serialization won't call ctor(s) if Serializable is implemented.
     * 
     * @since 2.1
     */
    protected transient int _jdkSerializeMaxEntries;

    private void readObject(ObjectInputStream in) throws IOException {
        _jdkSerializeMaxEntries = in.readInt();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(_jdkSerializeMaxEntries);
    }

    protected Object readResolve() {
        return new LRUMap<Object,Object>(_jdkSerializeMaxEntries, _jdkSerializeMaxEntries);
    }
}
