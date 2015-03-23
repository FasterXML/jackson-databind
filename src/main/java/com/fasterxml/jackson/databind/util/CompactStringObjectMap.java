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
public abstract class CompactStringObjectMap
{
    public static CompactStringObjectMap empty() {
        return Small.EMPTY;
    }

    public static <T> CompactStringObjectMap construct(Map<String,T> contents)
    {
        if (contents.size() == 0) { // can this occur?
            return empty();
        }
        Iterator<Map.Entry<String,T>> it = contents.entrySet().iterator();
        switch (contents.size()) {
        case 1:
            return new Small(it.next(), null);
        case 2:
            return new Small(it.next(), it.next());
        }
        // General-purpose "big" one needed:
        return Big.construct(contents);
    }

    public abstract Object find(String key);

    public abstract List<String> keys();
    
    final static class Small extends CompactStringObjectMap
    {
        public final static Small EMPTY = new Small(null, null);
        
        protected final String key1, key2;
        protected final Object value1, value2;

        private Small(Map.Entry<String,?> e1, Map.Entry<String,?> e2)
        {
            if (e1 == null) {
                key1 = null;
                value1 = null;
            } else {
                key1 = e1.getKey();
                value1 = e1.getValue();
            }
            if (e2 == null) {
                key2 = null;
                value2 = null;
            } else {
                key2 = e2.getKey();
                value2 = e2.getValue();
            }
        }

        @Override
        public Object find(String key) {
            // no assumption of key being intern()ed:
            if (key.equals(key1)) {
                return value1;
            }
            if (key.equals(key2)) {
                return value2;
            }
            return null;
        }

        @Override
        public List<String> keys() {
            ArrayList<String> keys = new ArrayList<String>(2);
            if (key1 != null) {
                keys.add(key1);
            }
            if (key2 != null) {
                keys.add(key2);
            }
            return keys;
        }
}

    /**
     * Raw mapping from keys to indices, optimized for fast access via
     * better memory efficiency. Hash area divide in three; main hash,
     * half-size secondary, followed by as-big-as-needed spillover.
     */
    final static class Big extends CompactStringObjectMap
    {
        private final int _hashMask, _spillCount;

        private final String[] _keys;
        private final Object[] _values;

        private Big(int hashMask, int spillCount, String[] keys, Object[] fields)
        {
            _hashMask = hashMask;
            _spillCount = spillCount;
            _keys = keys;
            _values = fields;
        }

        public static <T> Big construct(Map<String,T> all)
        {
            // First: calculate size of primary hash area
            final int size = findSize(all.size());
            final int mask = size-1;
            // and allocate enough to contain primary/secondary, expand for spillovers as need be
            int alloc = size + (size>>1);
            String[] keys = new String[alloc];
            Object[] fieldHash = new Object[alloc];
            int spills = 0;

            for (Map.Entry<String,T> entry : all.entrySet()) {
                String key = entry.getKey();

                int slot = key.hashCode() & mask;

                // primary slot not free?
                if (keys[slot] != null) {
                    // secondary?
                    slot = size + (slot >> 1);
                    if (keys[slot] != null) {
                        // ok, spill over.
                        slot = size + (size >> 1) + spills;
                        ++spills;
                        if (slot >= keys.length) {
                            keys = Arrays.copyOf(keys, keys.length + 4);
                            fieldHash = Arrays.copyOf(fieldHash, fieldHash.length + 4);
                        }
                    }
                }
                keys[slot] = key;
                fieldHash[slot] = entry.getValue();
            }
            return new Big(mask, spills, keys, fieldHash);
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
        
        @Override
        public Object find(String key) {
            int slot = key.hashCode() & _hashMask;
            String match = _keys[slot];
            if ((match == key) || key.equals(match)) {
                return _values[slot];
            }
            if (match == null) {
                return null;
            }
            // no? secondary?
            slot = (_hashMask+1) + (slot>>1);
            match = _keys[slot];
            if (key.equals(match)) {
                return _values[slot];
            }
            // or spill?
            return _findFromSpill(key);
        }

        private Object _findFromSpill(String key) {
            int hashSize = _hashMask+1;
            int i = hashSize + (hashSize>>1);
            for (int end = i + _spillCount; i < end; ++i) {
                if (key.equals(_keys[i])) {
                    return _values[i];
                }
            }
            return null;
        }

        @Override
        public List<String> keys() {
            List<String> keys = new ArrayList<String>(_keys.length >> 1);
            for (String key : _keys) {
                if (key != null) {
                    keys.add(key);
                }
            }
            return keys;
        }
}    
}
