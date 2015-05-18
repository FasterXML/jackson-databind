package com.fasterxml.jackson.databind.util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Helper class used for constructing "untyped" {@link java.util.List},
 * {@link java.util.Map} and <code>Object[]</code> values.
 * Could help performance if a single instance can be used for building
 * nested Maps, Lists/Object[] of relatively small size.
 * Whether use makes sense depends; currently this class is not used.
 */
public final class ContainerBuilder
{
    private final static int MAX_BUF = 1000;

    /**
     * Buffer in which contents are being buffered (except for cases where
     * size has grown too big to bother with separate buffer)
     */
    private Object[] b;

    /**
     * Pointer to the next available slot in temporary buffer.
     */
    private int tail;

    /**
     * When building potentially multiple containers, we need to keep track of
     * the starting pointer for the current container.
     */
    private int start;

    /**
     * In cases where size of buffered contents has grown big enough that buffering
     * does not make sense, an actual {@link java.util.List} will be constructed
     * earlier and used instead of buffering.
     */
    private List<Object> list;

    /**
     * Similar to <code>list</code>, we may sometimes eagerly construct result
     * {@link java.util.Map} and skip actual buffering.
     */
    private Map<String,Object> map;
    
    public ContainerBuilder(int bufSize) {
        b = new Object[bufSize & ~1];
    }

    public boolean canReuse() {
        return (list == null) && (map == null);
    }
    
    public int bufferLength() {
        return b.length;
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int start() {
        if (list != null || map != null) {
            throw new IllegalStateException();
        }
        final int prevStart = start;
        start = tail;
        return prevStart;
    }

    public int startList(Object value) {
        if (list != null || map != null) {
            throw new IllegalStateException();
        }
        final int prevStart = start;
        start = tail;
        add(value);
        return prevStart;
    }

    public int startMap(String key, Object value) {
        if (list != null || map != null) {
            throw new IllegalStateException();
        }
        final int prevStart = start;
        start = tail;
        put(key, value);
        return prevStart;
    }
    
    public void add(Object value) {
        if (list != null) {
            list.add(value);
        } else if (tail >= b.length) {
            _expandList(value);
        } else {
            b[tail++] = value;
        }
    }

    public void put(String key, Object value) {
        if (map != null) {
            map.put(key, value);
        } else if ((tail + 2) > b.length) {
            _expandMap(key, value);
        } else {
            b[tail++] = key;
            b[tail++] = value;
        }
    }

    public List<Object> finishList(int prevStart)
    {
        List<Object> l = list;
        if (l == null) {
            l = _buildList(true);
        } else {
            list = null;
        }
        start = prevStart;
        return l;
    }

    public Object[] finishArray(int prevStart)
    {
        Object[] result;
        if (list == null) {
            result = Arrays.copyOfRange(b, start, tail);
        } else {
            result = list.toArray(new Object[tail - start]);
            list = null;
        }
        start = prevStart;
        return result;
    }

    public <T> Object[] finishArray(int prevStart, Class<T> elemType)
    {
        final int size = tail-start;
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(elemType, size);

        if (list == null) {
            System.arraycopy(b, start, result, 0, size);
        } else {
            result = list.toArray(result);
            list = null;
        }
        start = prevStart;
        return result;
    }
    
    public Map<String,Object> finishMap(int prevStart)
    {
        Map<String,Object> m = map;
        
        if (m == null) {
            m = _buildMap(true);
        } else {
            map = null;
        }
        start = prevStart;
        return m;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    private void _expandList(Object value) {
        if (b.length < MAX_BUF) { // can still expand
            b = Arrays.copyOf(b, b.length << 1);
            b[tail++] = value;
        } else {
            list = _buildList(false);
            list.add(value);
        }
    }
    
    private List<Object> _buildList(boolean isComplete)
    {
        int currLen = tail - start;
        if (isComplete) {
            if (currLen < 2) {
                currLen = 2;
            }
        } else {
            if (currLen < 20) {
                currLen = 20;
            } else if (currLen < MAX_BUF) {
                currLen += (currLen>>1);
            } else {
                currLen += (currLen>>2);
            }
        }
        List<Object> l = new ArrayList<Object>(currLen);
        for (int i = start; i < tail; ++i) {
            l.add(b[i]);
        }
        tail = start; // reset buffered entries
        return l;
    }

    private void _expandMap(String key, Object value) {
        if (b.length < MAX_BUF) { // can still expand
            b = Arrays.copyOf(b, b.length << 1);
            b[tail++] = key;
            b[tail++] = value;
        } else {
            map = _buildMap(false);
            map.put(key, value);
        }
    }
    
    private Map<String,Object> _buildMap(boolean isComplete)
    {
        int size = (tail - start) >> 1;
        if (isComplete) { // when complete, optimize to smallest size
            if (size <= 3) { // 3 or fewer entries, hash table of 4
                size = 4; 
            } else if (size <= 40) {
                size += (size>>1);
            } else {
                size += (size>>2) + (size>>4); // * 1.3125
            }
        } else {
            if (size < 10) {
                size = 16;
            } else if (size < MAX_BUF) {
                size += (size>>1);
            } else {
                size += (size/3);
            }
        }
        Map<String,Object> m = new LinkedHashMap<String,Object>(size, 0.8f);
        for (int i = start; i < tail; i += 2) {
            m.put((String) b[i], b[i+1]);
        }
        tail = start; // reset buffered entries
        return m;
    }
}
