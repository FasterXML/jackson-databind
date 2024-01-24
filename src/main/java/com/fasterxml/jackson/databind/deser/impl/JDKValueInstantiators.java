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
            return new JsonLocationInstantiator();
        }
        // [databind#1868]: empty List/Set/Map
        // [databind#2416]: optimize commonly needed default creators
        if (Collection.class.isAssignableFrom(raw)) {
            if (raw == ArrayList.class) { // default impl, pre-constructed instance
                return ArrayListInstantiator.INSTANCE;
            }
            if (raw == HashSet.class) { // default impl, pre-constructed instance
                return HashSetInstantiator.INSTANCE;
            }
            if (raw == LinkedList.class) {
                return new LinkedListInstantiator();
            }
            if (raw == TreeSet.class) {
                return new TreeSetInstantiator();
            }
            if (raw == Collections.emptySet().getClass()) {
                return new ConstantValueInstantiator(Collections.emptySet());
            }
            if (raw == Collections.emptyList().getClass()) {
                return new ConstantValueInstantiator(Collections.emptyList());
            }
        } else if (Map.class.isAssignableFrom(raw)) {
            if (raw == LinkedHashMap.class) {
                return LinkedHashMapInstantiator.INSTANCE;
            }
            if (raw == HashMap.class) {
                return HashMapInstantiator.INSTANCE;
            }
            if (raw == ConcurrentHashMap.class) {
                return new ConcurrentHashMapInstantiator();
            }
            if (raw == TreeMap.class) {
                return new TreeMapInstantiator();
            }
            if (raw == Collections.emptyMap().getClass()) {
                return new ConstantValueInstantiator(Collections.emptyMap());
            }
        }
        return null;
    }

    // @since 2.17
    private abstract static class JDKValueInstantiator
        extends ValueInstantiator.Base
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 2L;

        public JDKValueInstantiator(Class<?> type) {
            super(type);
        }

        @Override
        public final boolean canInstantiate() { return true; }

        @Override
        public final boolean canCreateUsingDefault() {  return true; }

        // Make abstract to force (re)implementation
        @Override
        public abstract Object createUsingDefault(DeserializationContext ctxt) throws IOException;
    }

    private static class ArrayListInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        static final ArrayListInstantiator INSTANCE = new ArrayListInstantiator();

        public ArrayListInstantiator() {
            super(ArrayList.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new ArrayList<>();
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class LinkedListInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public LinkedListInstantiator() {
            super(LinkedList.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new LinkedList<>();
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class HashSetInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        static final HashSetInstantiator INSTANCE = new HashSetInstantiator();

        public HashSetInstantiator() {
            super(HashSet.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new HashSet<>();
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class TreeSetInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        public TreeSetInstantiator() {
            super(TreeSet.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new TreeSet<>();
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class ConcurrentHashMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

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

        static final HashMapInstantiator INSTANCE = new HashMapInstantiator();

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

        static final LinkedHashMapInstantiator INSTANCE = new LinkedHashMapInstantiator();

        public LinkedHashMapInstantiator() {
            super(LinkedHashMap.class);
        }

        @Override
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return new LinkedHashMap<>();
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class TreeMapInstantiator
        extends JDKValueInstantiator
    {
        private static final long serialVersionUID = 2L;

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

        protected final Object _value;

        public ConstantValueInstantiator(Object value) {
            super(value.getClass());
            _value = value;
        }

        @Override
        public final Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return _value;
        }
    }
}
