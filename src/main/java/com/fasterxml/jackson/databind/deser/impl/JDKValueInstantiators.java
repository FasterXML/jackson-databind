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
            return JsonLocationInstantiator.instance();
        }
        // [databind#1868]: empty List/Set/Map
        // [databind#2416]: optimize commonly needed default creators
        if (Collection.class.isAssignableFrom(raw)) {
            if (raw == ArrayList.class) {
                return ArrayListInstantiator.INSTANCE;
            }
            if (raw == LinkedList.class) {
                return LinkedListInstantiator.instance();
            }
            if (raw == HashSet.class) {
                return HashSetInstantiator.INSTANCE;
            }
            if (raw == TreeSet.class) {
                return TreeSetInstantiator.instance();
            }
            if (raw == Collections.emptySet().getClass()) {
                return EmptySetInstantiator.instance();
            }
            if (raw == Collections.emptyList().getClass()) {
                return EmptyListInstantiator.instance();
            }
        } else if (Map.class.isAssignableFrom(raw)) {
            if (raw == LinkedHashMap.class) {
                return LinkedHashMapInstantiator.INSTANCE;
            }
            if (raw == HashMap.class) {
                return HashMapInstantiator.INSTANCE;
            }
            if (raw == ConcurrentHashMap.class) {
                return ConcurrentHashMapInstantiator.instance();
            }
            if (raw == TreeMap.class) {
                return TreeMapInstantiator.instance();
            }
            if (raw == Collections.emptyMap().getClass()) {
                return EmptyMapInstantiator.instance();
            }
        }
        return null;
    }

    private abstract static class JDKValueInstantiator
        extends ValueInstantiator.Base
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 2L;

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

        private static final ArrayListInstantiator INSTANCE = new ArrayListInstantiator();

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

        private static LinkedListInstantiator _instance;

        private static LinkedListInstantiator instance() {
            if (_instance == null) {
                _instance = new LinkedListInstantiator();
            }
            return _instance;
        }

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

        private static final HashSetInstantiator INSTANCE = new HashSetInstantiator();

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

        private static TreeSetInstantiator _instance;

        private static TreeSetInstantiator instance() {
            if (_instance == null) {
                _instance = new TreeSetInstantiator();
            }
            return _instance;
        }

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

        private static ConcurrentHashMapInstantiator _instance;

        private static ConcurrentHashMapInstantiator instance() {
            if (_instance == null) {
                _instance = new ConcurrentHashMapInstantiator();
            }
            return _instance;
        }

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

        private static final HashMapInstantiator INSTANCE = new HashMapInstantiator();

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

        private static final LinkedHashMapInstantiator INSTANCE = new LinkedHashMapInstantiator();

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

        private static TreeMapInstantiator _instance;

        private static TreeMapInstantiator instance() {
            if (_instance == null) {
                _instance = new TreeMapInstantiator();
            }
            return _instance;
        }

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
        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
            return _value;
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class EmptySetInstantiator
            extends ConstantValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        private static EmptySetInstantiator _instance;

        private static EmptySetInstantiator instance() {
            if (_instance == null) {
                _instance = new EmptySetInstantiator();
            }
            return _instance;
        }

        public EmptySetInstantiator() {
            super(Collections.emptySet());
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class EmptyListInstantiator
            extends ConstantValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        private static EmptyListInstantiator _instance;

        private static EmptyListInstantiator instance() {
            if (_instance == null) {
                _instance = new EmptyListInstantiator();
            }
            return _instance;
        }

        public EmptyListInstantiator() {
            super(Collections.emptyList());
        }
    }

    // @since 2.17 [databind#4299] Instantiators for additional container classes
    private static class EmptyMapInstantiator
            extends ConstantValueInstantiator
    {
        private static final long serialVersionUID = 2L;

        private static EmptyMapInstantiator _instance;

        private static EmptyMapInstantiator instance() {
            if (_instance == null) {
                _instance = new EmptyMapInstantiator();
            }
            return _instance;
        }

        public EmptyMapInstantiator() {
            super(Collections.emptyMap());
        }
    }
}
