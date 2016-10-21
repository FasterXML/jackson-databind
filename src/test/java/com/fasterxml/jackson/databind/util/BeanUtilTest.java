package com.fasterxml.jackson.databind.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class BeanUtilTest extends BaseMapTest
{
    static class IsGetters {
        public boolean isPrimitive() { return false; }
        public Boolean isWrapper() { return false; }
        public String isNotGetter() { return null; }
        public boolean is() { return false; }
    }

    static class Getters {
        public String getCallbacks() { return null; }
        public String getMetaClass() { return null; }
        public boolean get() { return false; }
    }
    
    static class Setters {
        public void setFoo() { }
        public void notSetter() { }
        public void set() { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testNameMangle()
    {
        assertEquals("foo", BeanUtil.legacyManglePropertyName("getFoo", 3));
        assertEquals("foo", BeanUtil.stdManglePropertyName("getFoo", 3));

        assertEquals("url", BeanUtil.legacyManglePropertyName("getURL", 3));
        assertEquals("URL", BeanUtil.stdManglePropertyName("getURL", 3));
    }

    public void testGetDefaultValue()
    {
        TypeFactory tf = TypeFactory.defaultInstance();
        // For collection/array/Map types, should give `NOT_EMPTY`:
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(Map.class)));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(List.class)));
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(Object[].class)));
        // as well as ReferenceTypes, String
        assertEquals(JsonInclude.Include.NON_EMPTY,
                BeanUtil.getDefaultValue(tf.constructType(AtomicReference.class)));
        assertEquals("",
                BeanUtil.getDefaultValue(tf.constructType(String.class)));
        // primitive/wrappers have others
        assertEquals(Integer.valueOf(0),
                BeanUtil.getDefaultValue(tf.constructType(Integer.class)));
        

        // but POJOs have no real default
        assertNull(BeanUtil.getDefaultValue(tf.constructType(getClass())));
    }

    public void testIsGetter() throws Exception
    {
        _testIsGetter("isPrimitive", "primitive");
        _testIsGetter("isWrapper", "wrapper");
        _testIsGetter("isNotGetter", null);
        _testIsGetter("is", null);
    }

    public void testOkNameForGetter() throws Exception
    {
        // mostly chosen to exercise groovy exclusion
        _testOkNameForGetter("getCallbacks", "callbacks");
        _testOkNameForGetter("getMetaClass", "metaClass");
        _testOkNameForGetter("get", null);
    }
    
    public void testOkNameForSetter() throws Exception
    {
        _testOkNameForSetter("setFoo", "foo");
        _testOkNameForSetter("notSetter", null);
        _testOkNameForSetter("set", null);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testIsGetter(String name, String expName) throws Exception {
        _testIsGetter(name, expName, true);
        _testIsGetter(name, expName, false);
    }

    private void _testIsGetter(String name, String expName, boolean useStd) throws Exception
    {
        AnnotatedMethod m = _method(IsGetters.class, name);
        if (expName == null) {
            assertNull(BeanUtil.okNameForIsGetter(m, name, useStd));
        } else {
            assertEquals(expName, BeanUtil.okNameForIsGetter(m, name, useStd));
        }
    }

    private void _testOkNameForGetter(String name, String expName) throws Exception {
        _testOkNameForGetter(name, expName, true);
        _testOkNameForGetter(name, expName, false);
    }

    private void _testOkNameForGetter(String name, String expName, boolean useStd) throws Exception {
        AnnotatedMethod m = _method(Getters.class, name);
        if (expName == null) {
            assertNull(BeanUtil.okNameForGetter(m, useStd));
        } else {
            assertEquals(expName, BeanUtil.okNameForGetter(m, useStd));
        }
    }

    private void _testOkNameForSetter(String name, String expName) throws Exception {
        _testOkNameForSetter(name, expName, true);
        _testOkNameForSetter(name, expName, false);
    }

    @SuppressWarnings("deprecation")
    private void _testOkNameForSetter(String name, String expName, boolean useStd) throws Exception {
        AnnotatedMethod m = _method(Setters.class, name);
        if (expName == null) {
            assertNull(BeanUtil.okNameForSetter(m, useStd));
        } else {
            assertEquals(expName, BeanUtil.okNameForSetter(m, useStd));
        }
    }
    
    private AnnotatedMethod _method(Class<?> cls, String name, Class<?>...parameterTypes) throws Exception {
        return new AnnotatedMethod(null, cls.getMethod(name, parameterTypes), null, null);
    }
}
