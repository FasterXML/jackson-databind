package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper for simple bounded LRU maps used for reusing lookup values.
 *<p>
 * Note that serialization behavior is such that contents are NOT serialized,
 * on assumption that all use cases are for caching where persistence
 * does not make sense. The only thing serialized is the cache size of Map.
 */
public class LRUMap<K,V> extends LinkedHashMap<K,V>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final int _maxEntries;
    
    public LRUMap(int initialEntries, int maxEntries)
    {
        super(initialEntries, 0.8f, true);
        _maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest)
    {
        return size() > _maxEntries;
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
