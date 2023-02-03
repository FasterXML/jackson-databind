package com.fasterxml.jackson.databind.deser;

import java.util.*;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests related to handling of overloaded methods.
 * and specifically addressing problems [JACKSON-189]
 * and [JACKSON-739]
 */
public class TestOverloaded
    extends BaseMapTest
{
    static class BaseListBean
    {
        List<String> list;

        BaseListBean() { }

        public void setList(List<String> l) { list = l; }
    }

    static class ArrayListBean extends BaseListBean
    {
        ArrayListBean() { }

        public void setList(ArrayList<String> l) { super.setList(l); }
    }

    static class NumberBean {
        protected Object value;

        public void setValue(Number n) { value = n; }
    }

    static class WasNumberBean extends NumberBean {
        public void setValue(String str) { value = str; }
    }

    static class Overloaded739
    {
        protected Object _value;

        @JsonProperty
        public void setValue(String str) { _value = str; }

        // no annotation, should not be chosen:
        public void setValue(Object o) { throw new UnsupportedOperationException(); }
    }

    /**
     * And then a Bean that is conflicting and should not work
     */
    static class ConflictBean {
        public void setA(ArrayList<Object> a) { }
        public void setA(LinkedList<Object> a) { }
    }

    /*
    /************************************************************
    /* Unit tests, valid
    /************************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * It should be ok to overload with specialized
     * version; more specific method should be used.
     */
    public void testSpecialization() throws Exception
    {
        ArrayListBean bean = MAPPER.readValue
            ("{\"list\":[\"a\",\"b\",\"c\"]}", ArrayListBean.class);
        assertNotNull(bean.list);
        assertEquals(3, bean.list.size());
        assertEquals(ArrayList.class, bean.list.getClass());
        assertEquals("a", bean.list.get(0));
        assertEquals("b", bean.list.get(1));
        assertEquals("c", bean.list.get(2));
    }

    /**
     * As per [JACKSON-255], should also allow more general overriding,
     * as long as there are no in-class conflicts.
     */
    public void testOverride() throws Exception
    {
        WasNumberBean bean = MAPPER.readValue
            ("{\"value\" : \"abc\"}", WasNumberBean.class);
        assertNotNull(bean);
        assertEquals("abc", bean.value);
    }

    // for [JACKSON-739]
    public void testConflictResolution() throws Exception
    {
        Overloaded739 bean = MAPPER.readValue
                ("{\"value\":\"abc\"}", Overloaded739.class);
        assertNotNull(bean);
        assertEquals("abc", bean._value);
    }

    /*
    /************************************************************
    /* Unit tests, failures
    /************************************************************
    */

    /**
     * For genuine setter conflict, an exception is to be thrown.
     */
    public void testSetterConflict() throws Exception
    {
    	try {
    	MAPPER.readValue("{ }", ConflictBean.class);
    	} catch (Exception e) {
    	    verifyException(e, "Conflicting setter definitions");
    	}
    }
}
