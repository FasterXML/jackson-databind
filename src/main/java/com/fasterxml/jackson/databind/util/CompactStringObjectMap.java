package com.fasterxml.jackson.databind.util;

import java.util.*;

/**
 * Specialized lookup class that implements functionality similar to
 * {@link java.util.Map}, but for special case of key always being
 * {@link java.lang.String} and using more compact (and memory-access
 * friendly) hashing scheme. Assumption is also that keys are typically
 * intern()ed.
 *<p>
 * Generics are not used to avoid bridge methods and since these maps
 * are not exposed as part of external API.
 *
 * @since 2.6
 */
public final class CompactStringObjectMap
    implements java.io.Serializable // since 2.6.2
{
    private static final long serialVersionUID = 1L;

    /**
     * Shared instance that can be used when there are no contents to Map.
     */
    private final static CompactStringObjectMap EMPTY = new CompactStringObjectMap(1, 0,
            new Object[4]);

    private final int _hashMask, _spillCount;

    private final Object[] _hashArea;

    private CompactStringObjectMap(int hashMask, int spillCount, Object[] hashArea)
    {
        _hashMask = hashMask;
        _spillCount = spillCount;
        _hashArea = hashArea;
    }

    public static <T> CompactStringObjectMap construct(Map<String,T> all)
    {
        if (all.isEmpty()) { // can this happen?
            return EMPTY;
        }

        // First: calculate size of primary hash area
        final int size = findSize(all.size());
        final int mask = size-1;
        // and allocate enough to contain primary/secondary, expand for spillovers as need be
        int alloc = (size + (size>>1)) * 2;
        Object[] hashArea = new Object[alloc];
        int spillCount = 0;

        for (Map.Entry<String,T> entry : all.entrySet()) {
            String key = entry.getKey();

            // 09-Sep-2019, tatu: [databind#2309] skip `null`s if any included
            if (key == null) {
                continue;
            }

            int slot = key.hashCode() & mask;
            int ix = slot+slot;

            // primary slot not free?
            if (hashArea[ix] != null) {
                // secondary?
                ix = (size + (slot >> 1)) << 1;
                if (hashArea[ix] != null) {
                    // ok, spill over.
                    ix = ((size + (size >> 1) ) << 1) + spillCount;
                    spillCount += 2;
                    if (ix >= hashArea.length) {
                        hashArea = Arrays.copyOf(hashArea, hashArea.length + 4);
                    }
                }
            }
            hashArea[ix] = key;
            hashArea[ix+1] = entry.getValue();
        }
        return new CompactStringObjectMap(mask, spillCount, hashArea);
    }

    private final static int findSize(int size)
    {
        if (size <= 5) {
            return 8;
        }
        if (size <= 12) {
            return 16;
        }
        int needed = size + (size >> 2); // at most 80% full
        int result = 32;
        while (result < needed) {
            result += result;
        }
        return result;
    }

    public Object find(String key) {
        int slot = key.hashCode() & _hashMask;
        int ix = (slot<<1);
        Object match = _hashArea[ix];
        if ((match == key) || key.equals(match)) {
            return _hashArea[ix+1];
        }
        return _find2(key, slot, match);
    }

    private final Object _find2(String key, int slot, Object match)
    {
        if (match == null) {
            return null;
        }
        int hashSize = _hashMask+1;
        int ix = (hashSize + (slot>>1)) << 1;
        match = _hashArea[ix];
        if (key.equals(match)) {
            return _hashArea[ix+1];
        }
        if (match != null) { // _findFromSpill(...)
            int i = (hashSize + (hashSize>>1)) << 1;
            for (int end = i + _spillCount; i < end; i += 2) {
                match = _hashArea[i];
                if ((match == key) || key.equals(match)) {
                    return _hashArea[i+1];
                }
            }
        }
        return null;
    }

    // @since 2.9
    public Object findCaseInsensitive(String key) {
        for (int i = 0, end = _hashArea.length; i < end; i += 2) {
            Object k2 = _hashArea[i];
            if (k2 != null) {
                String s = (String) k2;
                if (s.equalsIgnoreCase(key)) {
                    return _hashArea[i+1]; // lgtm [java/index-out-of-bounds]
                }
            }
        }
        return null;
    }

    public List<String> keys() {
        final int end = _hashArea.length;
        List<String> keys = new ArrayList<String>(end >> 2);
        for (int i = 0; i < end; i += 2) {
            Object key = _hashArea[i];
            if (key != null) {
                keys.add((String) key);
            }
        }
        return keys;
    }
}
