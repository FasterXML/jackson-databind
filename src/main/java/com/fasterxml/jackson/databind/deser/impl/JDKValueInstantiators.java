package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonLocation;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.JsonLocationInstantiator;

/**
 * Container for a set of {@link ValueInstantiator}s used for certain critical
 * JDK value types, either as performance optimization for initialization time observed
 * by profiling, or due to difficulty in otherwise finding constructors.
 *
 * @since 2.10
 */
public abstract class JDKValueInstantiators
{
    public static ValueInstantiator findStdValueInstantiator(DeserializationConfig config,
            Class<?> raw)
    {
        if (raw == JsonLocation.class) {
            return JsonLocationInstantiator.INSTANCE;
        }
        // [databind#1868]: empty List/Set/Map
        // [databind#2416]: optimize commonly needed default creators
        if (Collection.class.isAssignableFrom(raw)) {
            if (raw == ArrayList.class) {
                return ArrayListInstantiator.INSTANCE;
            }
            if (raw == LinkedList.class) {
                return LinkedListInstantiator.INSTANCE;
            }
            if (raw == HashSet.class) {
                return HashSetInstantiator.INSTANCE;
            }
            if (raw == TreeSet.class) {
                return TreeSetInstantiator.INSTANCE;
            }
            if (Collections.EMPTY_SET.getClass() == raw) {
                return ConstantValueInstantiator.EMPTY_SET;
            }
            if (Collections.EMPTY_LIST.getClass() == raw) {
                return ConstantValueInstantiator.EMPTY_LIST;
            }
        } else if (Map.class.isAssignableFrom(raw)) {
            if (raw == LinkedHashMap.class) {
                return LinkedHashMapInstantiator.INSTANCE;
            }
            if (raw == HashMap.class) {
                return HashMapInstantiator.INSTANCE;
            }
            if (raw == ConcurrentHashMap.class) {
                return ConcurrentHashMapInstantiator.INSTANCE;
            }
            if (raw == TreeMap.class) {
                return TreeMapInstantiator.INSTANCE;
            }
            if (Collections.EMPTY_MAP.getClass() == raw) {
                return ConstantValueInstantiator.EMPTY_MAP;
            }
        }
        return null;
    }

    private abstract static class JDKValueInstantiator
        extends ValueInstantiator.Base
        implements java.io.Serializable
    {
        public JDKValueInstantiator(Class<?> type) {
            super(type);
        }

        @Override
        public boolean canInstantiate() { return true; }

        @Override
        public boolean canCreateUsingDefault() {  return true; }
    }

    private static class ArrayListInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final ArrayListInstantiator INSTANCE = new ArrayListInstantiator();

        public ArrayListInstantiator() {
            super(ArrayList.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new ArrayList<>();
        }
    }

    private static class LinkedListInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final LinkedListInstantiator INSTANCE = new LinkedListInstantiator();

        public LinkedListInstantiator() {
            super(LinkedList.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new LinkedList<>();
        }
    }

    private static class HashSetInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final HashSetInstantiator INSTANCE = new HashSetInstantiator();

        public HashSetInstantiator() {
            super(HashSet.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new HashSet<>();
        }
    }

    private static class TreeSetInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final TreeSetInstantiator INSTANCE = new TreeSetInstantiator();

        public TreeSetInstantiator() {
            super(TreeSet.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new TreeSet<>();
        }
    }

    private static class ConcurrentHashMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final ConcurrentHashMapInstantiator INSTANCE = new ConcurrentHashMapInstantiator();

        public ConcurrentHashMapInstantiator() {
            super(ConcurrentHashMap.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new ConcurrentHashMap<>();
        }
    }

    private static class HashMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final HashMapInstantiator INSTANCE = new HashMapInstantiator();

        public HashMapInstantiator() {
            super(HashMap.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new HashMap<>();
        }
    }

    private static class LinkedHashMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final LinkedHashMapInstantiator INSTANCE = new LinkedHashMapInstantiator();

        public LinkedHashMapInstantiator() {
            super(LinkedHashMap.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new LinkedHashMap<>();
        }
    }

    private static class TreeMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final TreeMapInstantiator INSTANCE = new TreeMapInstantiator();

        public TreeMapInstantiator() {
            super(TreeMap.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new TreeMap<>();
        }
    }

    private static class ConstantValueInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public static final ConstantValueInstantiator EMPTY_SET = new ConstantValueInstantiator(Collections.EMPTY_SET);

        public static final ConstantValueInstantiator EMPTY_LIST = new ConstantValueInstantiator(Collections.EMPTY_LIST);

        public static final ConstantValueInstantiator EMPTY_MAP = new ConstantValueInstantiator(Collections.EMPTY_MAP);

        protected final Object _value;

        public ConstantValueInstantiator(Object value) {
            super(value.getClass());
            _value = value;
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return _value;
        }
    }

}
